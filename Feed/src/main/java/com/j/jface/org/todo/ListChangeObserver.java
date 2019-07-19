package com.j.jface.org.todo;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public interface ListChangeObserver
{
  void onItemChanged(final int position, @NonNull final Todo payload);
  void onItemInserted(final int position, @NonNull final Todo payload);
  void onItemMoved(final int from, final int to, @NonNull final Todo payload);
  void onItemRangeInserted(final int position, final ArrayList<Todo> payload);
  void onItemRangeRemoved(final int position, final ArrayList<Todo> payload);
}
