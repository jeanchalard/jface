package com.j.jface.org.todo

import android.content.Context

class TodoListReadonlyFullView(c : Context) : TodoListView(c)
{
  override fun size() = mList.size()
  override fun get(i : Int) = mList[i]
  override fun getOrNull(i : Int) = mList[i]
  override fun updateTodo(todo : TodoCore) : TodoCore = throw IllegalArgumentException("Can't update a todo through a readonly view")
  override fun updateRawTodo(todo : TodoCore) : Todo = throw IllegalArgumentException("Can't update a todo through a readonly view")
}
