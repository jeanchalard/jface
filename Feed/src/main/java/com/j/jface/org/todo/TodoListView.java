package com.j.jface.org.todo;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

/**
 * A view of a TodoList. Accesses to the TodoList should generally go
 * through a view.
 */
public class TodoListView implements ListChangeObserver
{
  private static final boolean DEBUG_VIEW = true;

  @NonNull private final TodoList mList;
  @NonNull private ArrayList<Integer> mView;

  public TodoListView(@NonNull final TodoList sourceList)
  {
    mList = sourceList;
    mList.addObserver(this);
    mObservers = new ArrayList<>();
    mView = refreshView(mList);
    if (DEBUG_VIEW) dumpView("start");
  }

  public void updateTodo(@NonNull final Todo todo)
  {
    mList.updateRawTodo(todo);
  }

  public int size()
  {
    return mView.size();
  }

  public Todo get(final int index)
  {
    final int deref = mView.get(index);
    return mList.get(deref);
  }

  private ArrayList<Integer> refreshView(@NonNull final TodoList list)
  {
    final ArrayList<Integer> result = new ArrayList<>();
    int i = 0;
    Todo prevTodo = null;
    for (final Todo t : list)
    {
      t.ui.leaf = true;
      t.ui.lastChild = true;
      if (null != t.ui.parent) t.ui.parent.ui.leaf = false;
      if (null != prevTodo && prevTodo.depth <= t.depth) prevTodo.ui.lastChild = false;
      prevTodo = t;
      if (isAllHierarchyOpen(t)) result.add(i);
      ++i;
    }
    mView = result;
    return mView;
  }

  private boolean isAllHierarchyOpen(@NonNull Todo todo)
  {
    while (null != todo.ui.parent)
    {
      todo = todo.ui.parent;
      if (!todo.ui.open) return false;
    }
    return true;
  }

  private void shiftView(final int fromPos, final int by)
  {
    final int ins = mView.get(fromPos);
    for (int i = ins > 0 ? ins : - ins - 1; i < mView.size(); ++i)
      mView.set(i, mView.get(i) + by);
  }

  @Override public void notifyItemChanged(final int position, @NonNull final Todo payload)
  {
    final int posInView = Collections.binarySearch(mView, position);
    if (posInView > 0)
      for (final ListChangeObserver obs : mObservers) obs.notifyItemChanged(position, payload);
    if (DEBUG_VIEW) dumpView("itemChanged");
  }

  @Override public void notifyItemInserted(final int position, @NonNull final Todo payload)
  {
    Todo parent = payload.ui.parent;
    while (null != parent)
    {
      if (!parent.ui.open) toggleOpen(parent);
      parent.ui.leaf = false;
      parent = parent.ui.parent;
    }
    final int index = Collections.binarySearch(mView, position);
    final int insertionPoint = index > 0 ? index : -index - 1;
    shiftView(position, 1);
    mView.add(insertionPoint, position);
    for (final ListChangeObserver obs : mObservers) obs.notifyItemInserted(insertionPoint, payload);
    if (insertionPoint > 0)
    {
      final Todo prevTodo = get(insertionPoint - 1);
      if (prevTodo.depth == payload.depth)
      {
        prevTodo.ui.lastChild = false;
        for (final ListChangeObserver obs : mObservers) obs.notifyItemChanged(insertionPoint - 1, prevTodo);
      }
    }
    if (DEBUG_VIEW) dumpView("itemInserted");
  }

  @Override public void notifyItemMoved(final int from, final int to, @NonNull final Todo payload)
  {
    final boolean removed, added;
    final int localFrom = mView.get(from);
    if (localFrom > 0) // If it's not in the view don't do it
    {
      shiftView(from, -1);
      mView.remove(from);
      removed = true;
    } else removed = false;
    final int localTo = mView.get(to);
    if (localTo > 0)
    {
      shiftView(to, 1);
      mView.add(localTo, to);
      added = true;
    } else added = false;
    if (removed && added)
      for (final ListChangeObserver obs : mObservers) obs.notifyItemMoved(localFrom, localTo, payload);
    else if (added)
      for (final ListChangeObserver obs : mObservers) obs.notifyItemInserted(localTo, payload);
    else if (removed)
    {
      final ArrayList<Todo> l = new ArrayList<>(1);
      l.add(payload);
      for (final ListChangeObserver obs : mObservers) obs.notifyItemRangeRemoved(localFrom, l);
    }
  }

