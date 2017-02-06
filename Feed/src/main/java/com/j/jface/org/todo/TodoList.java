package com.j.jface.org.todo;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// A todo list backed by a sorted ArrayList. It guarantees ordering according to the ID,
// but provides indexation.
public class TodoList
{
  public interface ChangeObserver
  {
    void notifyItemChanged(final int position, @NonNull final Todo payload);
    void notifyItemInserted(final int position, @NonNull final Todo payload);
    void notifyItemsRemoved(int position, @NonNull final List<Todo> payload);
  }

  @NonNull private ArrayList<Todo> mList;
  @Nullable private ArrayList<ChangeObserver> mObservers;
  public TodoList() { this(0); }
  public TodoList(final int capacity) { this(new ArrayList<Todo>(capacity)); }
  // Careful : dangerous constructor. Assumes the todoList is already sorted like this
  // class likes it. Provided for performance when reading from the database, where
  // we know it's already ordered and we don't need to sort them again one by one
  // with each insertion.
  public TodoList(final ArrayList<Todo> todoList)
  {
    mList = todoList;
    mObservers = new ArrayList<>();
  }

  public void addObserver(@NonNull final ChangeObserver obs)
  {
    mObservers.add(obs);
  }

  public void removeObserver(@NonNull final ChangeObserver obs)
  {
    mObservers.remove(obs);
  }

  // Pass null for a top-level todo.
  @NonNull public Todo createAndInsertTodo(@NonNull final String text, @NonNull final Todo parent)
  {
    final Todo result = new Todo.Builder(text, ordForNewChild(parent)).setDepth(null == parent ? 0 : parent.mDepth + 1).build();
    updateTodo(result);
    return result;
  }

  public void updateTodo(@NonNull final Todo todo)
  {
    final int index = Collections.binarySearch(mList, todo);
    if (index >= 0)
    {
      if (todo.mCompletionTime > 0)
      {
        // Completed todo. Remove.
        final int lastChildIndex = getLastChildIndex(index);
        final List<Todo> subListToClear = mList.subList(index, lastChildIndex + 1); // inclusive, exclusive
        final ArrayList<Todo> removed = new ArrayList<>(subListToClear);
        subListToClear.clear();
        for (int i = 0; i < removed.size(); ++i)
          removed.set(i, new Todo.Builder(removed.get(i)).setCompletionTime(todo.mCompletionTime).build());
        for (final ChangeObserver obs : mObservers) obs.notifyItemsRemoved(index, removed);
      }
      else
      {
        mList.set(index, todo);
        for (final ChangeObserver obs : mObservers) obs.notifyItemChanged(index, todo);
      }
    }
    else
    {
      // BinarySearch returns -(insertion point) - 1 when the item is not in the list.
      final int insertionPoint = -index - 1;
      mList.add(insertionPoint, todo);
      for (final ChangeObserver obs : mObservers) obs.notifyItemInserted(insertionPoint, todo);
    }
  }

  public ArrayList<Todo> getDescendants(@NonNull final Todo todo)
  {
    final int index = mList.indexOf(todo);
    if (index < 0) return new ArrayList<>();
    final int lastChildIndex = getLastChildIndex(index);
    final List<Todo> subList = mList.subList(index, lastChildIndex + 1); // inclusive, exclusive
    return new ArrayList<>(subList);
  }

  // -1 if no such element
  private int getLastChildIndex(final int parentIndex)
  {
    if (parentIndex < 0)
      return mList.isEmpty() ? -1 : mList.size() - 1;
    final int parentDepth = mList.get(parentIndex).mDepth;
    final int size = mList.size();
    for (int i = parentIndex + 1; i < size; ++i)
      if (mList.get(i).mDepth <= parentDepth) return i - 1;
    return size - 1;
  }

  // Pass null to get an ord for the end of the list.
  private String ordForNewChild(@Nullable Todo parent)
  {
    final int parentIndex = mList.indexOf(parent);
    if (parentIndex < 0) parent = null;
    final int lastChildIndex = getLastChildIndex(parentIndex);
    final Todo lastChild = lastChildIndex < 0 ? null : mList.get(lastChildIndex);

    final String prevOrd = null == lastChild ? Todo.MIN_ORD : (lastChild == parent ? parent.mOrd + Todo.SEP_ORD : lastChild.mOrd);
    final String nextOrd = null == parent ? Todo.MAX_ORD : parent.mOrd + Todo.SEP_MAX_ORD;
    return Todo.ordBetween(prevOrd, nextOrd);
  }

  public Todo get(final int index) { return mList.get(index); }
  public int size() { return mList.size(); }
}
