package com.j.jface.org.todo;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import static java.util.Collections.binarySearch;

// A todo list backed by a sorted ArrayList. It guarantees ordering according to the ID,
// but provides indexation.
public class TodoList implements Handler.Callback
{
  public interface ChangeObserver
  {
    void notifyItemChanged(final int position, @NonNull final Todo payload);
    void notifyItemInserted(final int position, @NonNull final Todo payload);
    void notifyItemRangeInserted(final int position, final int count);
    void notifyItemRangeRemoved(final int position, final int count);
  }

  @NonNull private final ArrayList<Todo> mList;
  @NonNull private final ArrayList<Integer> mShownIndices;
  @NonNull private final ArrayList<ChangeObserver> mObservers;
  @NonNull private final TodoSource mSource;
  @NonNull private final Handler mHandler;
  private TodoList(@NonNull final Context context)
  {
    mSource = new TodoSource(context);
    mList = decorateForUI(mSource.fetchTodoList(), mSource);
    mShownIndices = computeShown(mList);
    mObservers = new ArrayList<>();
    mHandler = new Handler(this);
    mTodosToPersist = new HashMap<>();
  }

  private static ArrayList<Integer> computeShown(@NonNull final ArrayList<Todo> list)
  {
    return computeShown(new ArrayList<Integer>(list.size()), list);
  }

  private static ArrayList<Integer> computeShown(@NonNull final ArrayList<Integer> result, @NonNull final ArrayList<Todo> list)
  {
    result.clear();
    int i = 0;
    Todo.TodoUI todoUiParams = null; int lastDepth = 2;
    for (final Todo t : list)
    {
      if (null != todoUiParams) todoUiParams.leaf = t.depth <= lastDepth;
      lastDepth = t.depth;
      todoUiParams = t.ui;
      if (todoUiParams.allHierarchyOpen) result.add(i);
      ++i;
    }
    if (null != todoUiParams) todoUiParams.leaf = true;
    return result;
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
      if (!parents.isEmpty()) {
        final Todo parent = parents.peek();
        parent.ui.leaf = false;
        builder.setParent(parent);
        builder.setAllHierarchyOpen(parent.ui.allHierarchyOpen && parent.ui.open);
      }
      else builder.setAllHierarchyOpen(true);
      final Todo todo = builder.build();
      results.add(todo);
      parents.push(todo);
    }
    parents.pop().ui.lastChild = true;
    return results;
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
  @NonNull public Todo createAndInsertTodo(@NonNull final String text, @Nullable final Todo parent)
  {
    final boolean allHierarchyOpen;
    if (null != parent)
    {
      parent.ui.leaf = false;
      allHierarchyOpen = parent.ui.allHierarchyOpen && parent.ui.open;
    }
    else allHierarchyOpen = true;
    final Todo result = new Todo.Builder(text, ordForNewChild(parent))
     .setDepth(null == parent ? 0 : parent.depth + 1)
     .setParent(parent)
     .setOpen(true).setAllHierarchyOpen(allHierarchyOpen).setLastChild(true)
     .build();
    updateTodo(result);
    return result;
  }