  @Override public void notifyItemRangeInserted(final int position, final ArrayList<Todo> payload)
  {
    notifyItemRangeInsertedInternal(position, payload, false);
  }

  private void notifyItemRangeInsertedInternal(final int position, final ArrayList<Todo> payload, final boolean shiftExisting)
  {
    toggleOpenAllClosedParents(payload);
    final ArrayList<Todo> insertedItems = new ArrayList<>(payload.size());
    final ArrayList<Integer> insertedIndices = new ArrayList<>(payload.size());
    int curpos = position;
    for (final Todo t : payload)
    {
      if (null != t.ui.parent) t.ui.parent.ui.leaf = false;
      if (isAllHierarchyOpen(t))
      {
        if (insertedItems.size() > 0)
        {
          final Todo prevInsertedItem = insertedItems.get(insertedItems.size() - 1);
          prevInsertedItem.ui.lastChild = prevInsertedItem.depth > t.depth;
        }
        insertedItems.add(t);
        insertedIndices.add(curpos);
        ++curpos;
      }
    }
    if (insertedItems.isEmpty()) return;
    final int index = Collections.binarySearch(mView, position);
    final int insertionPoint = index > 0 ? index : -index - 1;
    if (insertionPoint < mView.size())
    {
      final Todo lastInserted = insertedItems.get(insertedItems.size() - 1);
      lastInserted.ui.lastChild = lastInserted.depth >= get(insertionPoint).depth;
    }
    if (shiftExisting) shiftView(position, insertedItems.size());
    mView.addAll(insertionPoint, insertedIndices);
    for (final ListChangeObserver obs : mObservers) obs.notifyItemRangeInserted(insertionPoint, insertedItems);
    if (insertionPoint > 0)
    {
      final Todo prevTodo = get(insertionPoint - 1);
      if (prevTodo.depth == insertedItems.get(0).depth)
      {
        prevTodo.ui.lastChild = false;
        for (final ListChangeObserver obs : mObservers) obs.notifyItemChanged(insertionPoint - 1, prevTodo);
      }
    }
    if (DEBUG_VIEW) dumpView("rangeInserted");
  }

  private static class RemovedRange
  {
    public final int start;
    @NonNull public final ArrayList<Todo> todos;
    public RemovedRange(final int start, @NonNull final Todo todo)
    {
      this.start = start;
      todos = new ArrayList<>();
      todos.add(todo);
    }
    public void add(@NonNull final Todo t) { todos.add(t); }
    public int end() { return start + todos.size(); }
  }
  @Override public void notifyItemRangeRemoved(int position, @NonNull final ArrayList<Todo> payload)
  {
    final ArrayList<RemovedRange> ranges = new ArrayList<>();
    RemovedRange currentRange = null;
    for (final Todo t : payload)
    {
      final int ref = Collections.binarySearch(mView, position);
      ++position;
      if (ref < 0) continue;
      if (null == currentRange || ref != currentRange.end())
      {
        currentRange = new RemovedRange(ref, t);
        ranges.add(currentRange);
      }
      else currentRange.add(t);
    }
    for (final RemovedRange range : ranges)
    {
      mView.subList(range.start, range.end()).clear(); // Incl, excl
      shiftView(mView.get(range.start), range.start - range.end());
      final Todo before = range.start - 1 < 0 ? null : get(range.start - 1);
      if (before != null)
      {
        before.ui.leaf = !mList.hasDescendants(before);
        final Todo after = range.start >= mView.size() ? null : get(range.start);
        before.ui.lastChild = null == after || after.depth < before.depth;
      }
      for (final ListChangeObserver obs : mObservers) obs.notifyItemRangeRemoved(range.start, range.todos);
    }
    if (DEBUG_VIEW) dumpView("rangeRemoved");
  }

  // Returns whether any change was made.
  private boolean toggleOpenAllClosedParents(final ArrayList<Todo> todos)
  {
    HashSet<Todo> closedParents = null; // Most of the time there are none, so don't alloc for nothing.
    for (final Todo insertedTodo : todos)
    {
      Todo parent = insertedTodo.ui.parent;
      while (null != parent)
      {
        if (!parent.ui.open)
        {
          if (null == closedParents) closedParents = new HashSet<>();
          closedParents.add(parent);
        }
        parent = parent.ui.parent;
      }
    }
    if (null == closedParents) return false;
    final ArrayList<Todo> closedParentsList = new ArrayList<>(closedParents);
    Collections.sort(closedParentsList, new Comparator<Todo>() {
      @Override public int compare(@NonNull final Todo o1, @NonNull final Todo o2)
      {
        return Integer.compare(o1.depth, o2.depth);
      }
    });
    for (final Todo t : closedParents) toggleOpen(t);
    return true;
  }

