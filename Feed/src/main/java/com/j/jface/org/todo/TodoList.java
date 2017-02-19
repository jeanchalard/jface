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

// A todo list backed by a sorted ArrayList. It guarantees ordering according to the ID,
// but provides indexation.
public class TodoList implements Handler.Callback
{
  public interface ChangeObserver
  {
    void notifyItemChanged(final int position, @NonNull final Todo payload);
    void notifyItemInserted(final int position, @NonNull final Todo payload);
    void notifyItemsRemoved(int position, @NonNull final List<Todo> payload);
  }

  @NonNull private final ArrayList<Todo> mList;
  @NonNull private final ArrayList<ChangeObserver> mObservers;
  @NonNull private final TodoSource mSource;
  @NonNull private final Handler mHandler;
  private TodoList(@NonNull final Context context)
  {
    mSource = new TodoSource(context);
    mList = mSource.fetchTodoList();
    mObservers = new ArrayList<>();
    mHandler = new Handler(this);
    mTodosToPersist = new HashMap<>();
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
    updateTodo(result);
    return result;
  }

  public void updateTodo(@NonNull final Todo todo)
  {
    mSource.updateTodo(todo);
    final int index = Collections.binarySearch(mList, todo);
    if (index >= 0)
    {
      if (todo.completionTime > 0)
      {
        // Completed todo. Remove.
        final int lastChildIndex = getLastChildIndex(index);
        final List<Todo> subListToClear = mList.subList(index, lastChildIndex + 1); // inclusive, exclusive
        final ArrayList<Todo> removed = new ArrayList<>(subListToClear);
        subListToClear.clear();
        for (int i = 0; i < removed.size(); ++i)
          removed.set(i, new Todo.Builder(removed.get(i)).setCompletionTime(todo.completionTime).build());
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

  @NonNull public Todo get(final int index) { return mList.get(index); }
  @Nullable public Todo get(@NonNull final String todoId)
  {
    for (final Todo t : mList) if (t.id.equals(todoId)) return t;
    return null;
  }
  public int size() { return mList.size(); }


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
