package com.j.jface.org.notif

import android.app.Notification
import com.j.jface.org.todo.TodoCore
import com.j.jface.org.todo.TodoListView

interface NotificationBuilder
{
  fun remainingItems(list : TodoListView) : List<TodoCore>
  fun buildNotification(id : Int, todo : TodoCore) : Notification
}
