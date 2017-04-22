package com.j.jface.org.todo;

import android.support.annotation.NonNull;

import java.util.ArrayList;

public interface ListChangeObserver
{
  void notifyItemChanged(final int position, @NonNull final Todo payload);
  void notifyItemInserted(final int position, @NonNull final Todo payload);
  void notifyItemMoved(final int from, final int to, @NonNull final Todo payload);
  void notifyItemRangeInserted(final int position, final ArrayList<Todo> payload);
  void notifyItemRangeRemoved(final int position, final ArrayList<Todo> payload);
}
