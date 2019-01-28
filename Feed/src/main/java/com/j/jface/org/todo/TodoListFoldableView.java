package com.j.jface.org.todo;

import android.content.Context;
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
public class TodoListFoldableView extends TodoListView
{
  private static final boolean DEBUG_VIEW = false;

  @NonNull private ArrayList<Integer> mView;

  public TodoListFoldableView(@NonNull final Context context)
  {
    this(TodoList.getInstance(context));
  }

  public TodoListFoldableView(@NonNull final TodoList sourceList)
  {
    super(sourceList);
    mObservers = new ArrayList<>();
    mView = refreshView(mList);
    if (DEBUG_VIEW) dumpView("start " + this);
  }

  @Override public int size()
  {
    return mView.size();
  }

  @Override @NonNull public Todo get(final int index)
  {
    final int deref = mView.get(index);
    return mList.get(deref);
  }

  @Nullable public Todo getOrNull(final int index)
  {
    return index < 0 || index >= mView.size() ? null : mList.get(mView.get(index));
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
    for (int i = fromPos; i < mView.size(); ++i)
      mView.set(i, mView.get(i) + by);
  }

  @Override public void notifyItemChanged(final int position, @NonNull final Todo payload)
  {
    final int posInView = Collections.binarySearch(mView, position);
    if (posInView > 0)
      for (final ListChangeObserver obs : mObservers) obs.notifyItemChanged(position, payload);
    if (DEBUG_VIEW) dumpView("itemChanged");
  }

