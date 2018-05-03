package com.j.jface.org;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.j.jface.R;
import com.j.jface.org.editor.TodoEditor;
import com.j.jface.org.sound.EditTextSoundRouter;
import com.j.jface.org.sound.SelReportEditText;
import com.j.jface.org.todo.ListChangeObserver;
import com.j.jface.org.todo.Todo;
import com.j.jface.org.todo.TodoListView;

import java.util.ArrayList;

public class TodoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, TextWatcher, ListChangeObserver
{
  @NonNull final static TransitionSet expandCollapseTransition;

  static
  {
    expandCollapseTransition = new TransitionSet();
    expandCollapseTransition.addTransition(new ChangeBounds());
    expandCollapseTransition.addTransition(new Fade());
    expandCollapseTransition.setOrdering(TransitionSet.ORDERING_TOGETHER);
  }

  @NonNull final private static Todo NULL_TODO = new Todo("", Todo.MIN_ORD);
  @NonNull private Todo mCurrentTodo;

  @NonNull final private JOrg mJorg;
  @NonNull final private RecyclerView mRecyclerView;

  @NonNull final private ExpanderView mExpander;
  @NonNull final private SelReportEditText mEditText;
  @NonNull final private View mExpansion;
  @NonNull final private TodoListView mList;
  @NonNull final private TodoEditor.TodoDetails mDetails;
  @NonNull final private LinearLayout mTodoActionButtons;
  @NonNull final private ImageButton mAddSubTodoButton, mClearTodoButton, mShowActionsButton;

  public TodoViewHolder(@NonNull final View itemView,
                        @NonNull final JOrg jorg,
                        @NonNull final EditTextSoundRouter router,
                        @NonNull final RecyclerView recyclerView,
                        @NonNull final TodoListView list)
  {
    super(itemView);
    itemView.setElevation(60);
    mJorg = jorg;
    mList = list;
    mList.addObserver(this);
    mRecyclerView = recyclerView;
    mExpander = itemView.findViewById(R.id.expander);
    mExpander.setOnClickListener(this);
    mEditText = itemView.findViewById(R.id.todoText);
    mEditText.setOnFocusChangeListener(router);
    mEditText.mListener = router;
    mEditText.addTextChangedListener(this);
    mExpansion = itemView.findViewById(R.id.todoExpanded);
    mCurrentTodo = NULL_TODO;
    mTodoActionButtons = itemView.findViewById(R.id.todoActionButtons);
    mAddSubTodoButton = itemView.findViewById(R.id.todoAddButton);
    mAddSubTodoButton.setOnClickListener(this);
    mAddSubTodoButton.setVisibility(View.GONE);
    mClearTodoButton = itemView.findViewById(R.id.todoClearButton);
    mClearTodoButton.setOnClickListener(this);
    mClearTodoButton.setVisibility(View.GONE);
    mShowActionsButton = itemView.findViewById(R.id.todoShowActionsButton);
    mShowActionsButton.setOnClickListener(this);
    mDetails = new TodoEditor.TodoDetails(mList, mCurrentTodo, (ViewGroup)mExpansion);
  }

  @NonNull public Todo todo()
  {
    return mCurrentTodo;
  }

  @Override public void onClick(@NonNull final View view)
  {
    if (view == mAddSubTodoButton)
      mJorg.addNewSubTodo(mCurrentTodo);
    else if (view == mClearTodoButton)
    {
      toggleShowActions();
      mJorg.clearTodo(mCurrentTodo);
    }
    else if (view == mShowActionsButton)
    {
      toggleShowActions();
      mRecyclerView.scrollToPosition(getAdapterPosition());
    }
    else if (view == mExpander)
    {
      mList.toggleOpen(mCurrentTodo);
      setupExpander(mCurrentTodo);
    }
  }

