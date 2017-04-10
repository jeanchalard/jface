package com.j.jface.org;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.j.jface.R;
import com.j.jface.org.editor.TodoEditor.TodoDetails;
import com.j.jface.org.sound.EditTextSoundRouter;
import com.j.jface.org.sound.SelReportEditText;
import com.j.jface.org.todo.Todo;
import com.j.jface.org.todo.TodoList;
import com.j.jface.org.todo.TodoUIParams;

import static com.j.jface.R.layout.todo;

// Adapter for Todo Recycler view.
public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.ViewHolder>
{
  public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, TextWatcher, TodoList.ChangeObserver, View.OnLongClickListener
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
    @NonNull final private TodoList mList;
    @NonNull final private TodoDetails mDetails;
    @NonNull final private LinearLayout mTodoActionButtons;
    @NonNull final private ImageButton mAddSubTodoButton, mClearTodoButton, mShowActionsButton;
    public ViewHolder(@NonNull final View itemView,
                      @NonNull final JOrg jorg,
                      @NonNull final EditTextSoundRouter router,
                      @NonNull final RecyclerView recyclerView,
                      @NonNull final TodoList list)
    {
      super(itemView);
      itemView.setElevation(60);
      mJorg = jorg;
      mList = list;
      mList.addObserver(this);
      mRecyclerView = recyclerView;
      mExpander = (ExpanderView)itemView.findViewById(R.id.expander);
      mExpander.setOnClickListener(this);
      mExpander.setOnLongClickListener(this);
      mEditText = (SelReportEditText)itemView.findViewById(R.id.todoText);
      mEditText.setOnFocusChangeListener(router);
      mEditText.mListener = router;
      mEditText.addTextChangedListener(this);
      mExpansion = itemView.findViewById(R.id.todoExpanded);
      mCurrentTodo = NULL_TODO;
      mTodoActionButtons = (LinearLayout)itemView.findViewById(R.id.todoActionButtons);
      mAddSubTodoButton = (ImageButton)itemView.findViewById(R.id.todoAddButton);
      mAddSubTodoButton.setOnClickListener(this);
      mAddSubTodoButton.setVisibility(View.GONE);
      mClearTodoButton = (ImageButton)itemView.findViewById(R.id.todoClearButton);
      mClearTodoButton.setOnClickListener(this);
      mClearTodoButton.setVisibility(View.GONE);
      mShowActionsButton = (ImageButton)itemView.findViewById(R.id.todoShowActionsButton);
      mShowActionsButton.setOnClickListener(this);
      mDetails = new TodoDetails(mJorg.getContext(), mCurrentTodo, (ViewGroup)mExpansion);
    }

    @Nullable public Todo parent()
    {
      return mList.getMetadata(mCurrentTodo).parent;
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
        mList.toggleOpen(mCurrentTodo);
    }

    @Override public boolean onLongClick(@NonNull final View v)
    {
      mJorg.startDrag(this);
      return true;
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

    @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
    @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
    @Override public void afterTextChanged(@NonNull final Editable editable)
    {
      if (mCurrentTodo == NULL_TODO) return;
      if (mCurrentTodo.text.equals(editable.toString())) return;
      mCurrentTodo = mJorg.updateTodoContents(mCurrentTodo, editable);
    }

    public void bind(@NonNull final Todo todo, @NonNull final TodoUIParams metadata)
    {
      if (todo.id.equals(mCurrentTodo.id)) return;
      mCurrentTodo = todo;
      mDetails.bind(todo);
      mExpander.setDepth(todo.depth);
      setupExpander(metadata);
      mShowActionsButton.setRotation(0f);
      mExpansion.setVisibility(View.GONE);
      mClearTodoButton.setVisibility(View.GONE);
      mAddSubTodoButton.setVisibility(View.GONE);
      mEditText.setText(todo.text);
    }

    public void requestFocus()
    {
      mEditText.requestFocus();
      final InputMethodManager imm = (InputMethodManager)mEditText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.showSoftInput(mEditText, 0);
    }

    @Override
    public void notifyItemChanged(final int position, @NonNull final Todo payload)
    {
      if (!payload.id.equals(mCurrentTodo.id)) return;
      mCurrentTodo = payload;
      setupExpander(mList.getMetadata(payload));
    }

    private void setupExpander(@NonNull final TodoUIParams metadata)
    {
      final int pos = getAdapterPosition();
      final int expansions = metadata.leaf ? ExpanderView.EXPANSIONS_NONE : (metadata.open ? ExpanderView.EXPANSIONS_OPEN : ExpanderView.EXPANSIONS_CLOSED);
      final int connectionUp = 0 == pos ? 0 : ExpanderView.CONNECTIONS_UP;
      final int connectionDown = !metadata.lastChild ? ExpanderView.CONNECTIONS_DOWN : 0;
      mExpander.setConnections(connectionUp | connectionDown);
      mExpander.setExpansions(expansions);
    }

    public void prepareViewForDrag()
    {
      mExpander.setVisibility(View.INVISIBLE);
      mTodoActionButtons.setVisibility(View.INVISIBLE);
    }

    public void cleanupViewAterDrag()
    {
      mExpander.setVisibility(View.VISIBLE);
      mTodoActionButtons.setVisibility(View.VISIBLE);
    }

    public void moveTodo(final int newPos)
    {
      final int oldPos = getAdapterPosition();
      final int prevTodoPos, nextTodoPos;
      if (oldPos < newPos)
      {
        prevTodoPos = newPos;
        nextTodoPos = newPos + 1 >= mList.size() ? -1 : newPos + 1;
      }
      else
      {
        prevTodoPos = newPos - 1;
        nextTodoPos = newPos;
      }
      final Todo prevTodo = prevTodoPos < 0 ? null : mList.get(prevTodoPos);
      final Todo nextTodo = nextTodoPos < 0 ? null : mList.get(nextTodoPos);
      final Todo parent = parent();
      final String parentOrd = null == parent ? "" : parent.ord;
      final String prevTodoOrd = null == prevTodo || prevTodo.depth != mCurrentTodo.depth ? parentOrd + Todo.SEP_ORD + Todo.MIN_ORD : prevTodo.ord;
      final String nextTodoOrd = null == nextTodo || nextTodo.depth != mCurrentTodo.depth ? parentOrd + Todo.SEP_ORD + Todo.MAX_ORD : nextTodo.ord;
      final String newOrd = Todo.ordBetween(prevTodoOrd, nextTodoOrd);
      final Todo newTodo = new Todo.Builder(mCurrentTodo).setOrd(newOrd).build();
      mList.updateTodo(newTodo);
    }

    @Override public void notifyItemInserted(final int position, @NonNull final Todo payload) {}
    @Override public void notifyItemRangeInserted(final int from, final int count) {}
    @Override public void notifyItemRangeRemoved(final int from, final int count) {}
  }

  private class TodoChangeObserver implements TodoList.ChangeObserver
  {
    @Override public void notifyItemChanged(final int position, @NonNull final Todo payload)
    {
      //TodoAdapter.this.notifyItemChanged(position);
    }

    @Override public void notifyItemInserted(final int position, @NonNull final Todo payload)
    {
      TodoAdapter.this.notifyItemInserted(position);
    }

    @Override public void notifyItemRangeInserted(final int from, final int count)
    {
      TodoAdapter.this.notifyItemRangeInserted(from, count);
    }

    @Override public void notifyItemRangeRemoved(final int from, final int count)
    {
      TodoAdapter.this.notifyItemRangeRemoved(from, count);
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
    return new ViewHolder(mInflater.inflate(todo, parent, false), mJorg, mRouter, mRecyclerView, mTodoList);
  }

  @Override public void onBindViewHolder(@NonNull final ViewHolder holder, final int position)
  {
    final Todo todo = mTodoList.get(position);
    final TodoUIParams metadata = mTodoList.getMetadata(todo);
    holder.bind(todo, metadata);
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
