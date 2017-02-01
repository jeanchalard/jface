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
  @Nullable public final Todo mParent;
  @NonNull public final List<Todo> mRequirements;
  @NonNull public final List<Todo> mDependents;
  @NonNull public final List<Todo> mChildren;
  @NonNull public final Planning mPlanning;
  public final int mEstimatedTime;

  public Todo(@NonNull final String text,
              @Nullable final Todo parent,
              @Nullable final Collection<? extends Todo> requirements,
              @Nullable final Collection<? extends Todo> dependents,
              @Nullable final Collection<? extends Todo> children,
              @Nullable final Planning planning,
              final int estimatedTime)
  {
    mText = text;
    mParent = parent;
    mRequirements = null == requirements ? Collections.<Todo>emptyList() : Collections.unmodifiableList(new ArrayList<>(requirements));
    mDependents = null == dependents ? Collections.<Todo>emptyList() : Collections.unmodifiableList(new ArrayList<>(dependents));
    mChildren = null == children ? Collections.<Todo>emptyList() : Collections.unmodifiableList(new ArrayList<>(children));
    mPlanning = null == planning ? new Planning() : planning;
    mEstimatedTime = estimatedTime;
  }

  public Todo(@NonNull final String text)
  {
    this(text, null, null, null, null, null, -1);
  }

  public class Builder
  {
    @NonNull private String mText;
    @Nullable private Todo mParent;
    @Nullable private List<Todo> mRequirements;
    @Nullable private List<Todo> mDependents;
    @Nullable private List<Todo> mChildren;
    @Nullable private Planning mPlanning;
    private int mEstimatedTime;

    public Builder(@NonNull final String text) { mText = text; }
    public Builder(@NonNull final Todo todo) { mText = todo.mText; mParent = todo.mParent; mRequirements = todo.mRequirements; mDependents = todo.mDependents; mChildren = todo.mChildren; mPlanning = todo.mPlanning; mEstimatedTime = todo.mEstimatedTime; }
    public Builder setParent(@Nullable final Todo parent) { mParent = parent; return this; }
    public Builder setRequirements(@Nullable final List<Todo> requirements) { mRequirements = requirements; return this; }
    public Builder setDependents(@Nullable final List<Todo> dependents) { mDependents = dependents; return this; }
    public Builder setChildren(@Nullable final List<Todo> children) { mChildren = children; return this; }
    public Builder setPlanning(@Nullable final Planning planning) { mPlanning = planning; return this; }
    public Builder setEstimatedTime(@Nullable final int estimatedTime) { mEstimatedTime = estimatedTime; return this; }

    public Todo build()
    {
      return new Todo(mText, mParent, mRequirements, mDependents, mChildren, mPlanning, mEstimatedTime);
    }
  }
}
