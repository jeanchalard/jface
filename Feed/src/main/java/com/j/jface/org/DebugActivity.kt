package com.j.jface.org

import android.widget.Button
import com.j.jface.R
import com.j.jface.lifecycle.WrappedActivity
import com.j.jface.org.notif.NotifEngine
import com.j.jface.org.todo.TodoCore

class DebugActivity(args : WrappedActivity.Args) : WrappedActivity(args)
{
  init
  {
    mA.setContentView(R.layout.debug_activity)
    mA.findViewById<Button>(R.id.try_notif).setOnClickListener {
      val todo = TodoCore("test notif", "X")
      NotifEngine(mA).splitNotification(todo)
    }
  }
}
