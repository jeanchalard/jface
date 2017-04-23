package com.j.jface.org.todo;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.j.jface.client.Client;
import com.j.jface.lifecycle.WrappedContentProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

// A provider of Todos.
// Needs to be public to be accessed by the booter
public class TodoProvider extends WrappedContentProvider implements Handler.Callback
{
  @NonNull private final static String DB_NAME = "TodoDb";
  private final static int DB_VERSION = 1;

  private class TodoOpenHelper extends SQLiteOpenHelper
  {
    public TodoOpenHelper(@NonNull final ContentProvider mC)
    {
      super(mC.getContext(), DB_NAME, null, DB_VERSION);
    }

    @Override public void onCreate(final SQLiteDatabase sqLiteDatabase)
    {
      sqLiteDatabase.execSQL("CREATE TABLE " + TodoProviderContract.TABLE_NAME + " (" +
       /*  0 */              TodoProviderContract.COLUMN_id + " TEXT PRIMARY KEY NOT NULL," +
       /*  1 */              TodoProviderContract.COLUMN_ord + " TEXT," +
       /*  2 */              TodoProviderContract.COLUMN_creationTime + " INTEGER," +
       /*  3 */              TodoProviderContract.COLUMN_updateTime + " INTEGER," +
       /*  4 */              TodoProviderContract.COLUMN_completionTime + " INTEGER," +
       /*  5 */              TodoProviderContract.COLUMN_text + " TEXT NOT NULL," +
       /*  6 */              TodoProviderContract.COLUMN_depth + " INTEGER," +
       /*  7 */              TodoProviderContract.COLUMN_lifeline + " INTEGER," +
       /*  8 */              TodoProviderContract.COLUMN_deadline + " INTEGER," +
       /*  9 */              TodoProviderContract.COLUMN_hardness + " TINYINT," +
       /* 10 */              TodoProviderContract.COLUMN_constraint + " TINYINT," +
       /* 11 */              TodoProviderContract.COLUMN_estimatedTime + " INTEGER," +
       /* 12 */              TodoProviderContract.COLUMN_status + " INTEGER)");
      sqLiteDatabase.execSQL("CREATE TABLE " + TodoProviderContract.UI_METADATA_TABLE_NAME + " (" +
       /*  0 */              TodoProviderContract.COLUMN_id + " TEXT PRIMARY KEY NOT NULL," +
       /*  1 */              TodoProviderContract.COLUMN_open + " SHORT DEFAULT 1)");
    }

    @Override public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1)
    {
      // So far no upgrade
    }
  }

  @NonNull private final Client mClient;
  @NonNull private final Handler mHandler;
  @NonNull private final TodoOpenHelper mDb;

  public TodoProvider(@NonNull Args a)
  {
    super(a);
    //noinspection ConstantConditions – we are called in the original onCreate, making getContext NonNull. According to the doc.
    mClient = new Client(mC.getContext());
    mHandler = new Handler(this);
    mDb = new TodoOpenHelper(mC);
  }

  @Nullable public Cursor query(@NonNull final Uri uri, @Nullable final String[] projection, @Nullable final String selection, @Nullable final String[] selectionArgs, @Nullable final String sortOrder)
  {
    final SQLiteDatabase db = mDb.getReadableDatabase();
    switch (TodoProviderMatcher.Matcher.match(uri))
    {
      case TodoProviderMatcher.ALL_CONTENT :
        return db.query(TodoProviderContract.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
      case TodoProviderMatcher.INDIVIDUAL_TODO :
        return getIndividualTodoData(db, TodoProviderContract.TABLE_NAME, uri, projection, sortOrder);
      case TodoProviderMatcher.INDIVIDUAL_TODO_METADATA :
        return getIndividualTodoData(db, TodoProviderContract.UI_METADATA_TABLE_NAME, uri, projection, sortOrder);
    }
    return null;
  }

  @Nullable private Cursor getIndividualTodoData(@NonNull SQLiteDatabase db, @NonNull final String tableName, @NonNull final Uri uri, @Nullable final String[] projection, @Nullable final String sortOrder)
  {
    final String todoId = uri.getLastPathSegment();
    return db.query(tableName, projection, "id = ?", new String[] { todoId }, null, null, sortOrder);
  }

  @Nullable public String getType(@NonNull final Uri uri)
  {
    switch (TodoProviderMatcher.Matcher.match(uri))
    {
      case TodoProviderMatcher.ALL_CONTENT : return TodoProviderContract.MIMETYPE_TODOLIST;
      case TodoProviderMatcher.INDIVIDUAL_TODO : return TodoProviderContract.MIMETYPE_TODO;
    }
    return null;
  }

  @Nullable public Uri insert(@NonNull final Uri uri, @NonNull final ContentValues values)
  {
    final SQLiteDatabase db = mDb.getWritableDatabase();
    // TODO : sanitize
    switch (TodoProviderMatcher.Matcher.match(uri))
    {
      case TodoProviderMatcher.INDIVIDUAL_TODO:
        db.insertWithOnConflict(TodoProviderContract.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        break;
      case TodoProviderMatcher.INDIVIDUAL_TODO_METADATA:
        db.insertWithOnConflict(TodoProviderContract.UI_METADATA_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        break;
    }
    final Uri result = Uri.withAppendedPath(TodoProviderContract.BASE_URI_TODO, values.getAsString(TodoProviderContract.COLUMN_id));
    scheduleSync();
    return result;
  }

  public int delete(@NonNull final Uri uri, @Nullable final String selection, @Nullable final String[] selectionArgs)
  {
    final SQLiteDatabase db = mDb.getWritableDatabase();
    final int result = db.delete(TodoProviderContract.TABLE_NAME, selection, selectionArgs);
    scheduleSync();
    return result;
  }

  public int update(@NonNull final Uri uri, @NonNull final ContentValues values, @Nullable final String selection, @Nullable final String[] selectionArgs)
  {
    final SQLiteDatabase db = mDb.getWritableDatabase();
    // TODO : sanitize
    final int result = db.update(TodoProviderContract.TABLE_NAME, values, selection, selectionArgs);
    scheduleSync();
    return result;
  }


  private static final int SYNC = 1;
  private void scheduleSync()
  {
    mHandler.removeMessages(SYNC);
    mHandler.sendEmptyMessageDelayed(SYNC, 3000); // 3 secs
  }

  @Override public boolean handleMessage(@NonNull final Message msg)
  {
    switch (msg.what)
    {
      case SYNC:
        syncUp();
        return true;
    }
    return false;
  }

  private void syncUp()
  {
    // getContext() is guaranteed to be non-null, because we are running after onCreate.
    @SuppressWarnings("ConstantConditions") final File path = mC.getContext().getDatabasePath(DB_NAME);
    try
    {
      new WriteFileAction(mClient, "Jormungand/Saves/latest", new FileInputStream(path)).enqueue();
    }
    catch (FileNotFoundException e)
    {
      Log.w("Jorg", "The todo DB in " + path.getPath() + " can't be found ; can't back it up in the cloud.");
      return;
    }
  }
}
