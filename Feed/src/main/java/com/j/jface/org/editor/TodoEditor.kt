package com.j.jface.org.editor

import androidx.transition.TransitionManager
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
import com.j.jface.lifecycle.AuthTrampoline
import com.j.jface.lifecycle.WrappedActivity
import com.j.jface.org.notif.FillinNotification
import com.j.jface.org.notif.NotifEngine
import com.j.jface.org.todo.Todo
import com.j.jface.org.todo.TodoUpdaterProxy
import java.util.*

// An activity that provides detailed editing for a single Todo.
class AuthTrampolineTodoEditorBoot : ActivityWrapper<AuthTrampolineTodoEditor>()
class AuthTrampolineTodoEditor(args : WrappedActivity.Args) : AuthTrampoline(args) { override val trampolineDestination get() = TodoEditorBoot::class.java }
class TodoEditorBoot : ActivityWrapper<TodoEditor>()
class TodoEditor(a : WrappedActivity.Args) : WrappedActivity(a)
{
  private val mDetails : TodoDetails

  init
  {
    mA.requestWindowFeature(Window.FEATURE_NO_TITLE)
    mA.setContentView(R.layout.todo_editor)
    val title = mA.findViewById<TextView>(R.id.todoEditor_title)

    val updaterProxy = TodoUpdaterProxy.getInstance(mA)

    val intent = mA.intent
    val todo : Todo
    val openField : Int
    val todoId = intent?.getStringExtra(Const.EXTRA_TODO_ID)
    if (null == intent || null == todoId)
    {
      todo = Todo.NULL_TODO
      openField = -1
    }
    else
    {
      todo = updaterProxy.findById(todoId) ?: Todo.NULL_TODO
      // No need to read the notif ID if the control comes here, as it should have been dismissed by the system already.
      openField = when
      {
        // At this time, only FILLIN notifications implemented
        intent.getIntExtra(Const.EXTRA_NOTIF_TYPE, -1) != NotifEngine.NotifType.FILLIN.ordinal -> -1
        // A reply should never be present
        -1 != intent.getIntExtra(Const.EXTRA_FILLIN_REPLY_INDEX, -1)                           -> -1
        else -> FillinNotification.Field.values()[intent.getIntExtra(Const.EXTRA_FILLIN_FIELD, -1)].fieldId
      }
    }

    title.text = todo.text
    mDetails = TodoDetails(updaterProxy, todo, mA.findViewById(R.id.todoEditor_details))
    if (openField > 0)
    {
      val openView = mA.findViewById<View>(openField)
      openView?.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener
      {
        override fun onViewAttachedToWindow(v : View) { v.performClick() }
        override fun onViewDetachedFromWindow(v : View) = Unit
      })
    }
  }

  companion object
  {
    fun activityClass() = AuthTrampolineTodoEditorBoot::class.java
  }

  class TodoDetails(private val mUpdater : TodoUpdaterProxy, private var mTodo : Todo, private val mRootView : ViewGroup) : CalendarView.DateChangeListener, View.OnClickListener, AdapterView.OnItemSelectedListener, NumericPicker.OnValueChangeListener
  {
    private val mLifeline : TextView = mRootView.findViewById(R.id.todoDetails_lifeline_text)
    private val mDeadline : TextView = mRootView.findViewById(R.id.todoDetails_deadline_text)
    private val mCalendarView : CalendarView = mRootView.findViewById(R.id.todoDetails_calendarView)
    private val mHardness : Spinner = mRootView.findViewById(R.id.todoDetails_hardness)
    private val mConstraint : Spinner = mRootView.findViewById(R.id.todoDetails_constraint)
    private val mEstimatedTimeFiveMinutes : NumericPicker = mRootView.findViewById(R.id.todoDetails_estimatedTime) // By increments of 5'
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

      mEstimatedTimeFiveMinutes.minValue = -1
      mEstimatedTimeFiveMinutes.maxValue = 96
      mEstimatedTimeFiveMinutes.setOnValueChangedListener(this)

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
      mEstimatedTimeFiveMinutes.value = if (mTodo.estimatedMinutes < 0) mTodo.estimatedMinutes else mTodo.estimatedMinutes / 5
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
      val b = mTodo.builder()
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
      if (view.parent === mHardness)
      {
        if (position == mTodo.hardness) return
        mTodo = mTodo.withHardness(position)
      }
      else if (view.parent === mConstraint)
      {
        if (position == mTodo.constraint) return
        mTodo = mTodo.withConstraint(position)
      }
      else return
      mUpdater.updateTodo(mTodo)
    }

    override fun onNothingSelected(parent : AdapterView<*>)
    {
      onItemSelected(parent, null, 0, 0)
    }

    override fun onValueChange(picker : NumericPicker, oldVal : Int, newVal : Int)
    {
      mTodo = mTodo.builder().setEstimatedMinutes(if (newVal < 0) newVal else newVal * 5).build()
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
