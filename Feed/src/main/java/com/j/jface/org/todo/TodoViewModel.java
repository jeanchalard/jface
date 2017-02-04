package com.j.jface.org.todo;

import android.support.annotation.NonNull;

// Container for Todo that also encapsulates metadata for UI use.
public class TodoViewModel
{
  @NonNull public final Todo todo;
  public final int flatIndex;
  public final int depth;
  public TodoViewModel(@NonNull final Todo todo, final int flatIndex, final int depth)
  {
    this.todo = todo;
    this.flatIndex = flatIndex;
    this.depth = depth;
  }
}