  public void updateTodo(@NonNull final Todo todo)
  {
    if ("!".equals(todo.ord)) throw new RuntimeException("Trying to update a null Todo");
    mSource.updateTodo(todo);
    final int index = Collections.binarySearch(mList, todo.ord);
    if (index >= 0)
    {
      if (todo.completionTime > 0)
      {
        // Completed todo. Remove.
        if (todo.ui.lastChild && index > 0)
        {
          final Todo prevItem = mList.get(index - 1);
          if (prevItem.depth == todo.depth)
          {
            prevItem.ui.lastChild = true;
            for (final ChangeObserver obs : mObservers) obs.notifyItemChanged(index - 1, prevItem);
          }
        }
        final int lastChildIndex = getLastChildIndex(index);
        final List<Todo> subListToClear = mList.subList(index, lastChildIndex + 1); // inclusive, exclusive
        final ArrayList<Todo> removed = new ArrayList<>(subListToClear);
        subListToClear.clear();
        if (null != todo.ui.parent)
          if (hasDescendants(todo.ui.parent))
            todo.ui.parent.ui.leaf = true;
        for (int i = 0; i < removed.size(); ++i)
          removed.set(i, new Todo.Builder(removed.get(i)).setCompletionTime(todo.completionTime).build());
        final int before = mShownIndices.size();
        final int from = Collections.binarySearch(mShownIndices, index);
        final int iTo = Collections.binarySearch(mShownIndices, index + removed.size());
        final int to = iTo < 0 ? -iTo - 1 : iTo;
        computeShown(mShownIndices, mList);
        // Make it easier to identify bugs happening here
        if (before - mShownIndices.size() != to - from) throw new RuntimeException("Badsize. " + before + " → " + mShownIndices.size() + " :: " + from + " → " + to);
        for (final ChangeObserver obs : mObservers) obs.notifyItemRangeRemoved(from, to - from);
      }
      else
      {
        mList.set(index, todo);
        final int ref = Collections.binarySearch(mShownIndices, index);
        for (final ChangeObserver obs : mObservers) obs.notifyItemChanged(ref, todo);
      }
    }
    else
    {
      // BinarySearch returns -(insertion point) - 1 when the item is not in the list.
      final int insertionPoint = -index - 1;
      final Todo oldTodo = getFromId(todo.id);
      if (null == oldTodo)
      {
        // This todo was actually not here, it's a new one.
        mList.add(insertionPoint, todo);
        // Not inserted yet, so we'll get -(insertion index) - 1
        if (insertionPoint > 0)
        {
          final Todo previousItem = mList.get(insertionPoint - 1);
          previousItem.ui.lastChild = false;
          for (final ChangeObserver obs : mObservers) obs.notifyItemChanged(index - 1, previousItem);
        }
        final Todo parent = todo.ui.parent;
        if (null != parent && !parent.ui.open) toggleOpen(parent);
        computeShown(mShownIndices, mList);
        final int inserted = Collections.binarySearch(mShownIndices, insertionPoint);
        for (final ChangeObserver obs : mObservers) obs.notifyItemInserted(inserted, todo);
      }
      else
      {
        // This todo was here but not with this ord ; it's a reorder.
        final int oldPos = Collections.binarySearch(mList, oldTodo.ord);
        mList.remove(oldPos);
        mList.add(insertionPoint, todo);
        computeShown(mShownIndices, mList);
        final Todo parent = oldTodo.ui.parent;
        final ArrayList<Todo> descendants = getDirectDescendants(parent);
        if (descendants.size() > 0)
        {
          for (final Todo child : descendants) child.ui.lastChild = false;
          descendants.get(descendants.size() - 1).ui.lastChild = true;
        }
      }
    }
  }

  public ArrayList<Todo> getTreeRootedAt(@NonNull final Todo todo)
  {
    final ArrayList<Todo> subTree = getDescendants(todo);
    subTree.add(0, todo);
    return subTree;
  }

  public boolean hasDescendants(@NonNull final Todo todo)
  {
    final int index = Collections.binarySearch(mList, todo.ord);
    if (mList.size() <= index + 1) return false;
    return mList.get(index + 1).depth > todo.depth;
  }

  public ArrayList<Todo> getDescendants(@NonNull final Todo todo)
  {
    final int index = Collections.binarySearch(mList, todo.ord);
    if (index < 0) return new ArrayList<>();
    final int lastChildIndex = getLastChildIndex(index);
    final List<Todo> subList = mList.subList(index + 1, lastChildIndex + 1); // inclusive, exclusive
    return new ArrayList<>(subList);
  }