  private void toggleShowActions()
  {
//      mJorg.startTodoEditor(mCurrentTodo);
    /* Pour expand la ligne vers le bas et révéler les détails */
    TransitionManager.beginDelayedTransition(mRecyclerView, expandCollapseTransition);
    if (mExpansion.getVisibility() == View.VISIBLE)
    {
      mExpansion.setVisibility(View.GONE);
      mClearTodoButton.setVisibility(View.GONE);
      mAddSubTodoButton.setVisibility(View.GONE);
      ObjectAnimator.ofFloat(mShowActionsButton, "rotation", 0f).start();
    }
    else
    {
      mExpansion.setVisibility(View.VISIBLE);
      mClearTodoButton.setVisibility(View.VISIBLE);
      mAddSubTodoButton.setVisibility(View.VISIBLE);
      ObjectAnimator.ofFloat(mShowActionsButton, "rotation", 180f).start();
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

  @Override public void beforeTextChanged(final CharSequence charSequence, final int i, final int i1, final int i2) {}
  @Override public void onTextChanged(final CharSequence charSequence, final int i, final int i1, final int i2) {}
  @Override public void afterTextChanged(@NonNull final Editable editable)
  {
    if (mCurrentTodo == NULL_TODO) return;
    if (mCurrentTodo.text.equals(editable.toString())) return;
    mCurrentTodo = mJorg.updateTodoContents(mCurrentTodo, editable);
  }

  public void bind(@NonNull final Todo todo)
  {
    if (todo.equals(mCurrentTodo)) return;
    mCurrentTodo = todo;
    mDetails.bind(todo);
    mExpander.setDepth(todo.depth);
    setupExpander(todo);
    mShowActionsButton.setRotation(0f);
    mExpansion.setVisibility(View.GONE);
    mClearTodoButton.setVisibility(View.GONE);
    mAddSubTodoButton.setVisibility(View.GONE);
    if (!todo.text.equals(mEditText.getText().toString()))
      mEditText.setText(todo.text);
  }

  public void requestFocus()
  {
    mEditText.requestFocus();
    final InputMethodManager imm = (InputMethodManager)mEditText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    if (null != imm) imm.showSoftInput(mEditText, 0);
  }

  private void setupExpander(@NonNull final Todo todo)
  {
    final int pos = getAdapterPosition();
    final int expansions = todo.ui.leaf ? ExpanderView.EXPANSIONS_NONE : (todo.ui.open ? ExpanderView.EXPANSIONS_OPEN : ExpanderView.EXPANSIONS_CLOSED);
    final int connectionUp = 0 == pos ? 0 : ExpanderView.CONNECTIONS_UP;
    final int connectionDown = !todo.ui.lastChild ? ExpanderView.CONNECTIONS_DOWN : 0;
    mExpander.setConnections(connectionUp | connectionDown);
    mExpander.setExpansions(expansions);
  }

  public void prepareViewForDrag()
  {
    mExpander.setVisibility(View.INVISIBLE);
    mTodoActionButtons.setVisibility(View.INVISIBLE);
  }

  public void cleanupViewAfterDrag()
  {
    mExpander.setVisibility(View.VISIBLE);
    mTodoActionButtons.setVisibility(View.VISIBLE);
  }

  @Override public void notifyItemChanged(final int position, @NonNull final Todo payload)
  {
    if (!payload.id.equals(mCurrentTodo.id)) return;
    TransitionManager.beginDelayedTransition((ViewGroup)itemView);
    bind(payload); // This also sets mCurrentTodo.
  }

  @Override public void notifyItemMoved(int from, int to, @NonNull Todo payload)
  {
    mJorg.runOnUiThread(() -> notifyItemChanged(to, payload));
  }

  @Override public void notifyItemInserted(final int position, @NonNull final Todo payload) {}
  @Override public void notifyItemRangeInserted(final int from, @NonNull final ArrayList<Todo> payload) {}
  @Override public void notifyItemRangeRemoved(final int from, @NonNull final ArrayList<Todo> payload) {}
}
