package com.j.jface.org.todo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;

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
  // Does not perform the expensive lookup for parent and children ; any mClient needing that should
  // just look up the entire tree.
  @Nullable public Todo getTodoFromIdWithoutHierarchy(@NonNull final String id)
  {
    final Uri uri = Uri.withAppendedPath(TodoProviderContract.BASE_URI_TODO, id);
    final Cursor c = mResolver.query(uri, null, null, null, null);
    if (null == c || c.getCount() != 1) return null;
    c.moveToFirst();
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
     c.getInt(TodoProviderContract.COLUMNINDEX_constraint),
     c.getInt(TodoProviderContract.COLUMNINDEX_estimatedTime));
    c.close();
    return t;
  }

  @NonNull public ArrayList<Todo> fetchTodoList()
  {
    final String condition = "completionTime = 0";
    final Cursor c = mResolver.query(TodoProviderContract.BASE_URI_TODO, null, condition, null, "ord");
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
       c.getInt(TodoProviderContract.COLUMNINDEX_constraint),
       c.getInt(TodoProviderContract.COLUMNINDEX_estimatedTime));
      c.moveToNext();
      todos.add(t);
    }
    c.close();
    return todos;
  }

  @NonNull public Todo updateTodo(@NonNull final Todo todo)
  {
    mResolver.insert(Uri.withAppendedPath(TodoProviderContract.BASE_URI_TODO, todo.id), contentValuesFromTodo(todo));
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
    cv.put(TodoProviderContract.COLUMN_constraint, todo.constraint);
    cv.put(TodoProviderContract.COLUMN_estimatedTime, todo.estimatedTime);
    return cv;
  }

  public boolean updateTodoOpen(@NonNull final Todo todo, final boolean open)
  {
    final ContentValues cv = new ContentValues();
    cv.put(TodoProviderContract.COLUMN_id, todo.id);
    cv.put(TodoProviderContract.COLUMN_open, open);
    final Uri uri = Uri.withAppendedPath(TodoProviderContract.BASE_URI_METADATA, todo.id);
    mResolver.insert(uri, cv);
    return open;
  }

  public boolean isOpen(@NonNull final Todo todo)
  {
    final Uri uri = Uri.withAppendedPath(TodoProviderContract.BASE_URI_METADATA, todo.id);
    final Cursor c = mResolver.query(uri, null, null, null, null);
    if (null == c || c.getCount() != 1) return true;
    c.moveToFirst();
    final int open = c.getShort(TodoProviderContract.COLUMNINDEX_open);
    c.close();
    return open != 0;
  }
}