  public ArrayList<Todo> getDirectDescendants(@Nullable final Todo todo)
  {
    int index;
    final int depth;
    if (null != todo)
    {
      index = Collections.binarySearch(mList, todo.ord);
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

  public void toggleOpen(@NonNull final Todo todo)
  {
    todo.ui.open = !todo.ui.open;
    mSource.updateTodoOpen(todo, todo.ui.open);
    final int index = mList.indexOf(todo);
    final ArrayList<Todo> descendants = getDescendants(todo);
    for (final Todo t : descendants) openOrCloseDescendants(t, todo.ui.open);
    computeShown(mShownIndices, mList);
    final int iFrom = Collections.binarySearch(mShownIndices, index + 1);
    final int iTo = Collections.binarySearch(mShownIndices, index + descendants.size());
    final int from = iFrom > 0 ? iFrom : -iFrom - 1;
    final int to = iTo > 0 ? iTo : -iTo - 1;
    if (todo.ui.open)
      for (final ChangeObserver obs : mObservers)
      {
        obs.notifyItemChanged(index, todo);
        obs.notifyItemRangeInserted(from, descendants.size());
      }
    else
      for (final ChangeObserver obs : mObservers)
      {
        obs.notifyItemChanged(index, todo);
        obs.notifyItemRangeRemoved(from, descendants.size());
      }
  }

  private void openOrCloseDescendants(@NonNull final Todo todo, final boolean open)
  {
    todo.ui.allHierarchyOpen = open;
    if (todo.ui.open)
      for (final Todo t : getDescendants(todo)) openOrCloseDescendants(t, open);
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

  // Pass null to get an ord for the end of the list.
  private String ordForNewChild(@Nullable Todo parent)
  {
    final int parentIndex = mList.indexOf(parent);
    if (parentIndex < 0) parent = null;
    final int lastChildIndex = getLastChildIndex(parentIndex);
    final Todo lastChild = lastChildIndex < 0 ? null : mList.get(lastChildIndex);

    final String prevOrd = null == lastChild ? Todo.MIN_ORD : (lastChild == parent ? parent.ord + Todo.SEP_ORD : lastChild.ord);
    final String nextOrd = null == parent ? Todo.MAX_ORD : parent.ord + Todo.SEP_MAX_ORD;
    return Todo.ordBetween(prevOrd, nextOrd);
  }

  @NonNull public Todo get(final int index)
  {
    final int deref = mShownIndices.get(index);
    return mList.get(deref);
  }

  @Nullable public Todo getFromOrd(@NonNull final String todoOrd)
  {
    final int index = binarySearch(mList, todoOrd);
    if (index >= 0) return mList.get(index);
    return null;
  }
  @Nullable public Todo getFromId(@NonNull final String todoId)
  {
    for (final Todo t : mList) if (t.id.equals(todoId)) return t;
    return null;
  }
  public int size() { return mShownIndices.size(); }

  ///////////////////////////////////////////////////////////////////////////
  // Handler for delayed persistence
  ///////////////////////////////////////////////////////////////////////////
  private static final int PERSIST_TODOS = 1;
  @NonNull private final HashMap<String, Todo> mTodosToPersist;
  public boolean handleMessage(@NonNull final Message msg)
  {
    switch (msg.what)
    {
      case PERSIST_TODOS:
        persistAllTodos();
        break;
    }
    return true;
  }

  @NonNull public Todo scheduleUpdateTodo(@NonNull final Todo todo)
  {
    if ("!".equals(todo.ord)) throw new RuntimeException("Trying to update a null Todo");
    synchronized(mTodosToPersist)
    {
      mTodosToPersist.put(todo.id, todo);
    }
    mHandler.removeMessages(PERSIST_TODOS);
    mHandler.sendEmptyMessageDelayed(PERSIST_TODOS, 3000); // 3 sec before persistence
    return todo;
  }

  public void persistAllTodos()
  {
    HashMap<String, Todo> todosToPersist = new HashMap<>();
    synchronized (mTodosToPersist)
    {
      todosToPersist.putAll(mTodosToPersist);
      mTodosToPersist.clear();
    }
    for (final Todo t : todosToPersist.values()) updateTodo(t);
  }

  public void onPauseApplication()
  {
    persistAllTodos();
  }

  ///////////////////////////////////////////////////////////////////////////
  // Singleton behavior
  ///////////////////////////////////////////////////////////////////////////
  @Nullable static TodoList sList;
  synchronized static public TodoList getInstance(@NonNull final Context context)
  {
    if (null == sList) sList = new TodoList(context);
    return sList;
  }
}
