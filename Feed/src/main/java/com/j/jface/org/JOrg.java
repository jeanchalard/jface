package com.j.jface.org;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;

import com.j.jface.Const;
import com.j.jface.R;
import com.j.jface.lifecycle.TodoEditorBoot;
import com.j.jface.lifecycle.WrappedActivity;
import com.j.jface.org.todo.Todo;
import com.j.jface.org.todo.TodoList;
import com.j.jface.org.todo.TodoSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

/**
 * Main activity class for JOrg.
 */
public class JOrg extends WrappedActivity implements Handler.Callback, TodoList.ChangeObserver
{
  @NonNull private final TodoSource mTodoSource;
  @NonNull private final SoundSource mSoundSource;
  @NonNull private final EditTextSoundRouter mSoundRouter;
  @NonNull private final TodoAdapter mAdapter;
  @NonNull private final CoordinatorLayout mTopLayout;

  private static final int PERSIST_TODOS = 1;
  @NonNull private final Handler mHandler;
  @NonNull private final TodoList mTodoList;
  @NonNull private final HashMap<String, Todo> mTodosToPersist;

  public JOrg(@NonNull final Args args)
  {
    super(args);
    mA.setContentView(R.layout.org_top);
    ((AppCompatActivity)mA).setSupportActionBar((Toolbar)mA.findViewById(R.id.orgTopActionBar));
    mHandler = new Handler(this);
    mTodoSource = new TodoSource(mA);
    mSoundSource = new SoundSource(mA, (ViewGroup)mA.findViewById(R.id.sound_source));
    mSoundRouter = new EditTextSoundRouter(mSoundSource);
    mTodosToPersist = new HashMap<>();
    mTodoList = mTodoSource.fetchTodoList();
    mTodoList.addObserver(this);

    mTopLayout = (CoordinatorLayout)mA.findViewById(R.id.topLayout);
    final RecyclerView rv = (RecyclerView)mA.findViewById(R.id.todoList);
    mAdapter = new TodoAdapter(this, mA, mSoundRouter, mTodoList, rv);
    rv.setAdapter(mAdapter);
    rv.addItemDecoration(new DividerItemDecoration(mA, ((LinearLayoutManager)rv.getLayoutManager()).getOrientation()));

    final FloatingActionButton fab = (FloatingActionButton)mA.findViewById(R.id.addTodo);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view)
      {
        addNewSubTodo(null);
      }
    });
  }

  public void onPause()
  {
    mSoundSource.onPause();
    persistAllTodos();
  }
  public void onResume()
  {
    mSoundSource.onResume();
  }

  public boolean handleMessage(@NonNull final Message msg)
  {
    switch (msg.what)
    {
      case PERSIST_TODOS:
        persistAllTodos();
        break;
    }
    return true;
  }

  // Null parent means top level, as always
  @NonNull public Todo addNewSubTodo(@Nullable final Todo parent)
  {
    final Todo todo = mTodoList.createAndInsertTodo("", parent);
    mAdapter.passFocusTo(todo);
    return todo;
  }

  public void clearTodo(@NonNull final Todo todo)
  {
    final ArrayList<Todo> descendants = mTodoList.getDescendants(todo);
    for (final ListIterator<Todo> it = descendants.listIterator(descendants.size()); it.hasPrevious(); )
    {
      final Todo t = it.previous();
      final Todo newTodo = new Todo.Builder(t).setCompletionTime(System.currentTimeMillis()).build();
      mTodoList.updateTodo(newTodo);
    }
    final Snackbar undoChance = Snackbar.make(mTopLayout, "Marked done.", Snackbar.LENGTH_LONG);
    undoChance.setDuration(8000); // 8 seconds, because LENGTH_LONG is punily short
    undoChance.setAction("Undo", new View.OnClickListener() {
      @Override public void onClick(View v)
      {
        for (final Todo todo : descendants) mTodoList.updateTodo(todo);
      }
    });
    undoChance.show();
  }

  public Todo updateTodoContents(@NonNull final Todo todo, @NonNull final Editable editable)
  {
    final String text = editable.toString();
    final Todo newTodo = new Todo.Builder(todo).setText(text).build();
    mTodoList.updateTodo(newTodo);
    return newTodo;
  }

  public void persistAllTodos()
  {
    HashMap<String, Todo> todosToPersist = new HashMap<>();
    synchronized (mTodosToPersist)
    {
      todosToPersist.putAll(mTodosToPersist);
      mTodosToPersist.clear();
    }
    for (final Todo t : todosToPersist.values()) mTodoSource.updateTodo(t);
  }

  @Override public void notifyItemChanged(final int position, @NonNull final Todo payload)
  {
    synchronized(mTodosToPersist)
    {
      mTodosToPersist.put(payload.id, payload);
    }
    mHandler.removeMessages(PERSIST_TODOS);
    mHandler.sendEmptyMessageDelayed(PERSIST_TODOS, 3000); // 3 sec before persistence
  }

  @Override public void notifyItemInserted(final int position, @NonNull final Todo payload)
  {
    mTodoSource.updateTodo(payload);
  }

  @Override public void notifyItemsRemoved(final int position, @NonNull final List<Todo> payload)
  {
    for (final Todo t : payload)
      mTodoSource.updateTodo(t);
  }

  public void startTodoEditor(@NonNull final Todo todo)
  {
    final Intent editorIntent = new Intent(mA, TodoEditorBoot.class);
    editorIntent.putExtra(Const.EXTRA_TODO_ID, todo.id);
    mA.startActivity(editorIntent);
  }
}
