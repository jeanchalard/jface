package com.j.jface.org.todo;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class Todo extends TodoCore
{
  @NonNull public static final Todo NULL_TODO = new Todo("", "");

  /**
   * A class to store UI-related Todo stuff. It encapsulates the mutable part of Todo, the parts that Todo adds over TodoCore.
   * This stores data that is either not persisted but kept for performance reasons, or local to this device.
   */
  public static class TodoUI
  {
    @Nullable public final Todo parent;
    public boolean open;
    public boolean allHierarchyOpen; // Whether this is visible ; an item is visible if all of its parents are open.
    public boolean leaf;
    public boolean lastChild;

    public TodoUI()
    {
      this(null, true, true, true);
    }

    public TodoUI(@Nullable final Todo parent, final boolean open, final boolean allHierarchyOpen, final boolean lastChild)
    {
      this.parent = parent;
      this.open = open;
      this.allHierarchyOpen = allHierarchyOpen;
      this.leaf = true;
      this.lastChild = lastChild;
    }

    public TodoUI(@NonNull final TodoUI ui)
    {
      this(ui.parent, ui.open, ui.allHierarchyOpen, ui.lastChild);
      this.leaf = ui.leaf;
    }
  }

  @NonNull public final TodoUI ui; // The members of this member are mutable.
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
              final int estimatedTime,
              @NonNull final TodoUI params)
  {
    super(id, ord, creationTime, completionTime, text, depth, lifeline, deadline, hardness, constraint, estimatedTime);
    ui = params;
  }

  public Todo(@NonNull final String text, @NonNull final String ord)
  {
    super(text, ord);
    ui = new TodoUI(null, true, true, true);
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
    private int constraint;
    private int estimatedTime;
    @Nullable public Todo parent;
    public boolean open;
    public boolean allHierarchyOpen;
    public boolean leaf;
    public boolean lastChild;

    public Builder(@NonNull final String text, @NonNull final String ord) { creationTime = System.currentTimeMillis(); this.text = text; this.ord = ord; this.estimatedTime = -1; }
    public Builder(@NonNull final TodoCore todoCore)
    {
      id = todoCore.id;
      ord = todoCore.ord;
      creationTime = todoCore.creationTime;
      completionTime = todoCore.completionTime;
      text = todoCore.text;
      depth = todoCore.depth;
      lifeline = todoCore.lifeline;
      deadline = todoCore.deadline;
      hardness = todoCore.hardness;
      constraint = todoCore.constraint;
      estimatedTime = todoCore.estimatedTime;
      if (todoCore instanceof Todo)
      {
        final Todo todo = (Todo)todoCore;
        parent = todo.ui.parent;
        open = todo.ui.open;
        allHierarchyOpen = todo.ui.allHierarchyOpen;
        leaf = todo.ui.leaf;
        lastChild = todo.ui.lastChild;
      }
      else
      {
        parent = null;
        open = true;
        allHierarchyOpen = true;
        leaf = true;
        lastChild = false;
      }
    }
    public Builder setId(@Nullable final String id) { this.id = id; return this; }
    public Builder setOrd(@NonNull final String ord) { this.ord = ord; return this; }
    public Builder setCompletionTime(final long completionTime) { this.completionTime = completionTime; return this; }
    public Builder setText(@NonNull final String text) { this.text = text; return this; }
    public Builder setDepth(final int depth) { this.depth = depth; return this; }
    public Builder setLifeline(final long lifeline) { this.lifeline = lifeline; return this; }
    public Builder setDeadline(final long deadline) { this.deadline = deadline; return this; }
    public Builder setHardness(final int hardness) { this.hardness = hardness; return this; }
    public Builder setConstraint(final int constraint) { this.constraint = constraint; return this; }
    public Builder setEstimatedTime(final int estimatedTime) { this.estimatedTime = estimatedTime; return this; }
    public Builder setParent(@Nullable final Todo parent) { this.parent = parent; return this; }
    public Builder setOpen(final boolean open) { this.open = open; return this; }
    public Builder setAllHierarchyOpen(final boolean allHierarchyOpen) { this.allHierarchyOpen = allHierarchyOpen; return this; }
    public Builder setLeaf(final boolean leaf) { this.leaf = leaf; return this; }
    public Builder setLastChild(final boolean lastChild) { this.lastChild = lastChild; return this; }

    public Todo build()
    {
      final TodoUI ui = new TodoUI(parent, open, allHierarchyOpen, lastChild);
      ui.leaf = leaf;
      return new Todo(id, ord, creationTime, completionTime, text, depth, lifeline, deadline, hardness, constraint, estimatedTime, ui);
    }
  }
}
