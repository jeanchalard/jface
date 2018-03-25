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
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInApi;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.j.jface.Const;
import com.j.jface.R;
import com.j.jface.action.GThread;
import com.j.jface.feed.views.SnackbarRegistry;
import com.j.jface.lifecycle.TodoEditorBoot;
import com.j.jface.lifecycle.WrappedActivity;
import com.j.jface.org.sound.EditTextSoundRouter;
import com.j.jface.org.sound.SoundSource;
import com.j.jface.org.todo.Todo;
import com.j.jface.org.todo.TodoListView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Main activity class for JOrg.
 */
public class JOrg extends WrappedActivity
{
  @NonNull private final GThread mGThread;
  @NonNull private final SoundSource mSoundSource;
  @NonNull private final EditTextSoundRouter mSoundRouter;
  @NonNull private final TodoAdapter mAdapter;
  @NonNull private final CoordinatorLayout mTopLayout;
  @NonNull private final ItemTouchHelper mTouchHelper;

  @NonNull private final TodoListView mTodoList;

  public JOrg(@NonNull final Args args)
  {
    super(args);
    mGThread = new GThread(mA);

    mA.setContentView(R.layout.org_top);
    ((AppCompatActivity)mA).setSupportActionBar(mA.findViewById(R.id.orgTopActionBar));
    mSoundSource = new SoundSource(mA, mA.findViewById(R.id.sound_source));
    mSoundRouter = new EditTextSoundRouter(mSoundSource);

    mTopLayout = mA.findViewById(R.id.topLayout);
    final RecyclerView rv = mA.findViewById(R.id.todoList);
    mTodoList = new TodoListView(mGThread, mA.getApplicationContext());
    mAdapter = new TodoAdapter(this, mA, mSoundRouter, mTodoList, rv);
    rv.setAdapter(mAdapter);
    rv.addItemDecoration(new DividerItemDecoration(mA, ((LinearLayoutManager)rv.getLayoutManager()).getOrientation()));

    final FloatingActionButton fab = mA.findViewById(R.id.addTodo);
    fab.setOnClickListener(view -> addNewSubTodo(null));

    mTouchHelper = new ItemTouchHelper(new TodoMover(mAdapter, mTodoList));
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
    SnackbarRegistry.INSTANCE.unsetSnackbarParent(mTopLayout);
  }

  public void onResume()
  {
    mSoundSource.onResume();
    final GregorianCalendar c = new GregorianCalendar();
    final String title = String.format(Locale.JAPAN, "%02d-%02d %s",
     c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), Const.WEEKDAYS[c.get(Calendar.DAY_OF_WEEK) - 1]);
    ((TextView)mA.findViewById(R.id.title_today)).setText(title);
    SnackbarRegistry.INSTANCE.setSnackbarParent(mTopLayout);
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
    final ArrayList<Todo> oldTree = mTodoList.markTodoCompleteAndReturnOldTree(todo);
    final Snackbar undoChance = Snackbar.make(mTopLayout, "Marked done.", Snackbar.LENGTH_LONG);
    undoChance.setDuration(8000); // 8 seconds, because LENGTH_LONG is punily short
    undoChance.setAction("Undo", v -> { for (final Todo t : oldTree) mTodoList.updateRawTodo(t); });
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

  @Override protected void onActivityResult(final int requestCode, final int resultCode, final Intent data)
  {
    super.onActivityResult(requestCode, resultCode, data);
    final GoogleSignInResult x = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
  }

  public void startDrag(@NonNull final TodoViewHolder holder)
  {
    mTouchHelper.startDrag(holder);
  }

  private void scheduleBackup()
  {
    // TODO : write this
    mGThread.enqueueF((final FirebaseFirestore db) -> {
      final Todo todo = new Todo("test todo", "123");
      db.collection("metadata").document("lastUse").set(todo).addOnCompleteListener((arg) -> Log.e("WRITE", "" + arg));
      return null;
    });
  }
}
