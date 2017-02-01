package com.j.jface.org.todo;

import android.support.annotation.NonNull;

import com.j.jface.Const;

// Contract constants for the todo provider.
public class TodoProviderContract
{
  @NonNull public static final String BASE_URI = Const.APP_PACKAGE + ".provider";
  @NonNull public static final String TABLE = "todo";

  @NonNull public static final String MIMETYPE_TODOLIST = "vnd.android.cursor.dir/vnd.com.j.jface.todolist";
  @NonNull public static final String MIMETYPE_TODO = "vnd.android.cursor.dir/vnd.com.j.jface.todo";
}
