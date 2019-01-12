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
class TodoProvider(a : WrappedContentProvider.Args) : WrappedContentProvider(a)
{
  private val mDb : TodoOpenHelper = TodoOpenHelper(mC)

  private inner class TodoOpenHelper(mC : ContentProvider) : SQLiteOpenHelper(mC.context, DB_NAME, null, DB_VERSION)
  {
    override fun onCreate(sqLiteDatabase : SQLiteDatabase)
    {
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
      TodoProviderMatcher.INDIVIDUAL_TODO_METADATA -> return getIndividualTodoData(db, uri, projection, sortOrder)
    }
    return null
  }

  private fun getIndividualTodoData(db : SQLiteDatabase, uri : Uri, projection : Array<String>?, sortOrder : String?) : Cursor?
  {
    val todoId = uri.lastPathSegment
    return db.query(TodoProviderContract.UI_METADATA_TABLE_NAME, projection, "id = ?", arrayOf(todoId), null, null, sortOrder)
  }

  override fun insert(uri : Uri, values : ContentValues) : Uri?
  {
    val db = mDb.writableDatabase
    // TODO : sanitize
    when (TodoProviderMatcher.Matcher.match(uri))
    {
      TodoProviderMatcher.INDIVIDUAL_TODO_METADATA -> db.insertWithOnConflict(TodoProviderContract.UI_METADATA_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
    return Uri.withAppendedPath(TodoProviderContract.BASE_URI_METADATA, values.getAsString(TodoProviderContract.COLUMN_id))
  }

  override fun delete(uri : Uri, selection : String?, selectionArgs : Array<String>?) : Int
  {
    return mDb.writableDatabase.delete(TodoProviderContract.UI_METADATA_TABLE_NAME, selection, selectionArgs)
  }

  override fun update(uri : Uri, values : ContentValues, selection : String?, selectionArgs : Array<String>?) : Int
  {
    // TODO : sanitize
    return mDb.writableDatabase.update(TodoProviderContract.UI_METADATA_TABLE_NAME, values, selection, selectionArgs)
  }

  companion object
  {
    private val DB_NAME = "TodoDb"
    private val DB_VERSION = 1
  }
}
