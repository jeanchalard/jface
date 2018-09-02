package com.j.jface.org

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.j.jface.R.layout.todo
import com.j.jface.org.sound.EditTextSoundRouter
import com.j.jface.org.todo.ListChangeObserver
import com.j.jface.org.todo.Todo
import com.j.jface.org.todo.TodoListFoldableView
import java.util.*

// Adapter for Todo Recycler view.
class TodoAdapter(private val mJorg : JOrg,
                  context : Context,
                  private val mRouter : EditTextSoundRouter,
                  private val mTodoList : TodoListFoldableView,
                  private val mRecyclerView : RecyclerView) : RecyclerView.Adapter<TodoViewHolder>()
{
  private val mInflater : LayoutInflater = LayoutInflater.from(context)
  private var mExpectFocus : Todo? = null

  init
  {
    mTodoList.addObserver(TodoListChangeObserver())
  }

  /*******************
   * Adapter methods.
   */
  override fun getItemCount() = mTodoList.size()

  override fun onCreateViewHolder(parent : ViewGroup, viewType : Int) = TodoViewHolder(mInflater.inflate(todo, parent, false), mJorg, mRouter, mRecyclerView, mTodoList)
  override fun onBindViewHolder(holder : TodoViewHolder, position : Int) = holder.bind(mTodoList.get(position))

  /******************
   * Focus handling.
   */
  fun passFocusTo(todo : Todo)
  {
    mExpectFocus = todo
  }

  override fun onViewAttachedToWindow(holder : TodoViewHolder?)
  {
    if (null == holder) return
    if (holder.todo() === mExpectFocus) holder.requestFocus()
  }

  /****************************************
   * Forwarding UI-worthy events to super.
   */
  private inner class TodoListChangeObserver : ListChangeObserver
  {
    override fun notifyItemChanged(position : Int, payload : Todo) = mJorg.runOnUiThread { this@TodoAdapter.notifyItemChanged(position, payload) }
    override fun notifyItemInserted(position : Int, payload : Todo) = mJorg.runOnUiThread { this@TodoAdapter.notifyItemInserted(position) }
    override fun notifyItemMoved(from : Int, to : Int, payload : Todo) = mJorg.runOnUiThread { if (mTodoList.get(to).id != payload.id) this@TodoAdapter.notifyItemMoved(from, to) }
    override fun notifyItemRangeInserted(from : Int, payload : ArrayList<Todo>) = mJorg.runOnUiThread { this@TodoAdapter.notifyItemRangeInserted(from, payload.size) }
    override fun notifyItemRangeRemoved(from : Int, payload : ArrayList<Todo>) = mJorg.runOnUiThread { this@TodoAdapter.notifyItemRangeRemoved(from, payload.size) }
  }
}
