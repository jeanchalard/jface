package com.j.jface.org

import android.animation.ObjectAnimator
import android.text.Editable
import android.text.TextWatcher
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.j.jface.R
import com.j.jface.org.editor.TodoEditor
import com.j.jface.org.sound.EditTextSoundRouter
import com.j.jface.org.sound.SelReportEditText
import com.j.jface.org.todo.ListChangeObserver
import com.j.jface.org.todo.Todo
import com.j.jface.org.todo.TodoListFoldableView
import java.util.ArrayList

class TodoViewHolder(itemView : View,
                     private val mJorg : JOrg,
                     router : EditTextSoundRouter,
                     private val mRecyclerView : RecyclerView,
                     private val mList : TodoListFoldableView) : RecyclerView.ViewHolder(itemView), View.OnClickListener, TextWatcher, ListChangeObserver
{
  companion object
  {
    private val NULL_TODO = Todo("", Todo.MIN_ORD)
    private val expandCollapseTransition = TransitionSet().apply {
      addTransition(ChangeBounds())
      addTransition(Fade())
      ordering = TransitionSet.ORDERING_TOGETHER
    }
  }

  private var mCurrentTodo : Todo

  private val mExpander : ExpanderView
  private val mEditText : SelReportEditText
  private val mExpansion : View
  private val mDetails : TodoEditor.TodoDetails
  private val mTodoActionButtons : LinearLayout
  private val mAddSubTodoButton : ImageButton
  private val mClearTodoButton : ImageButton
  private val mShowActionsButton : ImageButton

  init
  {
    itemView.elevation = 60f
    mList.addObserver(this)
    mExpander = itemView.findViewById(R.id.expander)
    mExpander.setOnClickListener(this)
    mEditText = itemView.findViewById(R.id.todoText)
    mEditText.onFocusChangeListener = router
    mEditText.mListener = router
    mEditText.addTextChangedListener(this)
    mExpansion = itemView.findViewById(R.id.todoExpanded)
    mCurrentTodo = NULL_TODO
    mTodoActionButtons = itemView.findViewById(R.id.todoActionButtons)
    mAddSubTodoButton = itemView.findViewById(R.id.todoAddButton)
    mAddSubTodoButton.setOnClickListener(this)
    mAddSubTodoButton.visibility = View.GONE
    mClearTodoButton = itemView.findViewById(R.id.todoClearButton)
    mClearTodoButton.setOnClickListener(this)
    mClearTodoButton.visibility = View.GONE
    mShowActionsButton = itemView.findViewById(R.id.todoShowActionsButton)
    mShowActionsButton.setOnClickListener(this)
    mDetails = TodoEditor.TodoDetails(mList, mCurrentTodo, mExpansion as ViewGroup)
  }

  fun todo() = mCurrentTodo

  override fun onClick(view : View)
  {
    when
    {
      view === mAddSubTodoButton -> mJorg.addNewSubTodo(mCurrentTodo)
      view === mClearTodoButton -> { toggleShowActions() ; mJorg.clearTodo(mCurrentTodo) }
      view === mShowActionsButton -> { toggleShowActions() ; mRecyclerView.scrollToPosition(adapterPosition) }
      view === mExpander -> { mList.toggleOpen(mCurrentTodo) ; setupExpander(mCurrentTodo) }
    }
  }

  private fun toggleShowActions()
  {
    showActions(mExpansion.visibility != View.VISIBLE)
  }

  fun showActions(show : Boolean)
  {
    //      mJorg.startTodoEditor(mCurrentTodo);
    /* Pour expand la ligne vers le bas et révéler les détails */
    TransitionManager.beginDelayedTransition(mRecyclerView, expandCollapseTransition)
    if (show)
    {
      mEditText.setSingleLine(false)
      mExpansion.visibility = View.VISIBLE
      mClearTodoButton.visibility = View.VISIBLE
      mAddSubTodoButton.visibility = View.VISIBLE
      ObjectAnimator.ofFloat(mShowActionsButton, "rotation", 180f).start()
    }
    else
    {
      mEditText.setSingleLine(true)
      mExpansion.visibility = View.GONE
      mClearTodoButton.visibility = View.GONE
      mAddSubTodoButton.visibility = View.GONE
      ObjectAnimator.ofFloat(mShowActionsButton, "rotation", 0f).start()
    }

    /* Pour afficher la palette de boutons
    TransitionManager.beginDelayedTransition(mTodoActionButtons, expandCollapseTransition);
    if (mAddSubTodoButton.getVisibility() == View.VISIBLE)
    {
      mAddSubTodoButton.setVisibility(View.GONE);
      mClearTodoButton.setVisibility(View.GONE);
      final LinearLayout.LayoutParams p = (LinearLayout.LayoutParams)mTodoActionButtons.getLayoutParams();
      final int btnHeight = 63;
      p.topMargin = (mView.getHeight() - btnHeight) / 2;
      p.bottomMargin = p.topMargin;
      mTodoActionButtons.setMinimumHeight(btnHeight);
      mTodoActionButtons.setLayoutParams(p);
      ObjectAnimator.ofFloat(mView, "translationZ", 0f).start();
      ObjectAnimator.ofFloat(mTodoActionButtons, "translationZ", 5f).start();
      ObjectAnimator.ofFloat(mAddSubTodoButton, "rotation", 180f).start();
      ObjectAnimator.ofFloat(mClearTodoButton, "rotation", 180f).start();
      ObjectAnimator.ofFloat(mShowActionsButton, "rotation", 90f).start();
    }
    else
    {
      mAddSubTodoButton.setVisibility(View.VISIBLE);
      mClearTodoButton.setVisibility(View.VISIBLE);
      final LinearLayout.LayoutParams p = (LinearLayout.LayoutParams)mTodoActionButtons.getLayoutParams();
      final int btnHeight = 126;
      p.topMargin = (mView.getHeight() - btnHeight) / 2;
      p.bottomMargin = p.topMargin;
      mTodoActionButtons.setMinimumHeight(btnHeight);
      mTodoActionButtons.setLayoutParams(p);
      ObjectAnimator.ofFloat(mView, "translationZ", 30f).start();
      ObjectAnimator.ofFloat(mTodoActionButtons, "translationZ", 35f).start();
      ObjectAnimator.ofFloat(mAddSubTodoButton, "rotation", 0f).start();
      ObjectAnimator.ofFloat(mClearTodoButton, "rotation", 0f).start();
      ObjectAnimator.ofFloat(mShowActionsButton, "rotation", 0f).start();
    }
    */
  }

  override fun beforeTextChanged(charSequence : CharSequence, i : Int, i1 : Int, i2 : Int) {}
  override fun onTextChanged(charSequence : CharSequence, i : Int, i1 : Int, i2 : Int) {}

  override fun afterTextChanged(editable : Editable)
  {
    if (mCurrentTodo === NULL_TODO) return
    if (mCurrentTodo.text == editable.toString()) return
    mCurrentTodo = mJorg.updateTodoContents(mCurrentTodo, editable)
  }

  fun bind(todo : Todo)
  {
    if (todo == mCurrentTodo) return
    mCurrentTodo = todo
    mDetails.bind(todo)
    mExpander.setDepth(todo.depth)
    setupExpander(todo)
    mShowActionsButton.rotation = 0f
    mExpansion.visibility = View.GONE
    mClearTodoButton.visibility = View.GONE
    mAddSubTodoButton.visibility = View.GONE
    if (todo.text != mEditText.text.toString())
      mEditText.setText(todo.text)
  }

  fun requestFocus()
  {
    mEditText.requestFocus()
    val imm = mEditText.context.getSystemService(InputMethodManager::class.java)
    imm?.showSoftInput(mEditText, 0)
  }

  private fun setupExpander(todo : Todo)
  {
    val pos = adapterPosition
    val expansions = if (todo.ui.leaf) ExpanderView.EXPANSIONS_NONE else if (todo.ui.open) ExpanderView.EXPANSIONS_OPEN else ExpanderView.EXPANSIONS_CLOSED
    val connectionUp = if (0 == pos) 0 else ExpanderView.CONNECTIONS_UP
    val connectionDown = if (!todo.ui.lastChild) ExpanderView.CONNECTIONS_DOWN else 0
    mExpander.setConnections(connectionUp or connectionDown)
    mExpander.setExpansions(expansions)
  }

  fun prepareViewForDrag()
  {
    mExpander.visibility = View.INVISIBLE
    mTodoActionButtons.visibility = View.INVISIBLE
  }

  fun cleanupViewAfterDrag()
  {
    mExpander.visibility = View.VISIBLE
    mTodoActionButtons.visibility = View.VISIBLE
  }

  override fun onItemChanged(position : Int, payload : Todo)
  {
    if (payload.id != mCurrentTodo.id) return
    TransitionManager.beginDelayedTransition(itemView as ViewGroup)
    bind(payload) // This also sets mCurrentTodo.
  }

  override fun onItemMoved(from : Int, to : Int, payload : Todo)
  {
    mJorg.runOnUiThread(Runnable { onItemChanged(to, payload) })
  }

  override fun onItemInserted(position : Int, payload : Todo) {}
  override fun onItemRangeInserted(from : Int, payload : ArrayList<Todo>) {}
  override fun onItemRangeRemoved(from : Int, payload : ArrayList<Todo>) {}
}
