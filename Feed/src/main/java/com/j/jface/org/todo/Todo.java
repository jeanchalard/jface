package com.j.jface.org.todo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.Exclude;

public class Todo extends TodoCore
{
  @NonNull public static final Todo NULL_TODO = new Todo("", "");

  /**
   * A class to store UI-related Todo stuff. It encapsulates the mutable
   * part of Todo, the parts that Todo adds over TodoCore.
   * This stores data that is either not persisted but kept for
   * performance reasons, or local to this device.
   */
  public static class TodoUI
  {
    @Nullable public final Todo parent;
    public boolean open;
    public boolean leaf;
    public boolean lastChild;

    public TodoUI()
    {
      this(null, true, true);
    }

    public TodoUI(@Nullable final Todo parent, final boolean open, final boolean lastChild)
    {
      this.parent = parent;
      this.open = open;
      this.leaf = true;
      this.lastChild = lastChild;
    }

    public TodoUI(@NonNull final TodoUI ui)
    {
      this(ui.parent, ui.open, ui.lastChild);
      this.leaf = ui.leaf;
    }

    public boolean equals(@Nullable final TodoUI other)
    {
      if (null == other) return false;
      if (other.open != open
       || other.leaf != leaf
       || other.lastChild != lastChild) return false;
      if (null == other.parent) return null == parent;
      if (null == parent) return false;
      return other.parent.id.equals(parent.id);
    }
  }

  // @Exclude : do not write to firestore.
  @NonNull @Exclude public final TodoUI ui; // The members of this member are mutable.
  public Todo(@Nullable final String id,
              @NonNull final String ord,
              final long creationTime,
              final long completionTime,
              @NonNull final String text,
              final int depth,
              final long lifeline,
              final long deadline,
              final int hardness,
              final int constraint,
              final int estimatedTimeMinutes,
              final long lastUpdateTime,
              @NonNull final TodoUI params)
  {
    super(id, ord, creationTime, completionTime, text, depth, lifeline, deadline, hardness, constraint, estimatedTimeMinutes, lastUpdateTime);
    ui = params;
  }

  public Todo(@NonNull final String text, @NonNull final String ord)
  {
    super(text, ord);
    ui = new TodoUI(null, true, true);
  }

  public boolean equals(@Nullable final Object other)
  {
    if (!super.equals(other)) return false;
    if (other instanceof Todo)
      return ui.equals(((Todo)other).ui);
    else return other instanceof TodoCore;
  }

  public static class Builder extends TodoCore.TodoBuilder<Builder>
  {
    @Nullable private Todo parent;
    private boolean open;
    private boolean leaf;
    private boolean lastChild;

    public Builder(@NonNull final String text, @NonNull final String ord) { super(text, ord); }
    public Builder(@NonNull final TodoCore todoCore)
    {
      super(todoCore);
      if (todoCore instanceof Todo)
      {
        final Todo todo = (Todo)todoCore;
        parent = todo.ui.parent;
        open = todo.ui.open;
        leaf = todo.ui.leaf;
        lastChild = todo.ui.lastChild;
      }
      else
      {
        parent = null;
        open = true;
        leaf = true;
        lastChild = false;
      }
    }
    public Builder setParent(@Nullable final Todo parent) { this.parent = parent; return this; }
    public Builder setOpen(final boolean open) { this.open = open; return this; }
    public Builder setLeaf(final boolean leaf) { this.leaf = leaf; return this; }
    public Builder setLastChild(final boolean lastChild) { this.lastChild = lastChild; return this; }

    @Override public Todo build()
    {
      final TodoUI ui = new TodoUI(parent, open, lastChild);
      ui.leaf = leaf;
      return new Todo(id, ord, creationTime, completionTime, text, depth, lifeline, deadline, hardness, constraint, estimatedMinutes, lastUpdateTime, ui);
    }
  }

  @Override @NonNull public Builder builder()
  {
    return new Builder(this);
  }

  // Utility functions
  public boolean descendsFrom(@Nullable final Todo parent)
  {
    for (Todo cp = ui.parent; cp != null; cp = cp.ui.parent)
      if (parent == cp) return true;
    return null == parent;
  }
}
