package com.j.jface.org.todo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;

public abstract class TodoListView implements ListChangeObserver, TodoUpdaterProxy
{
  @NonNull protected final TodoList mList;

  public TodoListView(@NonNull final Context context)
  {
    this(TodoList.getInstance(context));
  }

  public TodoListView(@NonNull final TodoList sourceList)
  {
    mList = sourceList;
    mList.addObserver(this);
  }

  public abstract int size();
  @NonNull public abstract Todo get(int index);
  @Nullable public abstract Todo getOrNull(final int index);

  public int findIndex(@NonNull final Todo todo)
  {
    for (int i = size() - 1; i >= 0; --i)
      if (get(i).equals(todo)) return i;
    return -1;
  }

  @Nullable public Todo findById(@NonNull final String todoId)
  {
    return mList.findById(todoId);
  }

  public int findIndexById(@NonNull final String todoId)
  {
    for (int i = size() - 1; i >= 0; --i)
      if (get(i).id.equals(todoId)) return i;
    return -1;
  }

  @Nullable public Todo findByOrd(@NonNull final String ord)
  {
    return mList.findByOrd(ord);
  }

  public int findIndexByOrd(@NonNull final String ord) // Used for testing. Returns the index.
  {
    for (int i = size() - 1; i >= 0; --i)
      if (get(i).ord.equals(ord)) return i;
    return -1;
  }

  @NonNull public ArrayList<Todo> getAllDescendants(@NonNull final TodoCore todo) { return mList.getDescendants(todo); }

  @NonNull public ArrayList<Todo> markTodoCompleteAndReturnOldTree(@NonNull final Todo todo)
  {
    final ArrayList<Todo> descendants = mList.getTreeRootedAt(todo);
    final Todo newTodo = todo.withCompletionTime(System.currentTimeMillis());
    mList.updateRawTodo(newTodo);
    return descendants;
  }
  @NonNull public Todo unmarkTodoComplete(@NonNull final Todo todo)
  {
    final Todo newTodo = todo.withCompletionTime(0);
    mList.updateRawTodo(newTodo);
    return newTodo;
  }

  /***************
   * App lifecycle and todo operations.
   ***************/
  @NonNull public TodoCore updateRawTodo(@NonNull final TodoCore todo) { return mList.updateRawTodo(todo); }
  @NonNull public Todo updateTodo(@NonNull final Todo todo) { return mList.updateTodo(todo); }
  @NonNull public Todo createAndInsertTodo(@NonNull final String text, @Nullable final Todo parent) { return mList.createAndInsertTodo(text, parent); }

  /***************
   * Default empty implementations so that subclasses don't have to provide what they don't care about
   ***************/
  @Override public void notifyItemInserted(final int position, @NonNull final Todo payload) {}
  @Override public void notifyItemChanged(int position, @NonNull final Todo payload) {}
  @Override public void notifyItemMoved(int from, int to, @NonNull final Todo payload) {}
  @Override public void notifyItemRangeInserted(int position, @Nullable final ArrayList<Todo> payload) {}
  @Override public void notifyItemRangeRemoved(int position, @Nullable final ArrayList<Todo> payload) {}
}
