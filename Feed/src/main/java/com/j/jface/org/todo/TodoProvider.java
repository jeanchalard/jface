package com.j.jface.org.todo;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.j.jface.lifecycle.WrappedContentProvider;

// A provider of Todos.
public class TodoProvider extends WrappedContentProvider
{
  @NonNull private final static String DB_NAME = "TodoDb";
  @NonNull private final static int DB_VERSION = 1;
  @NonNull private final static String TABLE_NAME = "todo";

  private class TodoOpenHelper extends SQLiteOpenHelper
  {
    public TodoOpenHelper(@NonNull final ContentProvider mC)
    {
      super(mC.getContext(), DB_NAME, null, DB_VERSION);
    }

    @Override public void onCreate(final SQLiteDatabase sqLiteDatabase)
    {
      sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
       "id TEXT PRIMARY KEY NOT NULL," +
       "text TEXT NOT NULL," +
       "parent TEXT," +
       "lifeline INTEGER," +
       "deadline INTEGER," +
       "hardness TINYINT," +
       "timeConstraint TINYINT," +
       "where INTEGER," +
       "estimatedTime INTEGER)");
    }

    @Override public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1)
    {
      // So far no upgrade
    }
  }

  @NonNull private final TodoOpenHelper mDb;

  public TodoProvider(@NonNull Args a)
  {
    super(a);
    mDb = new TodoOpenHelper(mC);
  }

  @Nullable public Cursor query(@NonNull final Uri uri, @Nullable final String[] projection, @Nullable final String selection, @Nullable final String[] selectionArgs, @Nullable final String sortOrder)
  {
  }

  @Nullable public String getType(@NonNull final Uri uri)
  {
  }

  @Nullable public Uri insert(@NonNull final Uri uri, @NonNull final ContentValues contentValues)
  {
    final SQLiteDatabase db = mDb.getWritableDatabase();
    db.insert(TABLE_NAME, null, contentValues);
  }

  public int delete(@NonNull final Uri uri, @Nullable final String selection, @Nullable final String[] selectionArgs)
  {
  }

  public int update(@NonNull final Uri uri, @NonNull final ContentValues values, @Nullable final String selection, @Nullable final String[] selectionArgs)
  {
  }
}
