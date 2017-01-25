package com.j.jface.org;

import android.animation.LayoutTransition;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.j.jface.R;
import com.j.jface.lifecycle.WrappedActivity;
import com.j.jface.org.todo.Todo;
import com.j.jface.org.todo.TodoUtil;

import java.util.Arrays;
import java.util.List;

/**
 * Main activity class for JOrg.
 */
public class JOrg extends WrappedActivity
{
  private static final int LAYOUT_ANIMATION_DURATION = 100;
  @NonNull private final SoundSource mSoundSource;
  @NonNull private final SoundRouter mSoundRouter;

  public JOrg(@NonNull Args args)
  {
    super(args);
    mA.setContentView(R.layout.org_top);
    final LinearLayout top = (LinearLayout)mA.findViewById(R.id.todoList);
    mSoundRouter = new SoundRouter();
    mSoundSource = new SoundSource(mA, mSoundRouter, (ViewGroup)mA.findViewById(R.id.sound_source));

    Todo tt[] = {
     new Todo("Inventer une machine à remonter le temps", null, null,
      Arrays.asList(new Todo("Trouver du plutonium"), new Todo("Acheter un gilet pare-balles", null, null, Arrays.asList(new Todo("Demander aux lybiens")), null, 0), new Todo("Trouver une batterie de 2.21GW")),
      null, 0), new Todo("Devenir maître du monde"), new Todo("Faire le jeu du futur avec Rubix"), new Todo("")
    };

    top.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
    top.getLayoutTransition().setDuration(LAYOUT_ANIMATION_DURATION);
    addTodos(Arrays.asList(tt), top, 0);
  }

  public void onPause()
  {
    mSoundSource.onPause();
  }

  public void onResume()
  {
    mSoundSource.onResume();
  }

  private void addTodos(final List<Todo> l, final LinearLayout topView, final int shift)
  {
    for (final Todo todo : l)
    {
      topView.addView(inflateTodo(todo, shift));
      final View separator = new View(mA);
      separator.setBackgroundColor(0xFFA0A0A0);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1);
      lp.setMargins(20 + shift, 0, 20, 0);
      separator.setLayoutParams(lp);
      topView.addView(separator);
      if (null != todo.mChildren) addTodos(todo.mChildren, topView, shift + 25);
    }
  }

  private View inflateTodo(final Todo t, final int shift)
  {
    final LinearLayout v = (LinearLayout)mA.getLayoutInflater().inflate(R.layout.todo, null);
    final EditText et = ((EditText)v.findViewById(R.id.todoText));
    et.setOnFocusChangeListener(mSoundRouter);
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
