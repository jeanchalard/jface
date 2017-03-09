package com.j.jface.org.todo;

import android.support.annotation.Nullable;

/**
 * A class to store UI-related Todo stuff.
 * This stores data that is either not persisted but kept for performance reasons, or local to this device.
 */
public class TodoUIParams
{
  @Nullable public final Todo parent;
  public boolean open;
  public boolean allHierarchyOpen; // Whether this is visible ; an item is visible if all of its parents are open.
  public boolean leaf;

  public TodoUIParams(@Nullable final Todo parent, final boolean open, final boolean allHierarchyOpen)
  {
    this.parent = parent;
    this.open = open;
    this.allHierarchyOpen = allHierarchyOpen;
    this.leaf = true;
  }
}
