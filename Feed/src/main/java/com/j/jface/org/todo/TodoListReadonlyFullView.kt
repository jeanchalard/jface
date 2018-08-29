package com.j.jface.org.todo

import android.content.Context

class TodoListReadonlyFullView(c : Context) : TodoListView(c)
{
  override fun size() = mList.size()
  override fun get(i : Int) = mList[i]
  override fun getOrNull(i : Int) = mList[i]
}
