package com.j.jface.org.todo;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

import static java.util.Collections.binarySearch;

/**
 * A todo list backed by a sorted ArrayList. It guarantees ordering according to the ID,
 * but provides indexation.
 */
// Package private ; need a strong business use to use this instead of a view.
class TodoList implements Iterable<Todo>, TodoUpdaterProxy, TodoSource.ListChangeListener
{
  @NonNull private final ArrayList<Todo> mList;
  @NonNull private final TodoSource mSource;
  private TodoList(@NonNull final Context context)
  {
    mSource = new TodoSource(context);
    mSource.addListChangeListener(this);
    mList = decorateForUI(mSource.fetchTodoList(), mSource);
    mObservers = new ArrayList<>();
  }

  /******************
   * Query methods.
   ******************/
  public int size() { return mList.size(); }

  @NonNull public Todo get(final int index)
  {
    return mList.get(index);
  }

  // This simply calls binarySearch and follows the same semantics for insertion points.
  public int rindex(final TodoCore todo)
  {
    return Collections.binarySearch(mList, todo.ord);
  }

  public boolean hasDescendants(@NonNull final TodoCore todo)
  {
    final int index = rindex(todo);
    if (mList.size() <= index + 1) return false;
    return mList.get(index + 1).depth > todo.depth;
  }

  public ArrayList<Todo> getTreeRootedAt(@NonNull final TodoCore todo)
  {
    return getDescendantsInternal(todo, true);
  }

  public ArrayList<Todo> getDescendants(@NonNull final TodoCore todo)
  {
    return getDescendantsInternal(todo, false);
  }

  private ArrayList<Todo> getDescendantsInternal(@NonNull final TodoCore todo, final boolean includeRoot)
  {
    final int index = rindex(todo);
    if (index < 0) return new ArrayList<>();
    final int lastChildIndex = getLastChildIndex(index);
    final List<Todo> subList = mList.subList(index + (includeRoot ? 0 : 1), lastChildIndex + 1); // inclusive, exclusive
    return new ArrayList<>(subList);
  }

  public ArrayList<Todo> getDirectDescendants(@Nullable final TodoCore todo)
  {
    int index;
    final int depth;
    if (null != todo)
    {
      index = rindex(todo);
      depth = todo.depth + 1;
    }
    else
    {
      index = 0;
      depth = 0;
    }
    final ArrayList<Todo> descendants = new ArrayList<>();
    while (index < mList.size())
    {
      final Todo prospectiveChild = mList.get(index);
      if (prospectiveChild.depth < depth) return descendants;
      if (prospectiveChild.depth == depth) descendants.add(prospectiveChild);
      ++index;
    }
    return descendants;
  }

  private static ArrayList<Todo> decorateForUI(@NonNull final ArrayList<TodoCore> list, @NonNull final TodoSource source)
  {
    final ArrayList<Todo> results = new ArrayList<>(list.size());
    final Stack<Todo> parents = new Stack<>();
    for (final TodoCore todoCore : list)
    {
      final Todo.Builder builder = new Todo.Builder(todoCore);
      builder.setOpen(source.isOpen(todoCore));
      if (!parents.isEmpty() && parents.peek().depth > todoCore.depth) parents.pop().ui.lastChild = true;
      while (!parents.isEmpty() && parents.peek().depth >= todoCore.depth) parents.pop();
      if (!parents.isEmpty())
      {
        final Todo parent = parents.peek();
        parent.ui.leaf = false;
        builder.setParent(parent);
      }
      final Todo todo = builder.build();
      results.add(todo);
      parents.push(todo);
    }
    if (!parents.isEmpty()) parents.pop().ui.lastChild = true;
    return results;
  }

  @NonNull public static Todo decorate(@NonNull final TodoCore t)
  {
    if (t instanceof Todo) return (Todo)t;
    return new Todo.Builder(t).build();
  }

  // -1 if no such element
  private int getLastChildIndex(final int parentIndex)
  {
    if (parentIndex < 0)
      return mList.isEmpty() ? -1 : mList.size() - 1;
    final int parentDepth = mList.get(parentIndex).depth;
    final int size = mList.size();
    for (int i = parentIndex + 1; i < size; ++i)
      if (mList.get(i).depth <= parentDepth) return i - 1;
    return size - 1;
  }

