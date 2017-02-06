package com.j.jface.org.todo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.j.jface.feed.Fences;

import java.util.ArrayList;

// Get Todo from the provider. This bridges the awful content provider interface
// to an easy to use one.
public class TodoSource
{
  @NonNull final ContentResolver mResolver;

  public TodoSource(final Context context)
  {
    mResolver = context.getContentResolver();
  }

  // Returns null if no such Todo, or if multiple Todos with this ID (which is supposed to be impossible)
  // Does not perform the expensive lookup for parent and children ; any client needing that should
  // just look up the entire tree.
  @Nullable public Todo getTodoFromIdWithoutHierarchy(@NonNull final String id)
  {
    final Uri uri = Uri.withAppendedPath(TodoProviderContract.BASE_URI, id);
    final Cursor c = mResolver.query(uri, null, null, null, null);
    if (null == c || c.getCount() != 1) return null;
    return new Todo(
     c.getString(TodoProviderContract.COLUMNINDEX_id),
     c.getString(TodoProviderContract.COLUMNINDEX_ord),
     c.getLong(TodoProviderContract.COLUMNINDEX_creationTime),
     c.getLong(TodoProviderContract.COLUMNINDEX_completionTime),
     c.getString(TodoProviderContract.COLUMNINDEX_text),
     c.getInt(TodoProviderContract.COLUMNINDEX_depth),
     new Planning(
      c.getInt(TodoProviderContract.COLUMNINDEX_lifeline),
      c.getInt(TodoProviderContract.COLUMNINDEX_deadline),
      c.getInt(TodoProviderContract.COLUMNINDEX_hardness),
      c.getInt(TodoProviderContract.COLUMNINDEX_timeConstraint),
      Fences.paramsFromName(c.getString(TodoProviderContract.COLUMNINDEX_where))
     ),
     c.getInt(TodoProviderContract.COLUMNINDEX_estimatedTime));
  }

  @NonNull public TodoList fetchTodoList()
  {
    final String condition = "completionTime = 0";
    final Cursor c = mResolver.query(TodoProviderContract.BASE_URI, null, condition, null, "ord");
    if (null == c || !c.moveToFirst()) return new TodoList();
    final ArrayList<Todo> todos = new ArrayList<>(c.getCount());
    while (!c.isAfterLast())
    {
      final Todo t = new Todo(
       c.getString(TodoProviderContract.COLUMNINDEX_id),
       c.getString(TodoProviderContract.COLUMNINDEX_ord),
       c.getLong(TodoProviderContract.COLUMNINDEX_creationTime),
       c.getLong(TodoProviderContract.COLUMNINDEX_completionTime),
       c.getString(TodoProviderContract.COLUMNINDEX_text),
       c.getInt(TodoProviderContract.COLUMNINDEX_depth),
       new Planning(
        c.getInt(TodoProviderContract.COLUMNINDEX_lifeline),
        c.getInt(TodoProviderContract.COLUMNINDEX_deadline),
        c.getInt(TodoProviderContract.COLUMNINDEX_hardness),
        c.getInt(TodoProviderContract.COLUMNINDEX_timeConstraint),
        Fences.paramsFromName(c.getString(TodoProviderContract.COLUMNINDEX_where))
       ),
       c.getInt(TodoProviderContract.COLUMNINDEX_estimatedTime));
      c.moveToNext();
      todos.add(t);
    }
    c.close();
    return new TodoList(todos);
  }

  @NonNull public Todo updateTodo(@NonNull final Todo todo)
  {
    mResolver.insert(TodoProviderContract.BASE_URI, contentValuesFromTodo(todo));
    return todo;
  }

  @NonNull private ContentValues contentValuesFromTodo(@NonNull final Todo todo)
  {
    final ContentValues cv = new ContentValues();
    cv.put(TodoProviderContract.COLUMN_id, todo.mId);
    cv.put(TodoProviderContract.COLUMN_ord, todo.mOrd);
    cv.put(TodoProviderContract.COLUMN_creationTime, todo.mCreationTime);
    cv.put(TodoProviderContract.COLUMN_updateTime, System.currentTimeMillis());
    cv.put(TodoProviderContract.COLUMN_completionTime, todo.mCompletionTime);
    cv.put(TodoProviderContract.COLUMN_text, todo.mText);
    cv.put(TodoProviderContract.COLUMN_depth, todo.mDepth);
    cv.put(TodoProviderContract.COLUMN_lifeline, todo.mPlanning.mLifeline);
    cv.put(TodoProviderContract.COLUMN_deadline, todo.mPlanning.mDeadline);
    cv.put(TodoProviderContract.COLUMN_hardness, todo.mPlanning.mHardness);
    cv.put(TodoProviderContract.COLUMN_timeConstraint, todo.mPlanning.mTimeConstraint);
    if (null != todo.mPlanning.mWhere) cv.put(TodoProviderContract.COLUMN_where, todo.mPlanning.mWhere.name);
    cv.put(TodoProviderContract.COLUMN_estimatedTime, todo.mEstimatedTime);
    return cv;
  }
}
