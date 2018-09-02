package com.j.jface.org

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.support.v7.widget.helper.ItemTouchHelper
import com.j.jface.clamp
import com.j.jface.org.todo.TodoListFoldableView

/**
 * An implementation of the item touch helper to move Todos in the list.
 */
internal class TodoMover(private val mAdapter : TodoAdapter, private val mList : TodoListFoldableView) : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
{
  override fun onMove(recyclerView : RecyclerView, viewHolder : ViewHolder, target : ViewHolder) = true
  override fun canDropOver(recyclerView : RecyclerView, current : ViewHolder, target : ViewHolder) = true
  override fun isItemViewSwipeEnabled() = false

  override fun onMoved(recyclerView : RecyclerView, viewHolder : ViewHolder, fromPos : Int, target : ViewHolder, toPos : Int, x : Int, y : Int)
  {
    super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)
    mList.moveTemporarily(fromPos, toPos)
    mAdapter.notifyItemMoved(fromPos, toPos)
  }

  override fun interpolateOutOfBoundsScroll(recyclerView : RecyclerView, viewSize : Int, viewSizeOutOfBounds : Int, totalSize : Int, msSinceStartScroll : Long) : Int
  {
    val i = super.interpolateOutOfBoundsScroll(recyclerView, viewSize, viewSizeOutOfBounds, totalSize, msSinceStartScroll)
    return clamp(-10, i, 10)
  }

  override fun clearView(recyclerView : RecyclerView, viewHolder : ViewHolder)
  {
    super.clearView(recyclerView, viewHolder)
    mList.stopDragging((viewHolder as TodoViewHolder).todo())
    viewHolder.cleanupViewAfterDrag()
  }

  override fun onSelectedChanged(viewHolder : ViewHolder?, actionState : Int)
  {
    super.onSelectedChanged(viewHolder, actionState)
    if (null == viewHolder) return
    val holder = viewHolder as TodoViewHolder
    val todo = holder.todo()
    mList.startDragging(todo)
    holder.prepareViewForDrag()
    holder.adapterPosition
  }

  override fun onSwiped(viewHolder : ViewHolder, direction : Int) {}
}
