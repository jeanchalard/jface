package com.j.jface.org;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.j.jface.R;
import com.j.jface.org.todo.TodoViewModel;
import com.j.jface.org.todo.Todo;

import java.util.ArrayList;
import java.util.List;

// Adapter for Todo Recycler view.
public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.ViewHolder>
{
  public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
  {
    final ImageView mExpander;
    final LinearLayout.LayoutParams mExpanderLayoutParams;
    final SelReportEditText mEditText;
    final View mExpansion;
    public ViewHolder(final View itemView, final EditTextSoundRouter router)
    {
      super(itemView);
      mExpander = (ImageView)itemView.findViewById(R.id.expander);
      mExpanderLayoutParams = (LinearLayout.LayoutParams)mExpander.getLayoutParams();
      mEditText = (SelReportEditText)itemView.findViewById(R.id.todoText);
      mEditText.setOnFocusChangeListener(router);
      mEditText.mListener = router;
      mExpansion = itemView.findViewById(R.id.todoExpanded);
      final ImageButton b = (ImageButton)itemView.findViewById(R.id.todoExpandButton);
      b.setOnClickListener(this);
    }

    @Override public void onClick(View view)
    {
      if (mExpansion.getVisibility() == View.VISIBLE)
        mExpansion.setVisibility(View.GONE);
      else
        mExpansion.setVisibility(View.VISIBLE);
    }
  }

  @NonNull final EditTextSoundRouter mRouter;
  @NonNull final LayoutInflater mInflater;
  @NonNull final ArrayList<TodoViewModel> mFlatTodoList;
  public TodoAdapter(@NonNull final Context context, @NonNull final EditTextSoundRouter router, @NonNull final ArrayList<Todo> todoList)
  {
    mRouter = router;
    mInflater = LayoutInflater.from(context);
    mFlatTodoList = flatten(todoList, 0, 0);
  }

  private static ArrayList<TodoViewModel> flatten(@NonNull final List<Todo> list, final int start, final int depth)
  {
    final ArrayList<TodoViewModel> flatList = new ArrayList<>(list.size());
    for (final Todo t : list)
    {
      flatList.add(new TodoViewModel(t, flatList.size() + start, depth));
      if (!t.mChildren.isEmpty()) flatList.addAll(flatten(t.mChildren, flatList.size(), depth + 1));
    }
    return flatList;
  }

  @Override public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType)
  {
    return new ViewHolder(mInflater.inflate(R.layout.todo, parent, false), mRouter);
  }

  @Override public void onBindViewHolder(@NonNull final ViewHolder holder, final int position)
  {
    final TodoViewModel model = mFlatTodoList.get(position);
    holder.mExpanderLayoutParams.leftMargin = model.depth * 25 + 10;
    holder.mExpander.setLayoutParams(holder.mExpanderLayoutParams);
    holder.mEditText.setText(model.todo.mText);
    holder.mExpansion.setVisibility(View.GONE);
  }

  @Override public int getItemCount()
  {
    return mFlatTodoList.size();
  }
}
