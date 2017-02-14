package com.j.jface.org.todo;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.j.jface.feed.Fences;

public class TodoUtil
{
  @NonNull public static String timeConstraintString(final int timeConstraint)
  {
    switch (timeConstraint)
    {
      case Todo.ON_WEEKDAY : return "Weekday";
      case Todo.ON_WEEKEND : return "Weekend";
      case Todo.ON_DAYTIME : return "Daytime";
      case Todo.ON_NIGHT :   return "Night";
      case Todo.ON_WEEKDAY | Todo.ON_DAYTIME : return "Business hours";
      case Todo.ON_WEEKDAY | Todo.ON_NIGHT : return "Week night";
      case Todo.ON_WEEKEND | Todo.ON_DAYTIME : return "Weekend day";
      case Todo.ON_WEEKEND | Todo.ON_NIGHT : return "Weekend night";
      default : return "Anytime";
    }
  }

  @NonNull public static String whereString(@Nullable final Fences.Params where)
  {
    if (null == where) return "Anywhere";
    return where.name;
  }
}
