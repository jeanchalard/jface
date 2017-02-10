package com.j.jface.org.editor;

import android.support.annotation.NonNull;

import com.j.jface.R;
import com.j.jface.lifecycle.WrappedActivity;

// An activity that provides detailed editing for a single Todo.
public class TodoEditor extends WrappedActivity
{
  protected TodoEditor(@NonNull final Args a)
  {
    super(a);
    mA.setContentView(R.layout.todo_editor);
  }
}