  @Override public void notifyItemMoved(final int from, final int to, @NonNull final Todo payload)
  {
    final boolean removed, added;
    final int localFrom = Collections.binarySearch(mView, from);
    if (localFrom >= 0) // If it's not in the view don't do it
    {
      mView.remove(localFrom);
      shiftView(localFrom, -1);
      removed = true;
    } else removed = false;
    final int prospectiveLocalTo = Collections.binarySearch(mView, to);
    final int localTo = prospectiveLocalTo < 0 ? -prospectiveLocalTo - 1 : prospectiveLocalTo;
    if (localTo >= 0)
    {
      shiftView(localTo, 1);
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
    if (DEBUG_VIEW) dumpView("itemMoved " + this);
  }

  @Override public void notifyItemInserted(final int position, @NonNull final Todo payload)
  {
    insertItem(position, payload, true);
  }
  private void insertItem(final int position, @NonNull final Todo payload, final boolean shiftExisting)
  {
    if (DEBUG_VIEW) Log.e("insertItem", "" + position + " (" + (shiftExisting ? "shift" : "view") + ") : " + payload.text);
    Todo parent = payload.ui.parent;
    while (null != parent)
    {
      if (!parent.ui.open) toggleOpen(parent);
      parent.ui.leaf = false;
      parent = parent.ui.parent;
    }
    final int index = Collections.binarySearch(mView, position);
    final int insertionPoint = index >= 0 ? index : -index - 1;
    if (shiftExisting) shiftView(insertionPoint, 1);
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

  @Override public void notifyItemRangeInserted(final int position, final ArrayList<Todo> payload)
  {
    insertRange(position, payload, true);
  }

  private void insertRange(final int position, final ArrayList<Todo> payload, final boolean shiftExisting)
  {
    if (DEBUG_VIEW) Log.e("insertRange", "" + position + " (" + (shiftExisting ? "shift" : "view") + ") : " + payload.size());
    final int index = Collections.binarySearch(mView, position);
    final int insertionPoint = index >= 0 ? index : -index - 1;
    if (shiftExisting) shiftView(insertionPoint, payload.size());
    final ArrayList<Todo> insertedItems = new ArrayList<>(payload.size());
    final ArrayList<Integer> insertedIndices = new ArrayList<>(payload.size());
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
        insertedIndices.add(mList.rindex(t));
      }
    }
    if (insertedItems.isEmpty()) return;
    if (insertionPoint < mView.size())
    {
      final Todo lastInserted = insertedItems.get(insertedItems.size() - 1);
      lastInserted.ui.lastChild = lastInserted.depth >= get(insertionPoint).depth;
    }
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
    @NonNull private final ArrayList<Todo> todos;
    private RemovedRange(final int start, @NonNull final Todo todo)
    {
      this.start = start;
      todos = new ArrayList<>();
      todos.add(todo);
    }
    public void add(@NonNull final Todo t) { todos.add(t); }
    public int end() { return start + todos.size(); }
  }
  @Override public void notifyItemRangeRemoved(final int position, @NonNull final ArrayList<Todo> payload)
  {
    removeRange(position, payload, true);
  }
  private void removeRange(final int position, @NonNull final ArrayList<Todo> payload, final boolean shiftExisting)
  {
    if (DEBUG_VIEW)
    {
      Log.e("removeRange " + this, "" + position + " (" + (shiftExisting ? "shift" : "view") + ") : " + payload.size());
      for (final Todo t : payload) Log.e("removeRange " + this, t.text);
    }
    // First : figure out what items in the view point to a element that was removed. To do this, iterate over all removed
    // items in order and find their reference in the view ; if it's not referenced, the view isn't showing it and it should
    // not be gathered, but if it is then take note. When taking note, look at whether the item that's being gathered is
    // immediately at the end of the range that's being built : if it is, add it to that range, otherwise create a new
    // range because it's not contiguous.
    final ArrayList<RemovedRange> ranges = new ArrayList<>();
    RemovedRange currentRange = null;
    int curPos = position;
    for (final Todo t : payload)
    {
      final int ref = Collections.binarySearch(mView, curPos);
      ++curPos;
      if (ref < 0) continue;
      if (null == currentRange || ref != currentRange.end())
      {
        currentRange = new RemovedRange(ref, t);
        ranges.add(currentRange);
      }
      else currentRange.add(t);
    }
    // If shifting is necessary, then compute the index of the reference where the removed range (in the list, not the view) is
    // starting. Because that range has been deleted, all the references after this should be updated because the index of their
    // referent has been reduced by payload.size(), so shift by -payload.size(). The references of the items that have been
    // removed are being shifted too from their old index (which at this time makes no sense, and points to some other item or
    // to outside the list) to another index that makes no sense. It does not matter because they will be removed after the
    // shift is complete.
    if (shiftExisting)
    {
      final int index = Collections.binarySearch(mView, position);
      final int insertionPoint = index >= 0 ? index : -index - 1;
      shiftView(insertionPoint, -payload.size());
    }
    // Actually remove the references to the items that have been removed in the list. Iterate through the ranges that need to
    // be removed from the view and remove the references the ranges refer to. For each range, recompute for the todo immediately
    // before the removed item whether it's a leaf (all its children might have just been removed) and whether it's a last child
    // (all its lower brethen may have just been removed).
    if (DEBUG_VIEW) dumpView("before removeRange " + this);
    for (final RemovedRange range : ranges)
    {
      mView.subList(range.start, range.end()).clear(); // Incl, excl
      final Todo before = range.start - 1 < 0 ? null : get(range.start - 1);
      if (before != null)
      {
        before.ui.leaf = !mList.hasDescendants(before);
        final Todo after = range.start >= mView.size() ? null : get(range.start);
        before.ui.lastChild = null == after || after.depth < before.depth;
      }
      for (final ListChangeObserver obs : mObservers) obs.notifyItemRangeRemoved(range.start, range.todos);
    }
    if (DEBUG_VIEW) dumpView("rangeRemoved " + this);
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
    closedParentsList.sort(Comparator.comparingInt(o -> o.depth));
    for (final Todo t : closedParents) toggleOpen(t);
    return true;
  }

  public void ensureVisible(@NonNull final Todo todo)
  {
    final ArrayList<Todo> l = new ArrayList<>();
    l.add(todo);
    toggleOpenAllClosedParents(l);
  }

