package com.j.jface.org;

import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.j.jface.R;
import com.j.jface.feed.Client;
import com.j.jface.lifecycle.WrappedActivity;
import com.j.jface.org.todo.Todo;
import com.j.jface.org.todo.TodoSource;
import com.j.jface.org.todo.TodoUtil;

import java.util.ArrayList;

/**
 * Main activity class for JOrg.
 */
public class JOrg extends WrappedActivity
{
  private static final int LAYOUT_ANIMATION_DURATION = 100;
  @NonNull private final Client mClient;
  @NonNull private final TodoSource mTodoSource;
  @NonNull private final SoundSource mSoundSource;
  @NonNull private final EditTextSoundRouter mSoundRouter;
  @NonNull private final TodoAdapter mAdapter;

  public JOrg(@NonNull Args args)
  {
    super(args);
    mA.setContentView(R.layout.org_top);
    mClient = new Client(mA);
    mTodoSource = new TodoSource(mA);
    mSoundSource = new SoundSource(mA, (ViewGroup)mA.findViewById(R.id.sound_source));
    mSoundRouter = new EditTextSoundRouter(mSoundSource);
    ArrayList<Todo> todoList = mTodoSource.fetchTodoList();
/*    Todo x = new Todo.Builder("subfoo").setParent(todoList.get(0)).build();
    mTodoSource.updateTodo(x);
    mTodoSource.updateTodo(new Todo.Builder("subsubfoo").setParent(x).build());*/

    final RecyclerView rv = (RecyclerView) mA.findViewById(R.id.todoList);
    mAdapter = new TodoAdapter(mA, mSoundRouter, todoList, rv);
    rv.setAdapter(mAdapter);
    rv.addItemDecoration(new DividerItemDecoration(mA, ((LinearLayoutManager)rv.getLayoutManager()).getOrientation()));

    final FloatingActionButton fab = (FloatingActionButton)mA.findViewById(R.id.addTodo);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view)
      {
//        final Todo newTodo = new Todo("");
//        mTodoList.add(newTodo);
//        addTodoView(newTodo, top, 0);
      }
    });
  }

  public void onPause()
  {
    mSoundSource.onPause();
  }

  public void onResume()
  {
    mSoundSource.onResume();
  }

  private void populateExpandedTodo(final View v, final Todo t)
  {
    final Spinner s = (Spinner)v.findViewById(R.id.deadLineType);
    final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mA,
     R.array.deadline_type, android.R.layout.simple_spinner_dropdown_item);
    s.setAdapter(adapter);
    final Button tc = (Button)v.findViewById(R.id.todoTimeConstraint);
    tc.setText(TodoUtil.timeConstraintString(t.mPlanning.mTimeConstraint));
    final Button w = (Button)v.findViewById(R.id.todoWhere);
    w.setText(TodoUtil.whereString(t.mPlanning.mWhere));
  }
}
