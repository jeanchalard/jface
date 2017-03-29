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
  @NonNull private final HashMap<String, TodoUIParams> mUIParams;
  @NonNull private final ArrayList<ChangeObserver> mObservers;
  @NonNull private final TodoSource mSource;
  @NonNull private final Handler mHandler;
  private TodoList(@NonNull final Context context)
  {
    mSource = new TodoSource(context);
    mList = mSource.fetchTodoList();
    mUIParams = computeUIParams(mList, mSource);
    mShownIndices = computeShown(mList, mUIParams);
    mObservers = new ArrayList<>();
    mHandler = new Handler(this);
    mTodosToPersist = new HashMap<>();
  }

  private static ArrayList<Integer> computeShown(@NonNull final ArrayList<Todo> list, @NonNull final HashMap<String, TodoUIParams> uiParams)
  {
    return computeShown(new ArrayList<Integer>(list.size()), list, uiParams);
  }

  private static ArrayList<Integer> computeShown(@NonNull final ArrayList<Integer> result, @NonNull final ArrayList<Todo> list, @NonNull final HashMap<String, TodoUIParams> uiParams)
  {
    result.clear();
    int i = 0;
    TodoUIParams todoUiParams = null; int lastDepth = 2;
    for (final Todo t : list)
    {
      if (null != todoUiParams) todoUiParams.leaf = t.depth <= lastDepth;
      lastDepth = t.depth;
      todoUiParams = uiParams.get(t.id);
      if (todoUiParams.allHierarchyOpen) result.add(i);
      ++i;
    }
    if (null != todoUiParams) todoUiParams.leaf = true;
    return result;
  }

  private static HashMap<String, TodoUIParams> computeUIParams(@NonNull final ArrayList<Todo> list, @NonNull final TodoSource source)
  {
    final HashMap<String, TodoUIParams> results = new HashMap<>();
    final Stack<Todo> parents = new Stack<>();
    for (final Todo todo : list)
    {
      final boolean open = source.isOpen(todo);
      while (!parents.isEmpty() && parents.peek().depth >= todo.depth) parents.pop();
      if (parents.isEmpty())
        results.put(todo.id, new TodoUIParams(null, open, true));
      else
      {
        final Todo parent = parents.peek();
        final TodoUIParams uiParams = results.get(parent.id);
        uiParams.leaf = false;
        results.put(todo.id, new TodoUIParams(parent, open, uiParams.allHierarchyOpen && uiParams.open));
      }
      parents.push(todo);
    }
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
    final Todo result = new Todo.Builder(text, ordForNewChild(parent)).setDepth(null == parent ? 0 : parent.depth + 1).build();
    final boolean allHierarchyOpen;
    if (null != parent)
    {
      final TodoUIParams parentUIParams = mUIParams.get(parent.id);
      parentUIParams.leaf = false;
      allHierarchyOpen = parentUIParams.allHierarchyOpen && parentUIParams.open;
    }
    else allHierarchyOpen = true;
    final TodoUIParams uiParams = new TodoUIParams(parent, true, allHierarchyOpen);
    mUIParams.put(result.id, uiParams);
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
        final int lastChildIndex = getLastChildIndex(index);
        final List<Todo> subListToClear = mList.subList(index, lastChildIndex + 1); // inclusive, exclusive
        final ArrayList<Todo> removed = new ArrayList<>(subListToClear);
        subListToClear.clear();
        final Todo parent = mUIParams.get(todo.id).parent;
        if (null != parent)
          if (hasDescendants(parent))
            mUIParams.get(parent.id).leaf = true;
        for (int i = 0; i < removed.size(); ++i)
          removed.set(i, new Todo.Builder(removed.get(i)).setCompletionTime(todo.completionTime).build());
        final int before = mShownIndices.size();
        computeShown(mShownIndices, mList, mUIParams);
        final int from = Collections.binarySearch(mShownIndices, index);
        final int iTo = Collections.binarySearch(mShownIndices, index + removed.size());
        final int to = iTo < 0 ? -iTo - 1 : iTo;
        // Make it easier to identify bugs happening here
        if (mShownIndices.size() - before != to - from) throw new RuntimeException("Badsize.");
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
      mList.add(insertionPoint, todo);
      final Todo parent = mUIParams.get(todo.id).parent;
      if (null != parent)
      {
        final TodoUIParams parentUIParams = mUIParams.get(parent.id);
        if (!parentUIParams.open) toggleOpen(parent);
      }
      computeShown(mShownIndices, mList, mUIParams);
      final int inserted = -Collections.binarySearch(mShownIndices, insertionPoint) - 1;
      for (final ChangeObserver obs : mObservers) obs.notifyItemInserted(inserted, todo);
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

  public void toggleOpen(@NonNull final Todo todo)
  {
    final TodoUIParams uiParams = mUIParams.get(todo.id);
    uiParams.open = !uiParams.open;
    mSource.updateTodoOpen(todo, uiParams.open);
    final int index = mList.indexOf(todo);
    final ArrayList<Todo> descendants = getDescendants(todo);
    for (final Todo t : descendants) openOrCloseDescendants(t, uiParams.open);
    computeShown(mShownIndices, mList, mUIParams);
    final int iFrom = Collections.binarySearch(mShownIndices, index + 1);
    final int iTo = Collections.binarySearch(mShownIndices, index + descendants.size());
    final int from = iFrom > 0 ? iFrom : -iFrom - 1;
    final int to = iTo > 0 ? iTo : -iTo - 1;
    if (uiParams.open)
      for (final ChangeObserver obs : mObservers) obs.notifyItemRangeInserted(from, descendants.size());
    else
      for (final ChangeObserver obs : mObservers) obs.notifyItemRangeRemoved(from, descendants.size());
  }

  private void openOrCloseDescendants(@NonNull final Todo todo, final boolean open)
  {
    final TodoUIParams uiParams = mUIParams.get(todo.id);
    uiParams.allHierarchyOpen = open;
    if (uiParams.open)
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

  @NonNull public TodoUIParams getMetadata(@NonNull final Todo todo)
  {
    return mUIParams.get(todo.id);
  }

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