  public void toggleOpen(@NonNull final Todo todo)
  {
    final ArrayList<Todo> descendants = mList.getDescendants(todo);
    if (descendants.isEmpty()) return;
    todo.ui.open = !todo.ui.open;
    mList.updateTodoOpen(todo);
    final int index = mList.rindex(todo);
    if (!todo.ui.open)
      removeRange(index + 1, descendants, false);
    else
    {
      final ArrayList<Todo> insertedDescendants = new ArrayList<>();
      for (final Todo t : descendants) if (isAllHierarchyOpen(t)) insertedDescendants.add(t);
      insertRange(index + 1, insertedDescendants, false);
    }
  }

  private void dumpView(@NonNull final String tag)
  {
    final StringBuilder tagg = new StringBuilder(tag);
    while (tagg.length() < 20) tagg.append(" ");
    for (int i = 0; i < mView.size(); ++i)
    {
      final int l = mView.get(i);
      final Todo t = mList.get(l);
      final StringBuilder s = new StringBuilder();
      for (int k = t.depth; k > 0; --k) s.append(" ");
      s.append(t.text);
      if (DEBUG_VIEW) Log.e(tagg.toString(), String.format("> %02d %02d : %s", i, l, s.toString()));
    }
  }

  /**************************
   * Dragging to move items.
   **************************/
  private static class DragData
  {
    @NonNull final public Todo todo;
    private final boolean openBeforeMove;
    private final int indexInListBeforeMove;
    private final int indexInViewBeforeMove;
    private int currentIndexInView;
    private DragData(@NonNull final Todo todo, final int indexInList, final int indexInView)
    {
      if (DEBUG_VIEW) Log.e("New DragData", todo.text + " = " + indexInView + ":" + indexInList);
      this.todo = todo;
      openBeforeMove = todo.ui.open;
      indexInListBeforeMove = indexInList;
      indexInViewBeforeMove = indexInView;
      currentIndexInView = indexInView;
    }
  }
  private DragData mCurrentDrag = null;
  public void startDragging(@NonNull final Todo todo)
  {
    if (DEBUG_VIEW) Log.e("Start dragging", todo.text);
    final int indexInList = mList.rindex(todo);
    mCurrentDrag = new DragData(todo, indexInList, Collections.binarySearch(mView, indexInList));
    if (todo.ui.open) toggleOpen(todo);
  }
  public void moveTemporarily(final int from, final int to)
  {
    if (DEBUG_VIEW) Log.e("Move temporarily", "" + from + ":" + mView.get(from) + " → " + to + ":" + mView.get(to));
    if (from != mCurrentDrag.currentIndexInView) throw new RuntimeException("Not in this position (" + from + ") but in " + mCurrentDrag.currentIndexInView);
    mCurrentDrag.currentIndexInView = to;
  }
  public void stopDragging(@NonNull final Todo todo)
  {
    if (DEBUG_VIEW) Log.e("Stop dragging", todo.text + " = " + mCurrentDrag.indexInViewBeforeMove + ":" + mCurrentDrag.indexInListBeforeMove + " → " + mCurrentDrag.currentIndexInView);
    if (mCurrentDrag.todo != todo) throw new RuntimeException("Not dragging this todo (" + todo.text + ") but another (" + mCurrentDrag.todo.text + ")");
    if (mCurrentDrag.indexInViewBeforeMove != mCurrentDrag.currentIndexInView)
    {
      // Compute which are the two todos between which this todo should go.
      final int replacedTodoIndex = mView.get(mCurrentDrag.currentIndexInView);
      final int prevTodoPos, nextTodoPos;
      if (mCurrentDrag.indexInViewBeforeMove < mCurrentDrag.currentIndexInView)
      {
        prevTodoPos = replacedTodoIndex;
        nextTodoPos = replacedTodoIndex + 1 >= mList.size() ? -1 : replacedTodoIndex + 1;
      }
      else
      {
        prevTodoPos = replacedTodoIndex - 1;  // If 0, that gives us -1 which is fine.
        nextTodoPos = replacedTodoIndex;
      }

      final Todo prevTodo;
      if (prevTodoPos < 0)
        prevTodo = null;
      else
      {
        final int prevOpenTodoPosInsertionIndex = Collections.binarySearch(mView, prevTodoPos);
        final int prevOpenTodoPos = prevOpenTodoPosInsertionIndex >= 0 ? prevOpenTodoPosInsertionIndex : -prevOpenTodoPosInsertionIndex - 2; // the one before
        prevTodo = this.getOrNull(prevOpenTodoPos);
      }
      final Todo nextTodo;
      if (nextTodoPos < 0)
        nextTodo = null;
      else
      {
        final int nextOpenTodoPosInsertionIndex = Collections.binarySearch(mView, nextTodoPos);
        final int nextOpenTodoPos = nextOpenTodoPosInsertionIndex >= 0 ? nextOpenTodoPosInsertionIndex : -nextOpenTodoPosInsertionIndex - 1;
        nextTodo = this.getOrNull(nextOpenTodoPos);
      }

      final Todo newTodo = TodoList.decorate(moveTodoBetween(mCurrentDrag.todo, prevTodo, nextTodo));
      if (mCurrentDrag.openBeforeMove != newTodo.ui.open && !newTodo.ui.leaf)
        toggleOpen(newTodo);
    }
    mCurrentDrag = null;
    if (DEBUG_VIEW) dumpView("moved");
  }

