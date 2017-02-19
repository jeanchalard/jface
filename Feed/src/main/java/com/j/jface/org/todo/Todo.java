package com.j.jface.org.todo;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.j.jface.feed.Fences;

import java.util.UUID;

/**
 * A class representing a single TODO.
 */

public class Todo implements Comparable<String>
{
  @NonNull public static final Todo NULL_TODO = new Todo("", "");

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

  @NonNull public final String id;
  @NonNull public final String ord;
  public final long creationTime;
  public final long completionTime;
  @NonNull public final String text;
  public final int depth;
  public final long lifeline; // Timestamp : when this can be started
  public final long deadline; // Timestamp : when this has to be done
  public final int hardness; // UNKNOWN or *_DEADLINE
  public final int timeConstraint; // UNKNOWN or ON_*
  public final int estimatedTime;
  @Nullable public final Fences.Params where; // Typically null

  public Todo(@Nullable final String id,
              @NonNull final String ord,
              final long creationTime,
              final long completionTime,
              @NonNull final String text,
              final int depth,
              final long lifeline,
              final long deadline,
              final int hardness,
              final int timeConstraint,
              final int estimatedTime,
              @Nullable final Fences.Params where)
  {
    this.id = null == id ? UUID.randomUUID().toString() : id;
    this.ord = ord;
    this.creationTime = creationTime;
    this.completionTime = completionTime;
    this.text = text;
    this.depth = depth;
    this.lifeline = lifeline;
    this.deadline = deadline;
    this.hardness = hardness;
    this.timeConstraint = timeConstraint;
    this.estimatedTime = estimatedTime;
    this.where = where;
  }

  public Todo(@NonNull final String text, @NonNull final String ord)
  {
    this(null, ord, System.currentTimeMillis(), 0, text, 0, 0, 0, UNKNOWN, UNKNOWN, 0, null);
  }

  public int compareTo(@NonNull final String otherOrd)
  {
    return ord.compareTo(otherOrd);
  }

  public static class Builder
  {
    @Nullable private String id;
    @NonNull private String ord;
    private long creationTime;
    private long completionTime;
    @NonNull private String text;
    private int depth;
    private long lifeline;
    private long deadline;
    private int hardness;
    private int timeConstraint;
    private int estimatedTime;
    @Nullable private Fences.Params where;

    public Builder(@NonNull final String text, @NonNull final String ord) { creationTime = System.currentTimeMillis(); this.text = text; this.ord = ord; this.where = null; }
    public Builder(@NonNull final Todo todo)
    {
      id = todo.id;
      ord = todo.ord;
      creationTime = todo.creationTime;
      completionTime = todo.completionTime;
      text = todo.text;
      depth = todo.depth;
      lifeline = todo.lifeline;
      deadline = todo.deadline;
      hardness = todo.hardness;
      timeConstraint = todo.timeConstraint;
      estimatedTime = todo.estimatedTime;
      where = todo.where;
    }
    public Builder setId(@Nullable final String id) { this.id = id; return this; }
    public Builder setOrd(@NonNull final String ord) { this.ord = ord; return this; }
    public Builder setCompletionTime(final long completionTime) { this.completionTime = completionTime; return this; }
    public Builder setText(@NonNull final String text) { this.text = text; return this; }
    public Builder setDepth(final int depth) { this.depth = depth; return this; }
    public Builder setLifeline(final long lifeline) { this.lifeline = lifeline; return this; }
    public Builder setDeadline(final long deadline) { this.deadline = deadline; return this; }
    public Builder setHardness(final int hardness) { this.hardness = hardness; return this; }
    public Builder setTimeConstraint(final int timeConstraint) { this.timeConstraint = timeConstraint; return this; }
    public Builder setEstimatedTime(final int estimatedTime) { this.estimatedTime = estimatedTime; return this; }
    public Builder setWhere(@Nullable final Fences.Params where) { this.where = where; return this; }

    public Todo build()
    {
      return new Todo(id, ord, creationTime, completionTime, text, depth, lifeline, deadline, hardness, timeConstraint, estimatedTime, where);
    }
  }

  private static final char SEPARATOR_ORD = ' ';
  private static final char MINIMUM_ORD = SEPARATOR_ORD + 1;
  private static final char MAXIMUM_ORD = '~';
  public static final String SEP_ORD = String.valueOf(SEPARATOR_ORD);
  public static final String MIN_ORD = String.valueOf(MINIMUM_ORD);
  public static final String MAX_ORD = String.valueOf(MAXIMUM_ORD);
  public static final String SEP_MAX_ORD = SEP_ORD + MAX_ORD;
  @NonNull private static String getAppendix(final int length)
  {
    final StringBuffer buf = new StringBuffer(length);
    for (int i = 0; i < length; ++i) buf.append(MINIMUM_ORD);
    return buf.toString();
  }
  private static int diffPoint(@NonNull final String ord1, @NonNull final String ord2)
  {
    int i = 0;
    while (ord1.charAt(i) == ord2.charAt(i)) ++i;
    return i;
  }

  @NonNull public static String ordBetween(@NonNull String ord1, @NonNull String ord2)
  {
    if (ord1.length() != ord2.length())
    {
      final String appendix = getAppendix(Math.abs(ord1.length() - ord2.length()));
      if (ord1.length() < ord2.length()) ord1 = ord1 + appendix;
      else ord2 = ord2 + appendix;
    }
    int diffPoint = diffPoint(ord1, ord2);
    String id = ord1.substring(0, diffPoint);
    char o1, o2, on;
    do
    {
      o1 = ord1.charAt(diffPoint);
      o2 = ord2.charAt(diffPoint);
      if (o2 == MINIMUM_ORD) o2 = MAXIMUM_ORD;
      on = (char)((o1 + o2) / 2); // Guaranteed to fit
      id += String.valueOf(on);
      if (o1 != on) return id;
      diffPoint += 1;
      if (diffPoint >= ord1.length()) return id + String.valueOf((char)((MINIMUM_ORD + MAXIMUM_ORD) / 2));
    }
    while(true);
  }
}
