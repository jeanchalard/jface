package com.j.jface.org;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.j.jface.R;
import com.j.jface.org.todo.Todo;
import com.j.jface.org.todo.TodoList;

import java.util.List;

import static com.j.jface.R.layout.todo;

// Adapter for Todo Recycler view.
public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.ViewHolder>
{
  public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, TextWatcher
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
    @NonNull public Todo mCurrentTodo;

    @NonNull final private JOrg mJorg;
    @NonNull final private RecyclerView mRecyclerView;

    @NonNull final private View mView;
    @NonNull final private ImageView mExpander;
    @NonNull final private LinearLayout.LayoutParams mExpanderLayoutParams;
    @NonNull final private SelReportEditText mEditText;
    @NonNull final private View mExpansion;
    @NonNull final private LinearLayout mTodoActionButtons;
    @NonNull final private ImageButton mAddSubTodoButton, mClearTodoButton, mShowActionsButton;
    public ViewHolder(@NonNull final View itemView,
                      @NonNull final JOrg jorg,
                      @NonNull final EditTextSoundRouter router,
                      @NonNull final RecyclerView recyclerView)
    {
      super(itemView);
      mJorg = jorg;
      mRecyclerView = recyclerView;
      mView = itemView;
      mExpander = (ImageView)itemView.findViewById(R.id.expander);
      mExpanderLayoutParams = (LinearLayout.LayoutParams)mExpander.getLayoutParams();
      mEditText = (SelReportEditText)itemView.findViewById(R.id.todoText);
      mEditText.setOnFocusChangeListener(router);
      mEditText.mListener = router;
      mEditText.addTextChangedListener(this);
      mExpansion = itemView.findViewById(R.id.todoExpanded);
      mCurrentTodo = NULL_TODO;
      mTodoActionButtons = (LinearLayout)itemView.findViewById(R.id.todoActionButtons);
      mAddSubTodoButton = (ImageButton)itemView.findViewById(R.id.todoAddButton);
      mAddSubTodoButton.setOnClickListener(this);
      mClearTodoButton = (ImageButton)itemView.findViewById(R.id.todoClearButton);
      mClearTodoButton.setOnClickListener(this);
      mShowActionsButton = (ImageButton)itemView.findViewById(R.id.todoShowActionsButton);
      mShowActionsButton.setOnClickListener(this);
    }

    @Override public void onClick(@NonNull final View view)
    {
      if (view == mAddSubTodoButton)
      {
        toggleShowActions();
        mJorg.addNewSubTodo(mCurrentTodo);
      }
      else if (view == mClearTodoButton)
      {
        toggleShowActions();
        mJorg.clearTodo(mCurrentTodo);
      }
      else if (view == mShowActionsButton)
        toggleShowActions();
    }

    private void toggleShowActions()
    {
      mJorg.startTodoEditor(mCurrentTodo);
      /* Pour expand la ligne vers le bas et révéler les détails
        if (mShowActionsButton.getRotation() < 45f)
        {
          TransitionManager.beginDelayedTransition(mRecyclerView, expandCollapseTransition);
          if (mExpansion.getVisibility() == View.VISIBLE)
            mExpansion.setVisibility(View.GONE);
          else
            mExpansion.setVisibility(View.VISIBLE);
        }
        else*/

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

    private boolean mBinding;
    @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
    @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
    @Override public void afterTextChanged(@NonNull final Editable editable)
    {
      if (mCurrentTodo == NULL_TODO) return;
      if (mCurrentTodo.mText.equals(editable.toString())) return;
      mCurrentTodo = mJorg.updateTodoContents(mCurrentTodo, editable);
    }

    public void bind(@NonNull final Todo todo)
    {
      if (todo.mId.equals(mCurrentTodo.mId)) return;
      mCurrentTodo = todo;
      mExpanderLayoutParams.leftMargin = todo.mDepth * 40 + 10;
      mExpander.setLayoutParams(mExpanderLayoutParams);
      mExpansion.setVisibility(View.GONE);
      mEditText.setText(todo.mText);
    }

    public void requestFocus()
    {
      mEditText.requestFocus();
      final InputMethodManager imm = (InputMethodManager)mEditText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.showSoftInput(mEditText, 0);
    }
  }

  private class TodoChangeObserver implements TodoList.ChangeObserver
  {
    @Override public void notifyItemChanged(int position, @NonNull Todo payload)
    {
      //TodoAdapter.this.notifyItemChanged(position);
    }

    @Override public void notifyItemInserted(int position, @NonNull Todo payload)
    {
      TodoAdapter.this.notifyItemInserted(position);
    }

    @Override public void notifyItemsRemoved(int position, @NonNull List<Todo> payload)
    {
      TodoAdapter.this.notifyItemRangeRemoved(position, payload.size());
    }
  }

  @NonNull private final JOrg mJorg;
  @NonNull private final EditTextSoundRouter mRouter;
  @NonNull private final LayoutInflater mInflater;
  @NonNull private final TodoList mTodoList;
  @NonNull private final RecyclerView mRecyclerView;
  @Nullable private Todo mExpectFocus;
  public TodoAdapter(@NonNull final JOrg jorg,
                     @NonNull final Context context,
                     @NonNull final EditTextSoundRouter router,
                     @NonNull final TodoList todoList,
                     @NonNull final RecyclerView recyclerView)
  {
    mJorg = jorg;
    mRouter = router;
    mInflater = LayoutInflater.from(context);
    mTodoList = todoList;
    todoList.addObserver(new TodoChangeObserver());
    mRecyclerView = recyclerView;
    mExpectFocus = null;
  }

  @Override public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType)
  {
    return new ViewHolder(mInflater.inflate(todo, parent, false), mJorg, mRouter, mRecyclerView);
  }

  @Override public void onBindViewHolder(@NonNull final ViewHolder holder, final int position)
  {
    final Todo todo = mTodoList.get(position);
    holder.bind(todo);
  }

  @Override public void onViewAttachedToWindow(final ViewHolder holder)
  {
    if (holder.mCurrentTodo == mExpectFocus)
      holder.requestFocus();
  }

  @Override public int getItemCount()
  {
    return mTodoList.size();
  }

  public void passFocusTo(@NonNull final Todo todo)
  {
    mExpectFocus = todo;
  }
}