  private TodoCore moveTodoBetween(@NonNull final Todo todo, @Nullable final Todo prevTodo, @Nullable final Todo nextTodo)
  {
    if (DEBUG_VIEW) Log.e("Move todo between", todo.text + " → (" + (null == prevTodo ? "null" : prevTodo.text) + " ; " + (null == nextTodo ? "null" : nextTodo.text) + ")");
    final Todo parent;
    final String prevOrd, nextOrd;
    if (null == prevTodo)
    {
      // Put before the first todo : new parent is null.
      parent = null;
      prevOrd = Todo.MIN_ORD;
      nextOrd = null == nextTodo ? Todo.MAX_ORD : nextTodo.ord;
    }
    else if (null == nextTodo || prevTodo.depth > nextTodo.depth)
    {
      // Put after the last child of some parent...
      if (todo.depth >= prevTodo.depth)
      {
        // ...and old todo was deeper than or as deep as prevTodo. New parent is the parent of prevTodo (possibly null).
        parent = prevTodo.ui.parent;
        prevOrd = prevTodo.ord;
        nextOrd = null == prevTodo.ui.parent ? Todo.MAX_ORD : prevTodo.ui.parent.ord + Todo.SEP_ORD + Todo.MAX_ORD;
      }
      else // todo.depth < prevTodo.depth
      {
        // ...and old todo was not as deep as prevTodo. Put as a sibling of nextTodo.
        parent = null == nextTodo ? null : nextTodo.ui.parent;
        Todo prevSiblingOfNext = prevTodo;
        while (null != prevSiblingOfNext && parent != prevSiblingOfNext.ui.parent) prevSiblingOfNext = prevSiblingOfNext.ui.parent; // prevSiblingOfNext probably can't be null but better safe than sorry
        prevOrd = null == prevSiblingOfNext ? Todo.MIN_ORD : prevSiblingOfNext.ord;
        nextOrd = null == nextTodo ? Todo.MAX_ORD : nextTodo.ord;
      }
    }
    else if (prevTodo.depth == nextTodo.depth)
    {
      // The simplest case : prev and next have same depth, just put between them
      parent = prevTodo.ui.parent;
      prevOrd = prevTodo.ord;
      nextOrd = nextTodo.ord;
    }
    else // prevTodo.depth < nextTodo.depth
    {
      // PrevTodo is the parent of nextTodo. Parent to prevTodo and put in front.
      parent = prevTodo;
      prevOrd = prevTodo.ord + Todo.SEP_ORD + Todo.MIN_ORD;
      nextOrd = nextTodo.ord;
    }

    final String newOrd = Todo.ordBetween(prevOrd, nextOrd);
    final Todo newTodo = new Todo.Builder(todo).setOrd(newOrd).setDepth(null == parent ? 0 : parent.depth + 1).build();
    return mList.updateRawTodo(newTodo);
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
    mList.assertListIsConsistentWithDB();
  }
}
