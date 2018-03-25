package com.j.jface.org.todo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.j.jface.action.GThread;

public interface TodoUpdaterProxy
{
  static TodoUpdaterProxy getInstance(@NonNull final GThread gThread, @NonNull final Context context) { return TodoList.getInstance(gThread, context); }
  @Nullable Todo getFromId(@NonNull final String todoId);
  @NonNull Todo scheduleUpdateTodo(@NonNull final Todo todo);
}
