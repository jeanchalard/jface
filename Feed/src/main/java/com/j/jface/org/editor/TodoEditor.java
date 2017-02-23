package com.j.jface.org.editor;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import com.j.jface.Const;
import com.j.jface.R;
import com.j.jface.lifecycle.WrappedActivity;
import com.j.jface.org.todo.Todo;
import com.j.jface.org.todo.TodoList;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import static com.j.jface.org.todo.Todo.NULL_TODO;

// An activity that provides detailed editing for a single Todo.
public class TodoEditor extends WrappedActivity
{
  @NonNull private final TodoList mList;
  @NonNull private final Todo mTodo;
  @NonNull private final TextView mTitle;
  @NonNull private final TodoDetails mDetails;
  protected TodoEditor(@NonNull final Args a)
  {
    super(a);
    mA.requestWindowFeature(Window.FEATURE_NO_TITLE);
    mA.setContentView(R.layout.todo_editor);
    mTitle = (TextView)mA.findViewById(R.id.todoEditor_title);

    mList = TodoList.getInstance(mA.getApplicationContext());
    final Intent intent = mA.getIntent();
    Todo todo;
    final String todoId = null == intent ? null : intent.getStringExtra(Const.EXTRA_TODO_ID);
    todo = null == todoId ? null : mList.getFromId(todoId);
    mTodo = null == todo ? NULL_TODO : todo;

    mTitle.setText(mTodo.text);
    mDetails = new TodoDetails(mA.getApplicationContext(), mTodo, (ViewGroup)mA.findViewById(R.id.todoEditor_details));
  }

  public static class TodoDetails implements CalendarView.DateChangeListener, View.OnClickListener, AdapterView.OnItemSelectedListener
  {
    @NonNull private final TodoList mList;
    @NonNull private Todo mTodo;
    @NonNull private final ViewGroup mRootView;
    @NonNull private final TextView mLifeline;
    @NonNull private final TextView mDeadline;
    @NonNull private final CalendarView mCalendarView;
    @NonNull private final Spinner mHardness;
    @Nullable private TextView mEditing;

    public TodoDetails(@NonNull final Context context, @NonNull final Todo todo, @NonNull final ViewGroup rootView)
    {
      mList = TodoList.getInstance(context);
      mTodo = todo;
      mRootView = rootView;
      mLifeline = (TextView)rootView.findViewById(R.id.todoDetails_lifeline_text);
      mDeadline = (TextView)rootView.findViewById(R.id.todoDetails_deadline_text);
      mHardness = (Spinner)rootView.findViewById(R.id.todoDetails_hardness);

      // Remove the huge and useless arrow from the spinner
      mHardness.setBackground(null);
      mHardness.setPadding(0, 0, 0, 0);
      mHardness.setOnItemSelectedListener(this);

      mLifeline.setOnClickListener(this);
      mDeadline.setOnClickListener(this);
      mCalendarView = (CalendarView)rootView.findViewById(R.id.todoDetails_calendarView);
      mCalendarView.addDateChangeListener(this);

      bind(todo);
    }

    public void bind(@NonNull final Todo todo)
    {
      mTodo = todo;
      setTextDate(mLifeline, todo.lifeline);
      setTextDate(mDeadline, todo.deadline);
      mHardness.setSelection(todo.hardness);
      mCalendarView.setVisibility(View.GONE);
    }

    @NonNull private static GregorianCalendar sRenderCalendar = new GregorianCalendar();
    public static String renderDate(final long date)
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
      mList.scheduleUpdateTodo(mTodo);
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

    @Override
    public void onItemSelected(@NonNull final AdapterView<?> parent, @Nullable final View view, final int position, final long id)
    {
      final Todo.Builder b = new Todo.Builder(mTodo);
      b.setHardness(position);
      mTodo = b.build();
      mList.scheduleUpdateTodo(mTodo);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {
      onItemSelected(parent, null, 0, 0);
    }
  }
}
