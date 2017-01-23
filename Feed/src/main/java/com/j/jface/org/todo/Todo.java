package com.j.jface.org.todo;

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
  @NonNull public final List<Todo> mChildren;
  @NonNull public final Planning mPlanning;
  public final int mEstimatedTime;

  public Todo(@NonNull final String text,
              @Nullable final Collection<? extends Todo> requirements,
              @Nullable final Collection<? extends Todo> dependents,
              @Nullable final Collection<? extends Todo> children,
              @Nullable final Planning planning,
              final int estimatedTime)
  {
    mText = text;
    mRequirements = null == requirements ? Collections.<Todo>emptyList() : Collections.unmodifiableList(new ArrayList<Todo>(requirements));
    mDependents = null == dependents ? Collections.<Todo>emptyList() : Collections.unmodifiableList(new ArrayList<Todo>(dependents));
    mChildren = null == children ? Collections.<Todo>emptyList() : Collections.unmodifiableList(new ArrayList<Todo>(children));
    mPlanning = null == planning ? new Planning() : planning;
    mEstimatedTime = estimatedTime;
  }

  public Todo(@NonNull final String text)
  {
    this(text, null, null, null, null, -1);
  }

  // Copy constructor with overrides. Any non-null arg is used, while all null is taken from the old Todo.
  public Todo(@NonNull final Todo todo,
              @Nullable final String text,
              @Nullable final Collection<? extends Todo> requirements,
              @Nullable final Collection<? extends Todo> dependents,
              @Nullable final Collection<? extends Todo> children,
              @Nullable final Planning planning,
              final int estimatedTime)
  {
    this(null != text ? text : todo.mText,
         null != requirements ? requirements : !todo.mRequirements.isEmpty() ? todo.mRequirements : null,
         null != dependents ? dependents : !todo.mDependents.isEmpty() ? todo.mDependents : null,
         null != children ? children : !todo.mChildren.isEmpty() ? todo.mChildren : null,
         null != planning ? planning : todo.mPlanning,
         0 != estimatedTime ? estimatedTime : todo.mEstimatedTime);
  }
}
