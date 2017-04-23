package com.j.jface.org;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.j.jface.org.sound.EditTextSoundRouter;
import com.j.jface.org.todo.ListChangeObserver;
import com.j.jface.org.todo.Todo;
import com.j.jface.org.todo.TodoListView;

import java.util.ArrayList;

import static com.j.jface.R.layout.todo;

// Adapter for Todo Recycler view.
public class TodoAdapter extends RecyclerView.Adapter<TodoViewHolder>
{
  @NonNull private final JOrg mJorg;
  @NonNull private final EditTextSoundRouter mRouter;
  @NonNull private final LayoutInflater mInflater;
  @NonNull private final TodoListView mTodoList;
  @NonNull private final RecyclerView mRecyclerView;
  @Nullable private Todo mExpectFocus;
  public TodoAdapter(@NonNull final JOrg jorg,
                     @NonNull final Context context,
                     @NonNull final EditTextSoundRouter router,
                     @NonNull final TodoListView todoList,
                     @NonNull final RecyclerView recyclerView)
  {
    mJorg = jorg;
    mRouter = router;
    mInflater = LayoutInflater.from(context);
    mTodoList = todoList;
    todoList.addObserver(new TodoListChangeObserver());
    mRecyclerView = recyclerView;
    mExpectFocus = null;
  }

  @Override public TodoViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType)
  {
    return new TodoViewHolder(mInflater.inflate(todo, parent, false), mJorg, mRouter, mRecyclerView, mTodoList);
  }

  @Override public void onBindViewHolder(@NonNull final TodoViewHolder holder, final int position)
  {
    holder.bind(mTodoList.get(position));
  }

  @Override public void onViewAttachedToWindow(final TodoViewHolder holder)
  {
    if (holder.todo() == mExpectFocus) holder.requestFocus();
  }

  @Override public int getItemCount()
  {
    return mTodoList.size();
  }

  public void passFocusTo(@NonNull final Todo todo)
  {
    mExpectFocus = todo;
  }


  private class TodoListChangeObserver implements ListChangeObserver
  {
    @Override public void notifyItemChanged(final int position, @NonNull final Todo payload) {}
    @Override public void notifyItemInserted(final int position, @NonNull final Todo payload)
    { TodoAdapter.this.notifyItemInserted(position); }
    @Override public void notifyItemMoved(final int from, final int to, @NonNull final Todo payload)
    { if (mTodoList.get(to) != payload) TodoAdapter.this.notifyItemMoved(from, to); }
    @Override public void notifyItemRangeInserted(final int from, @NonNull final ArrayList<Todo> payload)
    { TodoAdapter.this.notifyItemRangeInserted(from, payload.size()); }
    @Override public void notifyItemRangeRemoved(final int from, @NonNull final ArrayList<Todo> payload)
    { TodoAdapter.this.notifyItemRangeRemoved(from, payload.size()); }
  }
}
