package com.j.jface.todo;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A class representing a single TODO.
 */

public class Todo
{
  @NonNull public final String mText;
  @NonNull public final List<Todo> mRequirements;
  @NonNull public final List<Todo> mDependents;
  @NonNull public final Planning mPlanning;

  public Todo(@NonNull final String text,
              @Nullable final Collection<? extends Todo> requirements,
              @Nullable final Collection<? extends Todo> dependents,
              @Nullable final Planning planning)
  {
    mText = text;
    mRequirements = null == requirements ? Collections.<Todo>emptyList() : Collections.unmodifiableList(new ArrayList<Todo>(requirements));
    mDependents = null == dependents ? Collections.<Todo>emptyList() : (ArrayList<Todo>)Collections.unmodifiableList(new ArrayList<Todo>(dependents));
    mPlanning = null == planning ? new Planning() : planning;
  }

  public Todo(@NonNull final String text)
  {
    this(text, null, null, null);
  }

  // Copy constructor with overrides. Any non-null arg is used, while all null is taken from the old Todo.
  public Todo(@NonNull final Todo todo,
              @Nullable final String text,
              @Nullable final Collection<? extends Todo> requirements,
              @Nullable final Collection<? extends Todo> dependents,
              @Nullable final Planning planning)
  {
    this(null != text ? text : todo.mText,
         null != requirements ? requirements : !todo.mRequirements.isEmpty() ? todo.mRequirements : null,
         null != dependents ? dependents : !todo.mDependents.isEmpty() ? todo.mDependents : null,
         null != planning ? planning : todo.mPlanning);
  }
}
