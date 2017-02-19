package com.j.jface.org.todo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.j.jface.feed.Fences;

import java.util.ArrayList;
import java.util.HashMap;

// Get Todo from the provider. This bridges the awful content provider interface
// to an easy to use one.
public class TodoSource
{
  @NonNull final ContentResolver mResolver;

  public TodoSource(@NonNull final Context context)
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
    c.moveToFirst();
    return new Todo(
     c.getString(TodoProviderContract.COLUMNINDEX_id),
     c.getString(TodoProviderContract.COLUMNINDEX_ord),
     c.getLong(TodoProviderContract.COLUMNINDEX_creationTime),
     c.getLong(TodoProviderContract.COLUMNINDEX_completionTime),
     c.getString(TodoProviderContract.COLUMNINDEX_text),
     c.getInt(TodoProviderContract.COLUMNINDEX_depth),
     c.getInt(TodoProviderContract.COLUMNINDEX_lifeline),
     c.getInt(TodoProviderContract.COLUMNINDEX_deadline),
     c.getInt(TodoProviderContract.COLUMNINDEX_hardness),
     c.getInt(TodoProviderContract.COLUMNINDEX_timeConstraint),
     c.getInt(TodoProviderContract.COLUMNINDEX_estimatedTime),
     Fences.paramsFromName(c.getString(TodoProviderContract.COLUMNINDEX_where)));
  }

  @NonNull public ArrayList<Todo> fetchTodoList()
  {
    final String condition = "completionTime = 0";
    final Cursor c = mResolver.query(TodoProviderContract.BASE_URI, null, condition, null, "ord");
    if (null == c || !c.moveToFirst()) return new ArrayList<>();
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
       c.getInt(TodoProviderContract.COLUMNINDEX_lifeline),
       c.getInt(TodoProviderContract.COLUMNINDEX_deadline),
       c.getInt(TodoProviderContract.COLUMNINDEX_hardness),
       c.getInt(TodoProviderContract.COLUMNINDEX_timeConstraint),
       c.getInt(TodoProviderContract.COLUMNINDEX_estimatedTime),
       Fences.paramsFromName(c.getString(TodoProviderContract.COLUMNINDEX_where)));
      c.moveToNext();
      todos.add(t);
    }
    c.close();
    return todos;
  }

  @NonNull public Todo updateTodo(@NonNull final Todo todo)
  {
    mResolver.insert(TodoProviderContract.BASE_URI, contentValuesFromTodo(todo));
    return todo;
  }

  @NonNull private ContentValues contentValuesFromTodo(@NonNull final Todo todo)
  {
    final ContentValues cv = new ContentValues();
    cv.put(TodoProviderContract.COLUMN_id, todo.id);
    cv.put(TodoProviderContract.COLUMN_ord, todo.ord);
    cv.put(TodoProviderContract.COLUMN_creationTime, todo.creationTime);
    cv.put(TodoProviderContract.COLUMN_updateTime, System.currentTimeMillis());
    cv.put(TodoProviderContract.COLUMN_completionTime, todo.completionTime);
    cv.put(TodoProviderContract.COLUMN_text, todo.text);
    cv.put(TodoProviderContract.COLUMN_depth, todo.depth);
    cv.put(TodoProviderContract.COLUMN_lifeline, todo.lifeline);
    cv.put(TodoProviderContract.COLUMN_deadline, todo.deadline);
    cv.put(TodoProviderContract.COLUMN_hardness, todo.hardness);
    cv.put(TodoProviderContract.COLUMN_timeConstraint, todo.timeConstraint);
    cv.put(TodoProviderContract.COLUMN_estimatedTime, todo.estimatedTime);
    if (null != todo.where) cv.put(TodoProviderContract.COLUMN_where, todo.where.name);
    return cv;
  }
}
