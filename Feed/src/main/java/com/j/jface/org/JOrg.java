package com.j.jface.org;

import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.j.jface.R;
import com.j.jface.lifecycle.WrappedActivity;
import com.j.jface.org.todo.Todo;

/**
 * Main activity class for JOrg.
 */

public class JOrg extends WrappedActivity
{
  public JOrg(@NonNull Args args)
  {
    super(args);
    mA.setContentView(R.layout.org_top);
    final LinearLayout top = (LinearLayout) mA.findViewById(R.id.orgTop);

    Todo tt[] = {
     new Todo("foo1")//, new Todo("foo2"), new Todo("foo3"), new Todo("foo4")
    };

    for (final Todo todo : tt)
    {
      top.addView(inflateTodo(todo));
    }
  }

  private View inflateTodo(final Todo t)
  {
    final View v = mA.getLayoutInflater().inflate(R.layout.todo_short, null);
    ((TextView)v.findViewById(R.id.todoText)).setText(t.mText);
    return v;
  }
}
