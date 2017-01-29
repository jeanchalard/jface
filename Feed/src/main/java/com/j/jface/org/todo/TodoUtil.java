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
      case Planning.ON_WEEKDAY : return "Weekday";
      case Planning.ON_WEEKEND : return "Weekend";
      case Planning.ON_DAYTIME : return "Daytime";
      case Planning.ON_NIGHT :   return "Night";
      case Planning.ON_WEEKDAY | Planning.ON_DAYTIME : return "Business hours";
      case Planning.ON_WEEKDAY | Planning.ON_NIGHT : return "Week night";
      case Planning.ON_WEEKEND | Planning.ON_DAYTIME : return "Weekend day";
      case Planning.ON_WEEKEND | Planning.ON_NIGHT : return "Weekend night";
      default : return "Anytime";
    }
  }

  @NonNull public static String whereString(@Nullable final Fences.Params where)
  {
    if (null == where) return "Anywhere";
    return where.name;
  }
}
