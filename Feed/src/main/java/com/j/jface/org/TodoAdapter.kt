package com.j.jface.org

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
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

  override fun onViewAttachedToWindow(holder : TodoViewHolder)
  {
    if (holder.todo() === mExpectFocus) holder.requestFocus()
  }

  /****************************************
   * Forwarding UI-worthy events to super.
   */
  private inner class TodoListChangeObserver : ListChangeObserver
  {
    override fun onItemChanged(position : Int, payload : Todo) = mJorg.runOnUiThread { this@TodoAdapter.notifyItemChanged(position, payload) }
    override fun onItemInserted(position : Int, payload : Todo) = mJorg.runOnUiThread { this@TodoAdapter.notifyItemInserted(position) }
    override fun onItemMoved(from : Int, to : Int, payload : Todo) = mJorg.runOnUiThread { if (mTodoList.get(to).id != payload.id) this@TodoAdapter.notifyItemMoved(from, to) }
    override fun onItemRangeInserted(from : Int, payload : ArrayList<Todo>) = mJorg.runOnUiThread { this@TodoAdapter.notifyItemRangeInserted(from, payload.size) }
    override fun onItemRangeRemoved(from : Int, payload : ArrayList<Todo>) = mJorg.runOnUiThread { this@TodoAdapter.notifyItemRangeRemoved(from, payload.size) }
  }
}
