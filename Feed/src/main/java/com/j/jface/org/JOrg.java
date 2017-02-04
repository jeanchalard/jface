package com.j.jface.org;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.j.jface.R;
import com.j.jface.feed.Client;
import com.j.jface.lifecycle.WrappedActivity;
import com.j.jface.org.todo.Todo;
import com.j.jface.org.todo.TodoSource;
import com.j.jface.org.todo.TodoUtil;

import java.util.ArrayList;
import java.util.List;

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
  // This is final but nowhere near unmodifiable ; in fact it's modified all over
  // the place whenever the UI changes anything.
  @NonNull private final ArrayList<Todo> mTodoList;

  public JOrg(@NonNull Args args)
  {
    super(args);
    mA.setContentView(R.layout.org_top);
    mClient = new Client(mA);
    mTodoSource = new TodoSource(mA);
    mSoundSource = new SoundSource(mA, (ViewGroup) mA.findViewById(R.id.sound_source));
    mSoundRouter = new EditTextSoundRouter(mSoundSource);

    final LinearLayout top = (LinearLayout)mA.findViewById(R.id.todoList);
    top.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
    top.getLayoutTransition().setDuration(LAYOUT_ANIMATION_DURATION);

    mTodoList = mTodoSource.fetchTodoList();
    addTodoViews(mTodoList, top, 0);

    final FloatingActionButton fab = (FloatingActionButton)mA.findViewById(R.id.addTodo);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view)
      {
        final Todo newTodo = new Todo("");
        mTodoList.add(newTodo);
        addTodoView(newTodo, top, 0);
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

  private void addTodoViews(final List<Todo> l, final LinearLayout topView, final int shift)
  {
    for (final Todo todo : l)
    {
      addTodoView(todo, topView, shift);
      addTodoViews(todo.mChildren, topView, shift + 25);
    }
  }

  private void addTodoView(final Todo todo, final LinearLayout topView, final int shift)
  {
    topView.addView(inflateTodo(todo, shift));
    final View separator = new View(mA);
    separator.setBackgroundColor(0xFFA0A0A0);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1);
    lp.setMargins(20 + shift, 0, 20, 0);
    separator.setLayoutParams(lp);
    topView.addView(separator);
  }

  private View inflateTodo(final Todo t, final int shift)
  {
    @SuppressLint("InflateParams") final LinearLayout v = (LinearLayout)mA.getLayoutInflater().inflate(R.layout.todo, null);
    final SelReportEditText et = ((SelReportEditText)v.findViewById(R.id.todoText));
    et.setOnFocusChangeListener(mSoundRouter);
    et.mListener = mSoundRouter;
    final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lp.setMargins(shift, 0, 0, 0);
    v.setLayoutParams(lp);
    final LayoutTransition lt = v.getLayoutTransition();
    lt.setDuration(LayoutTransition.CHANGE_APPEARING, LAYOUT_ANIMATION_DURATION);
    lt.setDuration(LayoutTransition.CHANGE_DISAPPEARING, LAYOUT_ANIMATION_DURATION);
    lt.setStartDelay(LayoutTransition.APPEARING, 0);
    et.setText(t.mText);
    final View expansion = v.findViewById(R.id.todoExpanded);
    final ImageButton b = (ImageButton)v.findViewById(R.id.todoExpandButton);
    b.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(final View clicked)
      {
        if (expansion.getVisibility() == View.VISIBLE)
          expansion.setVisibility(View.GONE);
        else
          expansion.setVisibility(View.VISIBLE);
      }
    });
    expansion.setVisibility(View.GONE);
    populateExpandedTodo(v, t);
    return v;
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