  @Nullable public Todo findByOrd(@NonNull final String todoOrd)
  {
    final int index = binarySearch(mList, todoOrd);
    if (index >= 0) return mList.get(index);
    return null;
  }

  @Nullable public Todo findById(@NonNull final String todoId)
  {
    for (final Todo t : mList) if (t.id.equals(todoId)) return t;
    return null;
  }


  /*********************
   * Mutation methods.
   *********************/
  // Pass null into parent for a top-level todo.
  @NonNull public Todo createAndInsertTodo(@NonNull final String text, @Nullable final Todo parent)
  {
    final Todo result = new Todo.Builder(text, ordForNewChild(parent))
     .setParent(parent)
     .setOpen(true)
     .setLastChild(true)
     .build();
    updateRawTodo(result);
    return result;
  }

  public TodoCore updateRawTodo(@NonNull final TodoCore todo)
  {
    if ("!".equals(todo.ord)) throw new RuntimeException("Trying to update a null Todo");
    mSource.updateTodo(todo);
    return updateLocalTodo(todo);
  }

  // Implementation of TodoSource.ListChangeListener
  @Override public void onTodoRemoved(@NonNull final TodoCore todo)
  {
    final int index = rindex(todo);
    if (index >= 0) internalUpdateRemoveTodo(todo, index);
    else Log.e("REMOVED", "Firebase says Todo " + todo + " was removed, but it was not found locally");
  }
  @Override public void onTodoUpdated(@NonNull final TodoCore todo)
  {
    updateLocalTodo(todo);
  }

  private TodoCore updateLocalTodo(@NonNull final TodoCore todo)
  {
    final int index = rindex(todo);
    if (index >= 0)
    {
      if (todo.completionTime > 0)
        internalUpdateCompleteTodo(todo, index);
      else
        internalUpdateTodoContents(todo, index);
    }
    else
    {
      // BinarySearch returns -(insertion point) - 1 when the item is not in the list.
      final Todo oldTodo = findById(todo.id);
      if (null == oldTodo)
        internalUpdateAddTodo(todo, -index - 1);
      else
        return internalUpdateReorderTodo(oldTodo, todo);
    }
    return todo;
  }

  public void updateTodoOpen(@NonNull final Todo todo)
  {
    mSource.updateTodoOpen(todo);
  }

  private void internalUpdateTodoContents(@NonNull final TodoCore todo, final int index)
  {
    final Todo nTodo = decorate(todo);
    mList.set(index, nTodo);
    for (final ListChangeObserver obs : mObservers) obs.onItemChanged(index, nTodo);
  }

  private void internalUpdateRemoveTodo(@NonNull final TodoCore todo, final int index)
  {
    internalUpdateCompleteOrRemoveTodo(todo, index, false);
  }

  private void internalUpdateCompleteTodo(@NonNull final TodoCore todo, final int index)
  {
    internalUpdateCompleteOrRemoveTodo(todo, index, true);
  }

  private void internalUpdateCompleteOrRemoveTodo(@NonNull final TodoCore todo, final int index, final boolean complete)
  {
    // Remove a Todo and its descendants because they have been marked as completed locally or because the remote DB says they disappeared for any reason.
    final int lastChildIndex = getLastChildIndex(index);
    final List<Todo> subListToClear = mList.subList(index, lastChildIndex + 1); // inclusive, exclusive
    final ArrayList<Todo> removed = new ArrayList<>(subListToClear);
    subListToClear.clear();
    if (complete)
    {
      // If this is a todo marked complete locally, mark all descendants as complete. Otherwise, it's come from
      // the DB and it should simply be removed from display.
      for (int i = 0; i < removed.size(); ++i)
      {
        final Todo completedTodo = new Todo.Builder(removed.get(i)).setCompletionTime(todo.completionTime).build();
        mSource.updateTodo(completedTodo);
        removed.set(i, completedTodo);
      }
    }
    for (final ListChangeObserver obs : mObservers) obs.onItemRangeRemoved(index, removed);
  }

  private void internalUpdateAddTodo(@NonNull final TodoCore todo, final int insertionPoint)
  {
    // This todo was actually not here, it's a new one.
    final Todo nTodo = decorate(todo);
    mList.add(insertionPoint, nTodo);
    for (final ListChangeObserver obs : mObservers) obs.onItemInserted(insertionPoint, nTodo);
  }

