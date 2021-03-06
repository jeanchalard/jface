package com.j.jface.org.todo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

/**
 * A class representing a single TODO.
 */
@SuppressWarnings("unchecked")
public class TodoCore implements Comparable<String>
{
  public static final int UNKNOWN = 0;

  // Deadline hardnesses
  public static final int DEADLINE_SOFT = 1; // Decided myself but no practical reason for this specific date
  public static final int DEADLINE_SEMIHARD = 2; // Significant but not crippling drawbacks if missed
  public static final int DEADLINE_HARD = 3; // Really needs to be done

  // Add other patterns here if ever necessary
  public static final int ON_HOME = 1;
  public static final int ON_WORK = 2;
  public static final int ON_WEEKNIGHT = 3;
  public static final int ON_WEEKEND = 4;
  public static final int ON_NIGHT = 5;
  public static final int ON_ANY = 6;
  public static final String[] CONSTRAINT_NAMES = new String[]{"Any", "Home", "Work", "Weeknight", "Weekend", "Night"};

  private static final char SEPARATOR_ORD = ' ';
  private static final char MINIMUM_ORD = SEPARATOR_ORD + 1;
  private static final char MAXIMUM_ORD = '~';
  public static final String SEP_ORD = String.valueOf(SEPARATOR_ORD);
  public static final String MIN_ORD = String.valueOf(MINIMUM_ORD);
  public static final String MAX_ORD = String.valueOf(MAXIMUM_ORD);
  public static final String SEP_MAX_ORD = SEP_ORD + MAX_ORD;

  @NonNull public final String id;
  @NonNull public final String ord;
  public final long creationTime;
  public final long completionTime;
  @NonNull public final String text;
  public final int depth;
  public final long lifeline; // Timestamp : when this can be started
  public final long deadline; // Timestamp : when this has to be done
  public final int hardness; // UNKNOWN or *_DEADLINE
  public final int constraint; // ANY or ON_*
  public final int estimatedMinutes; // In minutes
  public final long lastUpdateTime;

