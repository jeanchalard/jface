package com.j.jface.org.todo;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.UUID;

/**
 * A class representing a single TODO.
 */

public class Todo implements Comparable<Todo>
{
  @NonNull public static final Todo NULL_TODO = new Todo("", "");

  @NonNull public final String mId;
  @NonNull public final String mOrd;
  public final long mCreationTime;
  public final long mCompletionTime;
  @NonNull public final String mText;
  public final int mDepth;
  @NonNull public final Planning mPlanning;
  public final int mEstimatedTime;

  public Todo(@Nullable final String id,
              @NonNull final String ord,
              final long creationTime,
              final long completionTime,
              @NonNull final String text,
              final int depth,
              @Nullable final Planning planning,
              final int estimatedTime)
  {
    mId = null == id ? UUID.randomUUID().toString() : id;
    mOrd = ord;
    mCreationTime = creationTime;
    mCompletionTime = completionTime;
    mText = text;
    mDepth = depth;
    mPlanning = null == planning ? new Planning() : planning;
    mEstimatedTime = estimatedTime;
  }

  public Todo(@NonNull final String text, @NonNull final String ord)
  {
    this(null, ord, System.currentTimeMillis(), 0, text, 0, null, -1);
  }

  public int compareTo(final Todo other)
  {
    return mOrd.compareTo(other.mOrd);
  }

  public static class Builder
  {
    @Nullable private String mId;
    @NonNull private String mOrd;
    private long mCreationTime;
    private long mCompletionTime;
    @NonNull private String mText;
    private int mDepth;
    @Nullable private Planning mPlanning;
    private int mEstimatedTime;

    public Builder(@NonNull final String text, @NonNull final String ord) { mCreationTime = System.currentTimeMillis(); mText = text; mOrd = ord; }
    public Builder(@NonNull final Todo todo) { mId = todo.mId; mOrd = todo.mOrd; mCreationTime = todo.mCreationTime; mCompletionTime = todo.mCompletionTime; mText = todo.mText; mDepth = todo.mDepth; mPlanning = todo.mPlanning; mEstimatedTime = todo.mEstimatedTime; }
    public Builder setId(@Nullable final String id) { mId = id; return this; }
    public Builder setOrd(@NonNull final String ord) { mOrd = ord; return this; }
    public Builder setCompletionTime(final long completionTime) { mCompletionTime = completionTime; return this; }
    public Builder setText(@NonNull final String text) { mText = text; return this; }
    public Builder setDepth(final int depth) { mDepth = depth; return this; }
    public Builder setPlanning(@Nullable final Planning planning) { mPlanning = planning; return this; }
    public Builder setEstimatedTime(@Nullable final int estimatedTime) { mEstimatedTime = estimatedTime; return this; }

    public Todo build()
    {
      return new Todo(mId, mOrd, mCreationTime, mCompletionTime, mText, mDepth, mPlanning, mEstimatedTime);
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
