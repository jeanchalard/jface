package com.j.jface.org.todo;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A class representing a single TODO.
 */

public class Todo implements Comparable<Todo>
{
  @NonNull public final String mId;
  public final long mCreationTime;
  public final long mCompletionTime;
  @NonNull public final String mText;
  public final int mDepth;
  @NonNull public final Planning mPlanning;
  public final int mEstimatedTime;

  public Todo(@NonNull final String id,
              final long creationTime,
              final long completionTime,
              @NonNull final String text,
              final int depth,
              @Nullable final Planning planning,
              final int estimatedTime)
  {
    mId = id;
    mCreationTime = creationTime;
    mCompletionTime = completionTime;
    mText = text;
    mDepth = depth;
    mPlanning = null == planning ? new Planning() : planning;
    mEstimatedTime = estimatedTime;
  }

  public Todo(@NonNull final String text, @NonNull final String id)
  {
    this(id, System.currentTimeMillis(), 0, text, 0, null, -1);
  }

  public int compareTo(final Todo other)
  {
    return mId.compareTo(other.mId);
  }

  public static class Builder
  {
    @NonNull private String mId;
    private long mCreationTime;
    private long mCompletionTime;
    @NonNull private String mText;
    private int mDepth;
    @Nullable private Planning mPlanning;
    private int mEstimatedTime;

    public Builder(@NonNull final String text, @NonNull final String id) { mCreationTime = System.currentTimeMillis(); mText = text; mId = id; }
    public Builder(@NonNull final Todo todo) { mId = todo.mId; mCreationTime = todo.mCreationTime; mCompletionTime = todo.mCompletionTime; mText = todo.mText; mDepth = todo.mDepth; mPlanning = todo.mPlanning; mEstimatedTime = todo.mEstimatedTime; }
    public Builder setId(@Nullable final String id) { mId = id; return this; }
    public Builder setCompletionTime(final long completionTime) { mCompletionTime = completionTime; return this; }
    public Builder setText(@NonNull final String text) { mText = text; return this; }
    public Builder setDepth(final int depth) { mDepth = depth; return this; }
    public Builder setPlanning(@Nullable final Planning planning) { mPlanning = planning; return this; }
    public Builder setEstimatedTime(@Nullable final int estimatedTime) { mEstimatedTime = estimatedTime; return this; }

    public Todo build()
    {
      return new Todo(mId, mCreationTime, mCompletionTime, mText, mDepth, mPlanning, mEstimatedTime);
    }
  }

  private static final char SEPARATOR_ID = ' ';
  private static final char MINIMUM_ID = SEPARATOR_ID + 1;
  private static final char MAXIMUM_ID = '~';
  public static final String SEP_ID = String.valueOf(SEPARATOR_ID);
  public static final String MIN_ID = String.valueOf(MINIMUM_ID);
  public static final String MAX_ID = String.valueOf(MAXIMUM_ID);
  public static final String SEP_MAX_ID = SEP_ID + MAX_ID;
  @NonNull private static String getAppendix(final int length)
  {
    final StringBuffer buf = new StringBuffer(length);
    for (int i = 0; i < length; ++i) buf.append(MINIMUM_ID);
    return buf.toString();
  }
  private static int diffPoint(@NonNull final String id1, @NonNull final String id2)
  {
    int i = 0;
    while (id1.charAt(i) == id2.charAt(i)) ++i;
    return i;
  }

  @NonNull public static String idBetween(@NonNull String id1, @NonNull String id2)
  {
    if (id1.length() != id2.length())
    {
      final String appendix = getAppendix(Math.abs(id1.length() - id2.length()));
      if (id1.length() < id2.length()) id1 = id1 + appendix;
      else id2 = id2 + appendix;
    }
    int diffPoint = diffPoint(id1, id2);
    String id = id1.substring(0, diffPoint);
    char o1, o2, on;
    do
    {
      o1 = id1.charAt(diffPoint);
      o2 = id2.charAt(diffPoint);
      if (o2 == MINIMUM_ID) o2 = MAXIMUM_ID;
      on = (char)((o1 + o2) / 2); // Guaranteed to fit
      id += String.valueOf(on);
      if (o1 != on) return id;
      diffPoint += 1;
      if (diffPoint >= id1.length()) return id + String.valueOf((char)((MINIMUM_ID + MAXIMUM_ID) / 2));
    }
    while(true);
  }
}
