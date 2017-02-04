package com.j.jface.org.todo;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A class representing a single TODO.
 */

public class Todo
{
  @Nullable public final String mId;
  public final long mCreationTime;
  @NonNull public final String mText;
  @Nullable public final Todo mParent;
  @NonNull public final List<Todo> mRequirements;
  @NonNull public final List<Todo> mDependents;
  @NonNull public final List<Todo> mChildren;
  @NonNull public final Planning mPlanning;
  public final int mEstimatedTime;

  public Todo(@Nullable final String id,
               final long creationTime,
               @NonNull final String text,
               @Nullable final Todo parent,
               @Nullable final Collection<? extends Todo> requirements,
               @Nullable final Collection<? extends Todo> dependents,
               @Nullable final Collection<? extends Todo> children,
               @Nullable final Planning planning,
               final int estimatedTime)
  {
    mId = id;
    mCreationTime = creationTime;
    mText = text;
    mParent = parent;
    mRequirements = null == requirements ? new ArrayList<Todo>() : new ArrayList<>(requirements);
    mDependents = null == dependents ? new ArrayList<Todo>() : new ArrayList<>(dependents);
    mChildren = null == children ? new ArrayList<Todo>() : new ArrayList<>(children);
    mPlanning = null == planning ? new Planning() : planning;
    mEstimatedTime = estimatedTime;
  }

  public Todo(@NonNull final String text)
  {
    this(null, System.currentTimeMillis(), text, null, null, null, null, null, -1);
  }

  public static class Builder
  {
    @Nullable private String mId;
    private long mCreationTime;
    @NonNull private String mText;
    @Nullable private Todo mParent;
    @Nullable private List<Todo> mRequirements;
    @Nullable private List<Todo> mDependents;
    @Nullable private List<Todo> mChildren;
    @Nullable private Planning mPlanning;
    private int mEstimatedTime;

    public Builder(@NonNull final String text) { mCreationTime = System.currentTimeMillis(); mText = text; }
    public Builder(@NonNull final Todo todo) { mId = todo.mId; mCreationTime = todo.mCreationTime; mText = todo.mText; mParent = todo.mParent; mRequirements = todo.mRequirements; mDependents = todo.mDependents; mChildren = todo.mChildren; mPlanning = todo.mPlanning; mEstimatedTime = todo.mEstimatedTime; }
    public Builder setId(@Nullable final String id) { mId = id; return this; }
    public Builder setParent(@Nullable final Todo parent) { mParent = parent; return this; }
    public Builder setRequirements(@Nullable final List<Todo> requirements) { mRequirements = requirements; return this; }
    public Builder setDependents(@Nullable final List<Todo> dependents) { mDependents = dependents; return this; }
    public Builder setChildren(@Nullable final List<Todo> children) { mChildren = children; return this; }
    public Builder setPlanning(@Nullable final Planning planning) { mPlanning = planning; return this; }
    public Builder setEstimatedTime(@Nullable final int estimatedTime) { mEstimatedTime = estimatedTime; return this; }

    public Todo build()
    {
      return new Todo(mId, mCreationTime, mText, mParent, mRequirements, mDependents, mChildren, mPlanning, mEstimatedTime);
    }
  }
}
