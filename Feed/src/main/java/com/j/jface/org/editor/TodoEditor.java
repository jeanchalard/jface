package com.j.jface.org.editor;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.j.jface.Const;
import com.j.jface.R;
import com.j.jface.lifecycle.WrappedActivity;
import com.j.jface.org.todo.Todo;
import com.j.jface.org.todo.TodoUpdaterProxy;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

// An activity that provides detailed editing for a single Todo.
public class TodoEditor extends WrappedActivity
{
  @NonNull private final TodoDetails mDetails;
  protected TodoEditor(@NonNull final Args a)
  {
    super(a);
    mA.requestWindowFeature(Window.FEATURE_NO_TITLE);
    mA.setContentView(R.layout.todo_editor);
    final TextView title = mA.findViewById(R.id.todoEditor_title);

    final Intent intent = mA.getIntent();
    final String todoId = null == intent ? null : intent.getStringExtra(Const.EXTRA_TODO_ID);
    final TodoUpdaterProxy updaterProxy = TodoUpdaterProxy.getInstance(mA);
    final Todo t = null == todoId ? Todo.NULL_TODO : updaterProxy.getFromId(todoId);
    final Todo todo = null == t ? Todo.NULL_TODO : t;

    title.setText(todo.text);
    mDetails = new TodoDetails(updaterProxy, todo, mA.findViewById(R.id.todoEditor_details));
  }

  public static class TodoDetails implements CalendarView.DateChangeListener, View.OnClickListener, AdapterView.OnItemSelectedListener, NumericPicker.OnValueChangeListener
  {
    @NonNull private final TodoUpdaterProxy mUpdater;
    @NonNull private Todo mTodo;
    @NonNull private final ViewGroup mRootView;
    @NonNull private final TextView mLifeline;
    @NonNull private final TextView mDeadline;
    @NonNull private final CalendarView mCalendarView;
    @NonNull private final Spinner mHardness;
    @NonNull private final Spinner mConstraint;
    @NonNull private final NumericPicker mEstimatedTime;
    @Nullable private TextView mEditing;

    public TodoDetails(@NonNull final TodoUpdaterProxy updaterProxy, @NonNull final Todo todo, @NonNull final ViewGroup rootView)
    {
      mUpdater = updaterProxy;
      mTodo = todo;
      mRootView = rootView;
      mLifeline = rootView.findViewById(R.id.todoDetails_lifeline_text);
      mDeadline = rootView.findViewById(R.id.todoDetails_deadline_text);
      mHardness = rootView.findViewById(R.id.todoDetails_hardness);
      mConstraint = rootView.findViewById(R.id.todoDetails_constraint);
      mEstimatedTime = rootView.findViewById(R.id.todoDetails_estimatedTime);

      cleanupSpinner(mHardness); cleanupSpinner(mConstraint);

      mLifeline.setOnClickListener(this);
      mDeadline.setOnClickListener(this);
      mCalendarView = rootView.findViewById(R.id.todoDetails_calendarView);
      mCalendarView.addDateChangeListener(this);

      mHardness.setOnItemSelectedListener(this);
      final ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(rootView.getContext(), android.R.layout.simple_spinner_item, Todo.CONSTRAINT_NAMES);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      mConstraint.setAdapter(adapter);
      mConstraint.setOnItemSelectedListener(this);
      mConstraint.setBackgroundResource(R.drawable.rectangle);

      mEstimatedTime.setMinValue(-1); mEstimatedTime.setMaxValue(96);
      mEstimatedTime.setOnValueChangedListener(this);

      bind(mTodo);
    }

    private void cleanupSpinner(final Spinner s)
    {
      // Remove the huge and useless arrow from the spinner
      s.setBackground(null);
      s.setPadding(0, 0, 0, 0);
      s.setOnItemSelectedListener(this);
    }

    public void bind(@NonNull final Todo todo)
    {
      mTodo = todo;
      setTextDate(mLifeline, todo.lifeline);
      setTextDate(mDeadline, todo.deadline);
      mHardness.setSelection(todo.hardness);
      mConstraint.setSelection(todo.constraint);
      mEstimatedTime.setValue(mTodo.estimatedTime < 0 ? mTodo.estimatedTime : mTodo.estimatedTime / 5);
      mCalendarView.setVisibility(View.GONE);
    }

    @NonNull private static GregorianCalendar sRenderCalendar = new GregorianCalendar();
    private static String renderDate(final long date)
    {
      if (0 == date) return "—";
      if (-1 == date) return "?";
      synchronized (sRenderCalendar)
      {
        sRenderCalendar.setTimeInMillis(date);
        return String.format(Locale.JAPAN, "%04d-%02d-%02d (%s) %02d:%02d",
         sRenderCalendar.get(Calendar.YEAR), sRenderCalendar.get(Calendar.MONTH) + 1, sRenderCalendar.get(Calendar.DAY_OF_MONTH),
         Const.WEEKDAYS[sRenderCalendar.get(Calendar.DAY_OF_WEEK) - 1], sRenderCalendar.get(Calendar.HOUR_OF_DAY), sRenderCalendar.get(Calendar.MINUTE));
      }
    }

    private void setTextDate(@NonNull final TextView textView, final long date)
    {
      textView.setText(renderDate(date));
      if (date > 0)
        textView.setBackground(null);
      else
        textView.setBackgroundResource(R.drawable.rectangle);
      if (textView == mEditing)
        textView.setBackgroundResource(R.drawable.red_rectangle);
    }

    @Override public void onDateChanged(final long newDate)
    {
      if (null == mEditing) return;
      setTextDate(mEditing, newDate);
      final Todo.Builder b = new Todo.Builder(mTodo);
      if (mLifeline == mEditing) b.setLifeline(newDate);
      else b.setDeadline(newDate);
      mTodo = b.build();
      mUpdater.updateTodo(mTodo);
      if (0 == newDate)
      {
        TransitionManager.beginDelayedTransition(mRootView);
        mCalendarView.setVisibility(View.GONE);
      }
    }

    @Override public void onClick(@NonNull final View v)
    {
      mDeadline.setBackground(null); mLifeline.setBackground(null);
      TransitionManager.beginDelayedTransition(mRootView);
      // Either Lifeline or Deadline
      if (v == mEditing)
      {
        mCalendarView.setVisibility(View.GONE);
      }
      else
      {
        final long date = v == mDeadline ? mTodo.deadline : mTodo.lifeline;
        v.setBackgroundResource(R.drawable.red_rectangle);
        mEditing = (TextView) v;
        mCalendarView.setDate(date > 0 ? date : System.currentTimeMillis());
        mCalendarView.setVisibility(View.VISIBLE);
      }
    }

    @Override public void onItemSelected(@NonNull final AdapterView<?> parent, @Nullable final View view, final int position, final long id)
    {
      if (null == view) return;
      final Todo.Builder b = new Todo.Builder(mTodo);
      if (view.getParent() == mHardness)
      {
        if (position == mTodo.hardness) return;
        b.setHardness(position);
      }
      else if (view.getParent() == mConstraint)
      {
        if (position == mTodo.constraint) return;
        b.setConstraint(position);
      }
      mTodo = b.build();
      mUpdater.updateTodo(mTodo);
    }

    @Override public void onNothingSelected(AdapterView<?> parent)
    {
      onItemSelected(parent, null, 0, 0);
    }

    @Override public void onValueChange(@NonNull final NumericPicker picker, final int oldVal, final int newVal)
    {
      mTodo = new Todo.Builder(mTodo).setEstimatedTime(newVal < 0 ? newVal : newVal * 5).build();
      mUpdater.updateTodo(mTodo);
    }
  }
}
