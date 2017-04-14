package com.j.jface.org;

import android.content.Context;
import android.content.Intent;
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
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.j.jface.Const;
import com.j.jface.R;
import com.j.jface.client.Client;
import com.j.jface.client.action.drive.RecursiveBackupAction;
import com.j.jface.lifecycle.TodoEditorBoot;
import com.j.jface.lifecycle.WrappedActivity;
import com.j.jface.org.todo.Todo;
import com.j.jface.org.todo.TodoList;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.ListIterator;
import java.util.Locale;

/**
 * Main activity class for JOrg.
 */
public class JOrg extends WrappedActivity
{
  @NonNull private final Client mClient;
  @NonNull private final SoundSource mSoundSource;
  @NonNull private final EditTextSoundRouter mSoundRouter;
  @NonNull private final TodoAdapter mAdapter;
  @NonNull private final CoordinatorLayout mTopLayout;
  @NonNull private final ItemTouchHelper mTouchHelper;

  @NonNull private final TodoList mTodoList;

  public JOrg(@NonNull final Args args)
  {
    super(args);
    mClient = new Client(mA);
    mA.setContentView(R.layout.org_top);
    ((AppCompatActivity)mA).setSupportActionBar((Toolbar)mA.findViewById(R.id.orgTopActionBar));
    mSoundSource = new SoundSource(mA, (ViewGroup)mA.findViewById(R.id.sound_source));
    mSoundRouter = new EditTextSoundRouter(mSoundSource);
    mTodoList = TodoList.getInstance(mA.getApplicationContext());

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

    mTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
      int mDestination = -1;

      @Override public boolean onMove(@NonNull final RecyclerView recyclerView, @NonNull final RecyclerView.ViewHolder viewHolder, @NonNull final RecyclerView.ViewHolder target)
      {
        mAdapter.notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
      }

      public void onMoved(@NonNull final RecyclerView recyclerView, @NonNull final RecyclerView.ViewHolder viewHolder, final int fromPos, @NonNull final RecyclerView.ViewHolder target, final int toPos, final int x, final int y)
      {
        super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y);
        mDestination = toPos;
      }

      @Override public boolean canDropOver(@NonNull final RecyclerView recyclerView, @NonNull final RecyclerView.ViewHolder current, @NonNull final RecyclerView.ViewHolder target)
      {
        return ((TodoAdapter.ViewHolder)current).parent() == ((TodoAdapter.ViewHolder)target).parent();
      }

      @Override public int interpolateOutOfBoundsScroll(@NonNull final RecyclerView recyclerView, final int viewSize, final int viewSizeOutOfBounds, final int totalSize, final long msSinceStartScroll)
      {
        final int i = super.interpolateOutOfBoundsScroll(recyclerView, viewSize, viewSizeOutOfBounds, totalSize, msSinceStartScroll);
        return i > 0 ? Math.max(i, 10) : Math.min(i, -10);
      }

      @Override public void clearView(@NonNull final RecyclerView recyclerView, @NonNull final RecyclerView.ViewHolder viewHolder)
      {
        super.clearView(recyclerView, viewHolder);
        ((TodoAdapter.ViewHolder)viewHolder).cleanupViewAterDrag();
        ((TodoAdapter.ViewHolder)viewHolder).moveTodo(mDestination);
      }

      public void onSelectedChanged(@Nullable final RecyclerView.ViewHolder viewHolder, final int actionState)
      {
        super.onSelectedChanged(viewHolder, actionState);
        if (null == viewHolder) return;
        ((TodoAdapter.ViewHolder)viewHolder).prepareViewForDrag();
        mDestination = -1;
      }

      @Override public boolean isItemViewSwipeEnabled() { return false; }
      @Override public void onSwiped(final RecyclerView.ViewHolder viewHolder, final int direction) {}
    });
    mTouchHelper.attachToRecyclerView(rv);

    scheduleBackup();
  }

  public Context getContext()
  {
    return mA;
  }

  public void onPause()
  {
    mSoundSource.onPause();
    mTodoList.onPauseApplication();
  }
  public void onResume()
  {
    mSoundSource.onResume();
    final GregorianCalendar c = new GregorianCalendar();
    final String title = String.format(Locale.JAPAN, "%02d-%02d %s",
     c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), Const.WEEKDAYS[c.get(Calendar.DAY_OF_WEEK) - 1]);
    ((TextView)mA.findViewById(R.id.title_today)).setText(title);
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
    final ArrayList<Todo> descendants = mTodoList.getTreeRootedAt(todo);
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
    mTodoList.scheduleUpdateTodo(newTodo);
    return newTodo;
  }

  public void startTodoEditor(@NonNull final Todo todo)
  {
    final Intent editorIntent = new Intent(mA, TodoEditorBoot.class);
    editorIntent.putExtra(Const.EXTRA_TODO_ID, todo.id);
    mA.startActivity(editorIntent);
  }

  public void startDrag(@NonNull final TodoAdapter.ViewHolder holder)
  {
    mTouchHelper.startDrag(holder);
  }

  private void scheduleBackup()
  {
    //new RecursiveBackupAction(mClient, null).enqueue();
  }
}
