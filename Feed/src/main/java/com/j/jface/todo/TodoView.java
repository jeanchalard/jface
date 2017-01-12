package com.j.jface.todo;

import android.content.Context;
import android.view.ViewGroup;

/**
 * A view representing a single TODO. It has a collapsed and an expanded mode.
 */

public class TodoView extends ViewGroup
{
  public final Todo mTodo;

  public TodoView(final Context context, final Todo todo)
  {
    super(context);
    mTodo = todo;
  }
}
