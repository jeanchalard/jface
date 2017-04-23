package com.j.jface.org.todo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

/**
 * A proxy class that lets a single Todo editor get a todo and update a todo.
 */
public class TodoUpdaterProxy
{
  @NonNull final private TodoList mList;

  public TodoUpdaterProxy(@NonNull final Context context) { this(TodoList.getInstance(context)); }
  public TodoUpdaterProxy(@NonNull final TodoList list) { mList = list; }
  @NonNull public Todo scheduleUpdateTodo(@NonNull final Todo todo) { return mList.scheduleUpdateTodo(todo); }
  @NonNull public Todo getFromId(@Nullable final String id)
  {
    if (TextUtils.isEmpty(id)) return Todo.NULL_TODO;
    final Todo t = mList.getFromId(id);
    return null == t ? Todo.NULL_TODO : t;
  }
}
