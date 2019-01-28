package com.j.jface.org.notif

import android.app.Notification
import com.j.jface.org.todo.TodoCore

interface NotificationBuilder
{
  fun buildNotification(id : Int, todo : TodoCore) : Notification
}
