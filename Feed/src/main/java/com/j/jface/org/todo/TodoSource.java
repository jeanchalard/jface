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
    void onTodoRemoved(@NonNull final TodoCore todo);
  }

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
      switch (type) {
        case REMOVED:
          for (ListChangeListener l : listeners) l.onTodoRemoved(todo);
          break;
        case ADDED:
        case MODIFIED:
          for (ListChangeListener l : listeners) l.onTodoUpdated(todo);
          break;
      }
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
    synchronized (mLock)
    {
      Firebase.TodoUpdateListener.INSTANCE.addListener(this);
      final List<TodoCore> l = Firebase.INSTANCE.getTodoList();
      if (l instanceof ArrayList) return (ArrayList<TodoCore>)l;
      return new ArrayList<>(l);
    }
  }

  @NonNull public TodoCore updateTodo(@NonNull final TodoCore todo)
  {
    Firebase.INSTANCE.updateTodo(todo);
    return todo;
  }

  public boolean updateTodoOpen(@NonNull final Todo todo)
  {
    final ContentValues cv = new ContentValues();
    cv.put(TodoProviderContract.COLUMN_id, todo.id);
    cv.put(TodoProviderContract.COLUMN_open, todo.ui.open);
    final Uri uri = Uri.withAppendedPath(TodoProviderContract.INSTANCE.getBASE_URI_METADATA(), todo.id);
    mResolver.insert(uri, cv);
    return todo.ui.open;
  }

  public boolean isOpen(@NonNull final TodoCore todo)
  {
    final Uri uri = Uri.withAppendedPath(TodoProviderContract.INSTANCE.getBASE_URI_METADATA(), todo.id);
    final Cursor c = mResolver.query(uri, null, null, null, null);
    if (null == c || c.getCount() != 1) return true;
    c.moveToFirst();
    final int open = c.getShort(TodoProviderContract.COLUMNINDEX_open);
    c.close();
    return open != 0;
  }
}