  private Todo internalUpdateReorderTodo(@NonNull final TodoCore oldTodo, @NonNull final TodoCore newTodo)
  {
    // This todo was here but not with this ord : it's a reorder.
    final int oldPos = rindex(oldTodo);
    final Todo todo = decorate(newTodo);
    if (hasDescendants(oldTodo))
    {
      final int oldTodoOrdLength = oldTodo.ord.length();
      final ArrayList<Todo> oldTree = getDescendants(oldTodo);
      final ArrayList<Todo> newTree = new ArrayList<>(oldTree.size());
      for (int i = 0; i < oldTree.size(); ++i)
      {
        final Todo oldChild = oldTree.get(i);
        final Todo.Builder newChildBuilder = new Todo.Builder(oldChild);
        newChildBuilder.setOrd(todo.ord + oldChild.ord.substring(oldTodoOrdLength))
         .setDepth(oldChild.depth + newTodo.depth - oldTodo.depth)
         .setParent(todo);
        final Todo newChild = newChildBuilder.build();
        newTree.add(newChild);
        mSource.updateTodo(newChild);
      }
      final int lastRemovedIndex = oldPos + oldTree.size() + 1;
      mList.subList(oldPos + 1, lastRemovedIndex).clear(); // incl, excl
      for (final ListChangeObserver obs : mObservers) obs.onItemRangeRemoved(oldPos + 1, oldTree);
      mList.remove(oldPos);
      final int insertionPoint = -rindex(todo) - 1;
      mList.add(insertionPoint, todo);
      for (final ListChangeObserver obs : mObservers) obs.onItemMoved(oldPos, insertionPoint, todo);
      mList.addAll(insertionPoint + 1, newTree);
      for (final ListChangeObserver obs : mObservers) obs.onItemRangeInserted(insertionPoint + 1, newTree);
    }
    else
    {
      mList.remove(oldPos);
      final int insertionPoint = -rindex(todo) - 1;
      mList.add(insertionPoint, todo);
      for (final ListChangeObserver obs : mObservers) obs.onItemMoved(oldPos, insertionPoint, todo);
    }
    return todo;
  }

  // Pass null to get an ord for the end of the list.
  private String ordForNewChild(@Nullable TodoCore parent)
  {
    final int parentIndex = mList.indexOf(parent);
    if (parentIndex < 0) parent = null;
    final int lastChildIndex = getLastChildIndex(parentIndex);
    final Todo lastChild = lastChildIndex < 0 ? null : mList.get(lastChildIndex);

    final String prevOrd = null == lastChild ? Todo.MIN_ORD : (lastChild == parent ? parent.ord + Todo.SEP_ORD : lastChild.ord);
    final String nextOrd = null == parent ? Todo.MAX_ORD : parent.ord + Todo.SEP_MAX_ORD;
    return Todo.ordBetween(prevOrd, nextOrd);
  }

  @NonNull public TodoCore updateTodo(@NonNull final TodoCore todo)
  {
    if ("!".equals(todo.ord)) throw new RuntimeException("Trying to update a null Todo");
    updateRawTodo(todo);
    return todo;
  }

  /*********************
   * Singleton behavior.
   *********************/
  @Nullable static private TodoList sList;
  synchronized static public TodoList getInstance(@NonNull final Context context)
  {
    if (null == sList) sList = new TodoList(context);
    return sList;
  }

  /*************
   * Iteration.
   *************/
  private class TodoIterator implements Iterator<Todo>
  {
    private int index = 0;
    @Override public boolean hasNext()
    {
      return index < mList.size();
    }

    @Override public Todo next()
    {
      return mList.get(index++);
    }
  }

  @Override @NonNull public Iterator<Todo> iterator()
  {
    return new TodoIterator();
  }


  /***************
   * Observation.
   ***************/
  @NonNull private final ArrayList<ListChangeObserver> mObservers;
  public void addObserver(@NonNull final ListChangeObserver obs)
  {
    mObservers.add(obs);
  }
  public void removeObserver(@NonNull final ListChangeObserver obs)
  {
    mObservers.remove(obs);
  }


  /***************
   * Debug tools.
   ***************/
  void assertListIsConsistentWithDB()
  {
    assert(Objects.equals(mList, mSource.fetchTodoList()));
  }

  void unload() // Please do not use this out of tests.
  {
    sList = null;
  }
}
