package com.j.jface.org

import android.widget.Button
import com.j.jface.R
import com.j.jface.lifecycle.ActivityWrapper
import com.j.jface.lifecycle.WrappedActivity
import com.j.jface.org.notif.NotifEngine
import com.j.jface.org.todo.TodoListReadonlyFullView

class DebugActivityBoot : ActivityWrapper<DebugActivity>()
class DebugActivity(args : WrappedActivity.Args) : WrappedActivity(args)
{
  init
  {
    val lv = TodoListReadonlyFullView(mA)
    mA.setContentView(R.layout.debug_activity)
    mA.findViewById<Button>(R.id.try_notif).setOnClickListener {
      val todo = lv[0]
      NotifEngine(mA).splitNotification(todo)
    }
  }
}
