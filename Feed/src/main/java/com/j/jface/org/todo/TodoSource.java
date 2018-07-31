package com.j.jface.org.todo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.firestore.DocumentChange;
import com.j.jface.firebase.Firebase;

import java.util.ArrayList;
import java.util.List;

// Get Todo from the provider. This bridges the awful content provider interface
// to an easy to use one.
public class TodoSource implements Firebase.TodoUpdateListener.Listener
{
  public interface ListChangeListener
  {
    void onTodoUpdated(@NonNull final TodoCore todo);
  }

  private static final boolean FIRESTORE = true;
  @NonNull private final ContentResolver mResolver;

  public TodoSource(@NonNull final Context context)
  {
    mResolver = context.getContentResolver();
  }

  private final Object mLock = new Object();
  private final ArrayList<ListChangeListener> listeners = new ArrayList<>();
  @Override public void onTodoUpdated(@NonNull final DocumentChange.Type type, @NonNull final TodoCore todo)
  {
    synchronized (mLock)
    {
      Log.e("Update", "" + type + " : " + todo);
      for (ListChangeListener l : listeners) l.onTodoUpdated(todo);
    }
  }
  public void addListChangeListener(@NonNull final ListChangeListener l)
  {
    synchronized (mLock) { listeners.add(l); }
  }
  public void removeListChangeListener(@NonNull final ListChangeListener l)
  {
    synchronized (mLock) { listeners.remove(l); }
  }

  @NonNull public ArrayList<TodoCore> fetchTodoList()
  {
    if (FIRESTORE)
      synchronized (mLock)
      {
        Firebase.TodoUpdateListener.INSTANCE.addListener(this);
        final List<TodoCore> l = Firebase.INSTANCE.getTodoList();
        if (l instanceof ArrayList) return (ArrayList<TodoCore>)l;
        return new ArrayList<>(l);
      }
    else
    {
      final String condition = "completionTime = 0";
      final Cursor c = mResolver.query(TodoProviderContract.BASE_URI_TODO, null, condition, null, "ord");
      if (null == c || !c.moveToFirst()) return new ArrayList<>();
      final ArrayList<TodoCore> todos = new ArrayList<>(c.getCount());
      while (!c.isAfterLast())
      {
        final TodoCore t = new TodoCore(
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
         c.getInt(TodoProviderContract.COLUMNINDEX_estimatedTime),
         c.getLong(TodoProviderContract.COLUMNINDEX_updateTime));
        c.moveToNext();
        todos.add(t);
        // Uncomment to write the local DB to remote
        // Firebase.INSTANCE.updateTodo(t);
      }
      c.close();
      return todos;
    }
  }

  @NonNull public TodoCore updateTodo(@NonNull final TodoCore todo)
  {
    mResolver.insert(Uri.withAppendedPath(TodoProviderContract.BASE_URI_TODO, todo.id), contentValuesFromTodo(todo));
    Firebase.INSTANCE.updateTodo(todo);
    return todo;
  }

  @NonNull private ContentValues contentValuesFromTodo(@NonNull final TodoCore todo)
  {
    final ContentValues cv = new ContentValues();
    cv.put(TodoProviderContract.COLUMN_id, todo.id);
    cv.put(TodoProviderContract.COLUMN_ord, todo.ord);
    cv.put(TodoProviderContract.COLUMN_creationTime, todo.creationTime);
    cv.put(TodoProviderContract.COLUMN_completionTime, todo.completionTime);
    cv.put(TodoProviderContract.COLUMN_text, todo.text);
    cv.put(TodoProviderContract.COLUMN_depth, todo.depth);
    cv.put(TodoProviderContract.COLUMN_lifeline, todo.lifeline);
    cv.put(TodoProviderContract.COLUMN_deadline, todo.deadline);
    cv.put(TodoProviderContract.COLUMN_hardness, todo.hardness);
    cv.put(TodoProviderContract.COLUMN_constraint, todo.constraint);
    cv.put(TodoProviderContract.COLUMN_estimatedTime, todo.estimatedTime);
    cv.put(TodoProviderContract.COLUMN_updateTime, System.currentTimeMillis());
    return cv;
  }

  public boolean updateTodoOpen(@NonNull final Todo todo)
  {
    final ContentValues cv = new ContentValues();
    cv.put(TodoProviderContract.COLUMN_id, todo.id);
    cv.put(TodoProviderContract.COLUMN_open, todo.ui.open);
    final Uri uri = Uri.withAppendedPath(TodoProviderContract.BASE_URI_METADATA, todo.id);
    mResolver.insert(uri, cv);
    return todo.ui.open;
  }

  public boolean isOpen(@NonNull final TodoCore todo)
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
