package com.j.jface.org;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.helper.ItemTouchHelper;

import com.j.jface.org.todo.Todo;
import com.j.jface.org.todo.TodoListView;

/**
 * An implementation of the item touch helper to move Todos in the list.
 */
class TodoMover extends ItemTouchHelper.SimpleCallback
{
  @NonNull final TodoAdapter mAdapter;
  @NonNull final TodoListView mList;

  int mDestination = -1;

  public TodoMover(@NonNull final TodoAdapter adapter, @NonNull final TodoListView list)
  {
    super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
    mAdapter = adapter;
    mList = list;
  }

  @Override public boolean onMove(@NonNull final RecyclerView recyclerView, @NonNull final ViewHolder viewHolder, @NonNull final ViewHolder target)
  {
    return true;
  }


  public void onMoved(@NonNull final RecyclerView recyclerView,
                      @NonNull final ViewHolder viewHolder, final int fromPos,
                      @NonNull final ViewHolder target, final int toPos,
                      final int x, final int y)
  {
    super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y);
    mList.moveTemporarily(fromPos, toPos);
    mAdapter.notifyItemMoved(fromPos, toPos);
    mDestination = toPos;
  }


  @Override public boolean canDropOver(@NonNull final RecyclerView recyclerView, @NonNull final ViewHolder current, @NonNull final ViewHolder target)
  {
    return true;
  }


  @Override public int interpolateOutOfBoundsScroll(@NonNull final RecyclerView recyclerView, final int viewSize, final int viewSizeOutOfBounds, final int totalSize, final long msSinceStartScroll)
  {
    final int i = super.interpolateOutOfBoundsScroll(recyclerView, viewSize, viewSizeOutOfBounds, totalSize, msSinceStartScroll);
    return i > 0 ? Math.max(i, 10) : Math.min(i, -10);
  }


  @Override public void clearView(@NonNull final RecyclerView recyclerView, @NonNull final ViewHolder viewHolder)
  {
    super.clearView(recyclerView, viewHolder);
    mList.stopDragging(((TodoViewHolder)viewHolder).todo());
    ((TodoViewHolder)viewHolder).cleanupViewAfterDrag();
  }


  public void onSelectedChanged(@Nullable final ViewHolder viewHolder, final int actionState)
  {
    super.onSelectedChanged(viewHolder, actionState);
    if (null == viewHolder) return;
    final TodoViewHolder holder = (TodoViewHolder)viewHolder;
    final Todo todo = holder.todo();
    mList.startDragging(todo);
    holder.prepareViewForDrag();
    mDestination = holder.getAdapterPosition();
  }

  @Override public boolean isItemViewSwipeEnabled() { return false; }
  @Override public void onSwiped(final ViewHolder viewHolder, final int direction) {}
}
