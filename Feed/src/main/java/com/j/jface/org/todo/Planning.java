package com.j.jface.org.todo;

import android.support.annotation.Nullable;

import com.j.jface.feed.Fences;

/**
 * A planning descriptor.
 *
 * This knows when a TODO needs to be done and where, and whether the deadline is soft or hard.
 */

public class Planning
{
  public static final int UNKNOWN = 0;

  // Deadline hardnesses
  public static final int SOFT_DEADLINE = 1; // Decided myself
  public static final int SEMIHARD_DEADLINE = 2; // Important, but not the end of the world if missed
  public static final int HARD_DEADLINE = 3; // Really needs to be done

  // Add other patterns here if ever necessary
  public static final int ON_WEEKDAY = 1;
  public static final int ON_WEEKEND = 2;
  public static final int ON_DAYTIME = 4;
  public static final int ON_NIGHT = 8;

  public final int mLifeline; // Timestamp : when this can be started
  public final int mDeadline; // Timestamp : when this has to be done
  public final int mHardness; // UNKNOWN or *_DEADLINE
  public final int mTimeConstraint; // UNKNOWN or ON_*
  @Nullable public final Fences.Params mWhere; // Typically null

  public Planning(final int lifeline, final int deadline, final int hardness, final int timeConstraint, @Nullable final Fences.Params where)
  {
    mLifeline = lifeline;
    mDeadline = deadline;
    mHardness = hardness;
    mTimeConstraint = timeConstraint;
    mWhere = where;
  }

  public Planning()
  {
    this(0, 0, UNKNOWN, UNKNOWN, null);
  }
}
