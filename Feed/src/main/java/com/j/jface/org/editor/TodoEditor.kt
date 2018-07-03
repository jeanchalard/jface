package com.j.jface.org.editor

import android.support.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.lifecycle.ActivityWrapper
import com.j.jface.lifecycle.WrappedActivity
import com.j.jface.org.todo.Todo
import com.j.jface.org.todo.TodoUpdaterProxy
import java.util.*

// An activity that provides detailed editing for a single Todo.
class TodoEditorBoot : ActivityWrapper<TodoEditor>()
class TodoEditor protected constructor(a : WrappedActivity.Args) : WrappedActivity(a)
{
  private val mDetails : TodoDetails

  init
  {
    mA.requestWindowFeature(Window.FEATURE_NO_TITLE)
    mA.setContentView(R.layout.todo_editor)
    val title = mA.findViewById<TextView>(R.id.todoEditor_title)

    val intent = mA.intent
    val todoId = intent?.getStringExtra(Const.EXTRA_TODO_ID)
    val updaterProxy = TodoUpdaterProxy.getInstance(mA)
    val t = if (null == todoId) Todo.NULL_TODO else updaterProxy.getFromId(todoId)
    val todo = t ?: Todo.NULL_TODO

    title.text = todo.text
    mDetails = TodoDetails(updaterProxy, todo, mA.findViewById(R.id.todoEditor_details))
  }

  class TodoDetails(private val mUpdater : TodoUpdaterProxy, private var mTodo : Todo, private val mRootView : ViewGroup) : CalendarView.DateChangeListener, View.OnClickListener, AdapterView.OnItemSelectedListener, NumericPicker.OnValueChangeListener
  {
    private val mLifeline : TextView = mRootView.findViewById(R.id.todoDetails_lifeline_text)
    private val mDeadline : TextView = mRootView.findViewById(R.id.todoDetails_deadline_text)
    private val mCalendarView : CalendarView = mRootView.findViewById(R.id.todoDetails_calendarView)
    private val mHardness : Spinner = mRootView.findViewById(R.id.todoDetails_hardness)
    private val mConstraint : Spinner = mRootView.findViewById(R.id.todoDetails_constraint)
    private val mEstimatedTime : NumericPicker = mRootView.findViewById(R.id.todoDetails_estimatedTime)
    private var mEditing : TextView? = null

    init
    {
      cleanupSpinner(mHardness)
      cleanupSpinner(mConstraint)

      mLifeline.setOnClickListener(this)
      mDeadline.setOnClickListener(this)
      mCalendarView.addDateChangeListener(this)

      mHardness.onItemSelectedListener = this
      val adapter = ArrayAdapter<CharSequence>(mRootView.context, android.R.layout.simple_spinner_item, Todo.CONSTRAINT_NAMES)
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
      mConstraint.adapter = adapter
      mConstraint.onItemSelectedListener = this
      mConstraint.setBackgroundResource(R.drawable.rectangle)

      mEstimatedTime.minValue = -1
      mEstimatedTime.maxValue = 96
      mEstimatedTime.setOnValueChangedListener(this)

      bind(mTodo)
    }

    private fun cleanupSpinner(s : Spinner)
    {
      // Remove the huge and useless arrow from the spinner
      s.background = null
      s.setPadding(0, 0, 0, 0)
      s.onItemSelectedListener = this
    }

    fun bind(todo : Todo)
    {
      mTodo = todo
      setTextDate(mLifeline, todo.lifeline)
      setTextDate(mDeadline, todo.deadline)
      mHardness.setSelection(todo.hardness)
      mConstraint.setSelection(todo.constraint)
      mEstimatedTime.value = if (mTodo.estimatedTime < 0) mTodo.estimatedTime else mTodo.estimatedTime / 5
      mCalendarView.visibility = View.GONE
    }

    private fun setTextDate(textView : TextView, date : Long)
    {
      textView.text = renderDate(date)
      if (date > 0)
        textView.background = null
      else
        textView.setBackgroundResource(R.drawable.rectangle)
      if (textView === mEditing)
        textView.setBackgroundResource(R.drawable.red_rectangle)
    }

    override fun onDateChanged(newDate : Long)
    {
      val editing = mEditing ?: return
      setTextDate(editing, newDate)
      val b = Todo.Builder(mTodo)
      if (mLifeline === editing)
        b.setLifeline(newDate)
      else
        b.setDeadline(newDate)
      mTodo = b.build()
      mUpdater.updateTodo(mTodo)
      if (0L == newDate)
      {
        TransitionManager.beginDelayedTransition(mRootView)
        mCalendarView.visibility = View.GONE
      }
    }

    override fun onClick(v : View)
    {
      mDeadline.background = null
      mLifeline.background = null
      TransitionManager.beginDelayedTransition(mRootView)
      // Either Lifeline or Deadline
      if (v === mEditing)
        mCalendarView.visibility = View.GONE
      else
      {
        val date = if (v === mDeadline) mTodo.deadline else mTodo.lifeline
        v.setBackgroundResource(R.drawable.red_rectangle)
        mEditing = v as TextView
        mCalendarView.setDate(if (date > 0) date else System.currentTimeMillis())
        mCalendarView.visibility = View.VISIBLE
      }
    }

    override fun onItemSelected(parent : AdapterView<*>, view : View?, position : Int, id : Long)
    {
      if (null == view) return
      val b : Todo.Builder
      if (view.parent === mHardness)
      {
        if (position == mTodo.hardness) return
        b = Todo.Builder(mTodo)
        b.setHardness(position)
      }
      else if (view.parent === mConstraint)
      {
        if (position == mTodo.constraint) return
        b = Todo.Builder(mTodo)
        b.setConstraint(position)
      }
      else return
      mTodo = b.build()
      mUpdater.updateTodo(mTodo)
    }

    override fun onNothingSelected(parent : AdapterView<*>)
    {
      onItemSelected(parent, null, 0, 0)
    }

    override fun onValueChange(picker : NumericPicker, oldVal : Int, newVal : Int)
    {
      mTodo = Todo.Builder(mTodo).setEstimatedTime(if (newVal < 0) newVal else newVal * 5).build()
      mUpdater.updateTodo(mTodo)
    }

    companion object
    {

      private val sRenderCalendar = GregorianCalendar()
      private fun renderDate(date : Long) : String
      {
        if (0L == date) return "—"
        if (-1L == date) return "?"
        synchronized(sRenderCalendar) {
          sRenderCalendar.timeInMillis = date
          return String.format(Locale.JAPAN, "%04d-%02d-%02d (%s) %02d:%02d",
           sRenderCalendar.get(Calendar.YEAR), sRenderCalendar.get(Calendar.MONTH) + 1, sRenderCalendar.get(Calendar.DAY_OF_MONTH),
           Const.WEEKDAYS[sRenderCalendar.get(Calendar.DAY_OF_WEEK) - 1], sRenderCalendar.get(Calendar.HOUR_OF_DAY), sRenderCalendar.get(Calendar.MINUTE))
        }
      }
    }
  }
}
