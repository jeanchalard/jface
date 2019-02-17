package com.j.jface.org.todo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

// TODO : rework the class hierarchy, this is ugly. There should be a read-only view, then
// update methods, and the classes doing the work should inherit and implement as needed.
public interface TodoUpdaterProxy
{
  static TodoUpdaterProxy getInstance(@NonNull final Context context) { return TodoList.getInstance(context); }
  @Nullable Todo findById(@NonNull final String todoId);
  @Nullable public Todo findByOrd(@NonNull final String ord);
  @NonNull Todo updateTodo(@NonNull final Todo todo);
}