  public TodoCore(@NonNull final String text, @NonNull final String ord) { this(null, ord, System.currentTimeMillis(), 0, text, 0, 0, 0, UNKNOWN, UNKNOWN, -1, System.currentTimeMillis()); }
  public TodoCore(@Nullable final String id,
                  @NonNull final String ord,
                  final long creationTime,
                  final long completionTime,
                  @NonNull final String text,
                  final int depth,
                  final long lifeline,
                  final long deadline,
                  final int hardness,
                  final int constraint,
                  final int estimatedMinutes,
                  final long lastUpdateTime)
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
    this.constraint = constraint;
    this.estimatedMinutes = estimatedMinutes;
    this.lastUpdateTime = lastUpdateTime;
  }

  @NonNull private static String getAppendix(final int length)
  {
    final StringBuilder buf = new StringBuilder(length);
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
    StringBuilder id = new StringBuilder(ord1.substring(0, diffPoint));
    char o1, o2, on;
    do
    {
      o1 = ord1.charAt(diffPoint);
      o2 = ord2.charAt(diffPoint);
      if (o2 == MINIMUM_ORD) o2 = MAXIMUM_ORD;
      on = (char)((o1 + o2) / 2); // Guaranteed to fit
      id.append(String.valueOf(on));
      if (o1 != on) return id.toString();
      diffPoint += 1;
      if (diffPoint >= ord1.length())
        return id + String.valueOf((char)((MINIMUM_ORD + MAXIMUM_ORD) / 2));
    }
    while (true);
  }

  @Override public String toString() {
    final StringBuilder b = new StringBuilder("TODO : \"");
    for (int i = depth; i > 0; --i) b.append("> ");
    b.append(text);
    b.append("\"");
    return b.toString();
  }

  public static class TodoBuilder<U extends TodoBuilder>
  {
    @Nullable protected String id;
    @NonNull protected String ord;
    protected long creationTime;
    protected long completionTime;
    @NonNull protected String text;
    protected int depth;
    protected long lifeline;
    protected long deadline;
    protected int hardness;
    protected int constraint;
    protected int estimatedMinutes;
    protected long lastUpdateTime;

    public TodoBuilder(@NonNull final String text, @NonNull final String ord) { creationTime = System.currentTimeMillis(); this.text = text; this.ord = ord; this.estimatedMinutes = -1; }
    public TodoBuilder(@NonNull final TodoCore todo)
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
      constraint = todo.constraint;
      estimatedMinutes = todo.estimatedMinutes;
      lastUpdateTime = todo.lastUpdateTime;
    }
    public U setId              (@Nullable final String id)  { this.id               = id;               return (U) this; }
    public U setOrd             (@NonNull final String ord)  { this.ord              = ord;              return (U) this; }
    public U setCompletionTime  (final long completionTime)  { this.completionTime   = completionTime;   return (U) this; }
    public U setText            (@NonNull final String text) { this.text             = text;             return (U) this; }
    public U setDepth           (final int depth)            { this.depth            = depth;            return (U) this; }
    public U setLifeline        (final long lifeline)        { this.lifeline         = lifeline;         return (U) this; }
    public U setDeadline        (final long deadline)        { this.deadline         = deadline;         return (U) this; }
    public U setHardness        (final int hardness)         { this.hardness         = hardness;         return (U) this; }
    public U setConstraint      (final int constraint)       { this.constraint       = constraint;       return (U) this; }
    public U setEstimatedMinutes(final int estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; return (U) this; }
    public U setLastUpdateTime  (final long lastUpdateTime)  { this.lastUpdateTime   = lastUpdateTime;   return (U) this; }

    public <T extends TodoCore> T build() {
      return (T) new TodoCore(id, ord, creationTime, completionTime, text, depth, lifeline, deadline, hardness, constraint, estimatedMinutes, lastUpdateTime);
    }
  }
  public class Builder extends TodoBuilder<Builder> {
    public Builder(@NonNull final String text, @NonNull final String ord) { super(text, ord); }
    public Builder(@NonNull final TodoCore todo) { super(todo); }
  }

  public <T extends TodoCore> T withCompletionTime  (final long completionTime)  { return (T) builder().setCompletionTime(completionTime).build(); }
  public <T extends TodoCore> T withText            (@NonNull final String text) { return (T) builder().setText(text).build(); }
  public <T extends TodoCore> T withDepth           (final int depth)            { return (T) builder().setDepth(depth).build(); }
  public <T extends TodoCore> T withLifeline        (final long lifeline)        { return (T) builder().setLifeline(lifeline).build(); }
  public <T extends TodoCore> T withDeadline        (final long deadline)        { return (T) builder().setDeadline(deadline).build(); }
  public <T extends TodoCore> T withHardness        (final int hardness)         { return (T) builder().setHardness(hardness).build(); }
  public <T extends TodoCore> T withConstraint      (final int constraint)       { return (T) builder().setConstraint(constraint).build(); }
  public <T extends TodoCore> T withEstimatedMinutes(final int estimatedMinutes) { return (T) builder().setEstimatedMinutes(estimatedMinutes).build(); }
  public <T extends TodoCore> T withLastUpdateTime  (final long lastUpdateTime)  { return (T) builder().setLastUpdateTime(lastUpdateTime).build(); }

  public int compareTo(@NonNull final String otherOrd)
  {
    return ord.compareTo(otherOrd);
  }

  public boolean equals(@Nullable final Object o)
  {
    if (!(o instanceof TodoCore)) return false;
    final TodoCore other = (TodoCore)o;
    return other.id.equals(id)
     && other.ord.equals(ord)
     && other.creationTime == creationTime
     && other.completionTime == completionTime
     && other.text.equals(text)
     && other.depth == depth
     && other.lifeline == lifeline
     && other.deadline == deadline
     && other.hardness == hardness
     && other.constraint == constraint
     && other.estimatedMinutes == estimatedMinutes
     && other.lastUpdateTime == lastUpdateTime;
  }

  @NonNull public <U extends TodoBuilder> U builder()
  {
    return (U) new Builder(this);
  }
}
