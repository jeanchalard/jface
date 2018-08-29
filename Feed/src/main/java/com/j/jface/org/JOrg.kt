package com.j.jface.org

import android.app.RemoteInput
import android.content.Intent
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.Editable
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.gms.auth.api.Auth
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.feed.views.SnackbarRegistry
import com.j.jface.lifecycle.ActivityWrapper
import com.j.jface.lifecycle.AppCompatActivityWrapper
import com.j.jface.lifecycle.AuthTrampoline
import com.j.jface.lifecycle.WrappedActivity
import com.j.jface.org.editor.TodoEditor
import com.j.jface.org.sound.EditTextSoundRouter
import com.j.jface.org.sound.SelReportEditText
import com.j.jface.org.sound.SoundSource
import com.j.jface.org.todo.Todo
import com.j.jface.org.todo.TodoListFoldableView
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Main activity class for JOrg.
 */
class AuthTrampolineJOrgBoot : ActivityWrapper<AuthTrampolineJOrg>()
class AuthTrampolineJOrg(args : WrappedActivity.Args) : AuthTrampoline(args) { override val trampolineDestination get() = JOrgBoot::class.java }
class JOrgBoot : AppCompatActivityWrapper<JOrg>()
class JOrg(args : WrappedActivity.Args) : WrappedActivity(args)
{
  private val mSoundSource : SoundSource
  private val mSoundRouter : EditTextSoundRouter
  private val mTopLayout : CoordinatorLayout
  private val mRecyclerView : RecyclerView
  private var mTodoList : TodoListFoldableView? = null
  private var mTouchHelper : ItemTouchHelper? = null

  init
  {
    mA.setContentView(R.layout.org_top)
    (mA as AppCompatActivity).setSupportActionBar(mA.findViewById(R.id.orgTopActionBar))
    mSoundSource = SoundSource(mA, mA.findViewById(R.id.sound_source))
    mSoundRouter = EditTextSoundRouter(mSoundSource)
    mTopLayout = mA.findViewById(R.id.topLayout)
    mRecyclerView = mA.findViewById(R.id.todoList)

    val loadingSpinner = mA.findViewById<ProgressBar>(R.id.orgTop_loading)
    mRecyclerView.addItemDecoration(DividerItemDecoration(mA, (mRecyclerView.layoutManager as LinearLayoutManager).orientation))
    val fab = mA.findViewById<FloatingActionButton>(R.id.addTodo)
    fab.setOnClickListener { _ -> addNewSubTodo(null) }

    val editedTodoId : String? = mA.intent.getStringExtra(Const.EXTRA_TODO_ID)
    val editedTodoSubitems = RemoteInput.getResultsFromIntent(mA.intent)?.getString(Const.EXTRA_TODO_SUBITEMS)?.split(",")
    if (null == editedTodoId) mA.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

    val executor = Executors.newSingleThreadExecutor()
    executor.submit(Callable {
      executor.shutdown()
      val tlv = TodoListFoldableView(mA.getApplicationContext())
      val adapter = TodoAdapter(this, mA, mSoundRouter, tlv, mRecyclerView)
      val touchHelper = ItemTouchHelper(TodoMover(adapter, tlv))
      val editedTodoIndexInView = ensureOpenAndReturnIndex(editedTodoId, tlv)
      mA.runOnUiThread {
        touchHelper.attachToRecyclerView(mRecyclerView)
        mRecyclerView.adapter = adapter
        loadingSpinner.visibility = GONE
        mTopLayout.visibility = VISIBLE
        if (editedTodoIndexInView >= 0)
        {
          mRecyclerView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener
          {
            override fun onGlobalLayout()
            {
              mRecyclerView.scrollToPosition(editedTodoIndexInView)
              val textView = mRecyclerView.getChildAt(editedTodoIndexInView).findViewById<SelReportEditText>(R.id.todoText)
              textView.requestFocus()
              textView.setSelection(textView.length())
              val holder = mRecyclerView.findViewHolderForAdapterPosition(editedTodoIndexInView) as TodoViewHolder
              holder.showActions(true)
              mRecyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
          })
        }
      }

      mTouchHelper = touchHelper
      mTodoList = tlv
    })

    scheduleBackup()
  }

  private fun ensureOpenAndReturnIndex(id : String?, tlv : TodoListFoldableView) : Int
  {
    if (null == id) return -1
    val todo = tlv.findById(id) ?: return -1
    tlv.ensureVisible(todo)
    return tlv.findIndexById(id)
  }

  override fun onPause()
  {
    mSoundSource.onPause()
    SnackbarRegistry.unsetSnackbarParent(mTopLayout)
  }

  override fun onResume()
  {
    mSoundSource.onResume()
    val c = GregorianCalendar()
    val title = String.format(Locale.JAPAN, "%02d-%02d %s",
     c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), Const.WEEKDAYS[c.get(Calendar.DAY_OF_WEEK) - 1])
    (mA.findViewById<View>(R.id.title_today) as TextView).text = title
    SnackbarRegistry.setSnackbarParent(mTopLayout)
  }

  // TODO : all these guys should be somewhere else presumably. It's scary to just assume
  // mTodoList is not null and to just skip doing the stuff otherwise, even if in the
  // practice it's probably fine because you can't actually do something to a Todo before
  // it is displayed anyway which is supposed to mean the list was loaded for sure
  // Null parent means top level, as always
  fun addNewSubTodo(parent : Todo?) : Todo?
  {
    val todo = mTodoList?.createAndInsertTodo("", parent) ?: return null
    val adapter = mRecyclerView.adapter as TodoAdapter? ?: return todo
    adapter.passFocusTo(todo)
    return todo
  }

  fun clearTodo(todo : Todo)
  {
    val todoList = mTodoList ?: return
    val oldTree = todoList.markTodoCompleteAndReturnOldTree(todo)
    val undoChance = Snackbar.make(mTopLayout, "Marked done.", Snackbar.LENGTH_LONG)
    undoChance.duration = 8000 // 8 seconds, because LENGTH_LONG is punily short
    undoChance.setAction("Undo") { _ -> for (t in oldTree) todoList.updateRawTodo(t) }
    undoChance.show()
  }

  fun updateTodoContents(todo : Todo, editable : Editable) : Todo
  {
    val text = editable.toString()
    val newTodo = Todo.Builder(todo).setText(text).build()
    mTodoList?.updateTodo(newTodo)
    return newTodo
  }

  fun startTodoEditor(todo : Todo)
  {
    val editorIntent = Intent(mA, TodoEditor.activityClass())
    editorIntent.putExtra(Const.EXTRA_TODO_ID, todo.id)
    mA.startActivity(editorIntent)
  }

  override fun onActivityResult(requestCode : Int, resultCode : Int, data : Intent)
  {
    super.onActivityResult(requestCode, resultCode, data)
    val x = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
    Log.e("Google sign in result", "" + resultCode + " : " + x.status)
  }

  fun runOnUiThread(r : Runnable)
  {
    mA.runOnUiThread(r)
  }

  private fun scheduleBackup()
  {
    // TODO : implement this
  }

  companion object
  {
    fun activityClass() = AuthTrampolineJOrgBoot::class.java
  }
}
