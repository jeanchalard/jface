package com.j.jface.org.todo

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.os.Handler
import android.os.Message
import android.util.Log
import com.j.jface.Util
import com.j.jface.client.Client
import com.j.jface.client.action.drive.WriteFileAction
import com.j.jface.lifecycle.ContentProviderWrapper
import com.j.jface.lifecycle.WrappedContentProvider
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

// A provider of Todos.
// Needs to be public to be accessed by the booter
class TodoProviderBoot : ContentProviderWrapper<TodoProvider>()
class TodoProvider(a : WrappedContentProvider.Args) : WrappedContentProvider(a), Handler.Callback
{
  private val mClient : Client = Client(mC.context)
  private val mHandler : Handler = Handler(this)
  private val mDb : TodoOpenHelper = TodoOpenHelper(mC)

  private inner class TodoOpenHelper(mC : ContentProvider) : SQLiteOpenHelper(mC.context, DB_NAME, null, DB_VERSION)
  {
    override fun onCreate(sqLiteDatabase : SQLiteDatabase)
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
       /* 10 */              TodoProviderContract.COLUMN_constraintSql + " TINYINT," +
       /* 11 */              TodoProviderContract.COLUMN_estimatedTime + " INTEGER," +
       /* 12 */              TodoProviderContract.COLUMN_status + " INTEGER)")
      sqLiteDatabase.execSQL("CREATE TABLE " + TodoProviderContract.UI_METADATA_TABLE_NAME + " (" +
       /*  0 */              TodoProviderContract.COLUMN_id + " TEXT PRIMARY KEY NOT NULL," +
       /*  1 */              TodoProviderContract.COLUMN_open + " SHORT DEFAULT 1)")
    }

    override fun onUpgrade(sqLiteDatabase : SQLiteDatabase, i : Int, i1 : Int)
    {
      // So far no upgrade
    }
  }

  override fun query(uri : Uri, projection : Array<String>?, selection : String?, selectionArgs : Array<String>?, sortOrder : String?) : Cursor?
  {
    val db = mDb.readableDatabase
    when (TodoProviderMatcher.Matcher.match(uri))
    {
      TodoProviderMatcher.ALL_CONTENT -> return db.query(TodoProviderContract.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder)
      TodoProviderMatcher.INDIVIDUAL_TODO -> return getIndividualTodoData(db, TodoProviderContract.TABLE_NAME, uri, projection, sortOrder)
      TodoProviderMatcher.INDIVIDUAL_TODO_METADATA -> return getIndividualTodoData(db, TodoProviderContract.UI_METADATA_TABLE_NAME, uri, projection, sortOrder)
    }
    return null
  }

  private fun getIndividualTodoData(db : SQLiteDatabase, tableName : String, uri : Uri, projection : Array<String>?, sortOrder : String?) : Cursor?
  {
    val todoId = uri.lastPathSegment
    return db.query(tableName, projection, "id = ?", arrayOf(todoId), null, null, sortOrder)
  }

  override fun getType(uri : Uri) : String?
  {
    when (TodoProviderMatcher.Matcher.match(uri))
    {
      TodoProviderMatcher.ALL_CONTENT -> return TodoProviderContract.MIMETYPE_TODOLIST
      TodoProviderMatcher.INDIVIDUAL_TODO -> return TodoProviderContract.MIMETYPE_TODO
    }
    return null
  }

  override fun insert(uri : Uri, values : ContentValues) : Uri?
  {
    val db = mDb.writableDatabase
    // TODO : sanitize
    when (TodoProviderMatcher.Matcher.match(uri))
    {
      TodoProviderMatcher.INDIVIDUAL_TODO -> db.insertWithOnConflict(TodoProviderContract.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
      TodoProviderMatcher.INDIVIDUAL_TODO_METADATA -> db.insertWithOnConflict(TodoProviderContract.UI_METADATA_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
    val result = Uri.withAppendedPath(TodoProviderContract.BASE_URI_TODO, values.getAsString(TodoProviderContract.COLUMN_id))
    scheduleSync()
    return result
  }

  override fun delete(uri : Uri, selection : String?, selectionArgs : Array<String>?) : Int
  {
    val db = mDb.writableDatabase
    val result = db.delete(TodoProviderContract.TABLE_NAME, selection, selectionArgs)
    scheduleSync()
    return result
  }

  override fun update(uri : Uri, values : ContentValues, selection : String?, selectionArgs : Array<String>?) : Int
  {
    val db = mDb.writableDatabase
    // TODO : sanitize
    val result = db.update(TodoProviderContract.TABLE_NAME, values, selection, selectionArgs)
    scheduleSync()
    return result
  }

  private fun scheduleSync()
  {
    mHandler.removeMessages(SYNC)
    mHandler.sendEmptyMessageDelayed(SYNC, 3000) // 3 secs
  }

  override fun handleMessage(msg : Message) : Boolean
  {
    when (msg.what)
    {
      SYNC ->
        //        syncUp();
        return true
    }
    return false
  }

  private fun syncUp()
  {
    // getContext() is guaranteed to be non-null, because we are running after onCreate.
    val path = mC.context!!.getDatabasePath(DB_NAME)
    try
    {
      WriteFileAction(mClient, "Jormungand/Saves/latest", FileInputStream(path)).enqueue()
    }
    catch (e : FileNotFoundException)
    {
      Log.w("Jorg", "The todo DB in " + path.path + " can't be found ; can't back it up in the cloud.")
    }
  }

  companion object
  {
    private val DB_NAME = "TodoDb"
    private val DB_VERSION = 1

    private val SYNC = 1

    // A most dangerous method. It will take the specified file, and overwrite the current database
    // with its contents. It will destroy everything. So use it wisely.
    fun destroyDatabaseAndReplaceWithFileContents(context : Context, inputStream : InputStream) : Boolean
    {
      return try
      {
        Util.copy(inputStream, context.getDatabasePath(DB_NAME).absoluteFile)
        true
      }
      catch (e : IOException)
      {
        false
      }
    }
  }
}