  public void toggleOpen(@NonNull final Todo todo)
  {
    todo.ui.open = !todo.ui.open;
    Log.e(todo.ui.open ? "OPEN" : "CLOSE", todo.text);
    mList.updateTodoOpen(todo);
    final ArrayList<Todo> descendants = mList.getDescendants(todo);
    final int index = mList.rindex(todo);
    if (!todo.ui.open)
      notifyItemRangeRemoved(index + 1, descendants);
    else
    {
      for (final Todo t : descendants) t.ui.open = true;
      notifyItemRangeInserted(index + 1, descendants);
    }
  }

  public void moveTodo(final int localFrom, final int localTo)
  {
    if (localFrom == localTo) return;
    final int from = mView.get(localFrom);
    final int to = mView.get(localTo);

    final int prevTodoPos, nextTodoPos;
    if (from < to)
    {
      prevTodoPos = to;
      nextTodoPos = to + 1 >= mList.size() ? -1 : to + 1;
    }
    else
    {
      prevTodoPos = to - 1;  // If 0, that gives us -1 which is fine.
      nextTodoPos = to;
    }
    final Todo prevTodo = prevTodoPos < 0 ? null : mList.get(prevTodoPos);
    final Todo nextTodo = nextTodoPos < 0 ? null : mList.get(nextTodoPos);
    final Todo todo = mList.get(from);

    final Todo parent;
    if (null == prevTodo)
      parent = null;
    else if (null == nextTodo || prevTodo.depth > nextTodo.depth)
    {
      if (todo.depth >= prevTodo.depth)
        parent = prevTodo.ui.parent;
      else // todo.depth < prevTodo.depth
        if (prevTodo.descendsFrom(todo.ui.parent))
          parent = todo.ui.parent;
        else
          parent = prevTodo.ui.parent;
    }
    else if (prevTodo.depth == nextTodo.depth)
      parent = prevTodo.ui.parent;
    else // prevTodo.depth < nextTodo.depth
      parent = prevTodo;

    final int newDepth = null == parent ? 0 : parent.depth + 1;
    final String parentOrdAndSep = null == parent ? "" : parent.ord + Todo.SEP_ORD;
    final String prevTodoOrd = (null == prevTodo || prevTodo.depth != newDepth) ? parentOrdAndSep + Todo.MIN_ORD : prevTodo.ord;
    final String nextTodoOrd = (null == nextTodo || nextTodo.depth != newDepth) ? parentOrdAndSep + Todo.MAX_ORD : nextTodo.ord;
    final String newOrd = Todo.ordBetween(prevTodoOrd, nextTodoOrd);
    final Todo newTodo = new Todo.Builder(todo).setOrd(newOrd).build();
    mList.updateRawTodo(newTodo);
  }

  public void dumpView(String tag)
  {
    while (tag.length() < 20) tag += " ";
    for (int i = 0; i < mView.size(); ++i)
    {
      final int l = mView.get(i);
      final Todo t = mList.get(l);
      String s = "";
      for (int k = t.depth; k > 0; --k) s = s + "  ";
      s += t.text;
      Log.e(tag, String.format("> %02d %02d : %s", i, l, s));
    }
  }

  /***************
   * App lifecycle and todo operations.
   ***************/
  public void onPauseApplication() { mList.onPauseApplication(); }
  @NonNull public TodoCore updateRawTodo(@NonNull final TodoCore todo) { return mList.updateRawTodo(todo); }
  @NonNull public Todo scheduleUpdateTodo(@NonNull final Todo todo) { return mList.scheduleUpdateTodo(todo); }
  @NonNull public Todo createAndInsertTodo(@NonNull final String text, @Nullable final Todo parent) { return mList.createAndInsertTodo(text, parent); }

  @NonNull public ArrayList<Todo> markTodoCompleteAndReturnOldTree(@NonNull final Todo todo)
  {
    final ArrayList<Todo> descendants = mList.getTreeRootedAt(todo);
    final Todo newTodo = new Todo.Builder(todo).setCompletionTime(System.currentTimeMillis()).build();
    mList.updateRawTodo(newTodo);
    return descendants;
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
}
