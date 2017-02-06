package com.j.jface.org.todo;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.j.jface.R;
import com.j.jface.org.EditTextSoundRouter;
import com.j.jface.org.JOrg;
import com.j.jface.org.SelReportEditText;

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
    @NonNull final private static Todo NULL_TODO = new Todo("", Todo.MIN_ID);
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
    {/*
        if (mShowActionsButton.getRotation() < 45f)
        {
          TransitionManager.beginDelayedTransition(mRecyclerView, expandCollapseTransition);
          if (mExpansion.getVisibility() == View.VISIBLE)
            mExpansion.setVisibility(View.GONE);
          else
            mExpansion.setVisibility(View.VISIBLE);
        }
        else*/
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
    }

    private boolean mBinding;
    @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
    @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
    @Override public void afterTextChanged(@NonNull final Editable editable)
    {
      if (mCurrentTodo == NULL_TODO) return;
      mCurrentTodo = mJorg.updateTodoContents(mCurrentTodo, editable);
    }

    public void bind(@NonNull final Todo todo)
    {
      if (todo.mId.equals(mCurrentTodo.mId)) return;
      mExpanderLayoutParams.leftMargin = todo.mDepth * 40 + 10;
      mExpander.setLayoutParams(mExpanderLayoutParams);
      mExpansion.setVisibility(View.GONE);
      mEditText.setText(todo.mText);
      mCurrentTodo = todo;
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

  @Override public int getItemCount()
  {
    return mTodoList.size();
  }
}
