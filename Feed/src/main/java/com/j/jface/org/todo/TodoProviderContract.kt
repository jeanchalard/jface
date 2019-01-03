package com.j.jface.org.todo

import android.net.Uri

import com.j.jface.Const

internal object TodoProviderContract
{
  const val BASE_URI_SCHEME = "content"
  const val BASE_URI_HOST = Const.APP_PACKAGE + ".provider"
  const val TABLE_NAME = "todo"
  const val UI_METADATA_TABLE_NAME = "uiMetadata"
  val BASE_URI_TODO = Uri.parse("${BASE_URI_SCHEME}://${BASE_URI_HOST}/${TABLE_NAME}")!!
  val BASE_URI_METADATA = Uri.parse("${BASE_URI_SCHEME}://${BASE_URI_HOST}/${UI_METADATA_TABLE_NAME}")!!

  const val MIMETYPE_TODOLIST = "vnd.android.cursor.dir/vnd.com.j.jface.todolist"
  const val MIMETYPE_TODO = "vnd.android.cursor.dir/vnd.com.j.jface.todo"

  const val COLUMN_id = "id"
  const val COLUMN_ord = "ord"
  const val COLUMN_creationTime = "creationTime"
  const val COLUMN_updateTime = "lastUpdateTime"
  const val COLUMN_completionTime = "completionTime"
  const val COLUMN_text = "text"
  const val COLUMN_depth = "depth"
  const val COLUMN_lifeline = "lifeline"
  const val COLUMN_deadline = "deadline"
  const val COLUMN_hardness = "hardness"
  const val COLUMN_constraint = "constraint"
  const val COLUMN_constraintSql = "constraintSpec" // "constraint" is a reserved SQL word :/
  const val COLUMN_estimatedTime = "estimatedTime"
  const val COLUMN_status = "status"

  const val COLUMNINDEX_id = 0
  const val COLUMNINDEX_ord = 1
  const val COLUMNINDEX_creationTime = 2
  const val COLUMNINDEX_updateTime = 3
  const val COLUMNINDEX_completionTime = 4
  const val COLUMNINDEX_text = 5
  const val COLUMNINDEX_depth = 6
  const val COLUMNINDEX_lifeline = 7
  const val COLUMNINDEX_deadline = 8
  const val COLUMNINDEX_hardness = 9
  const val COLUMNINDEX_constraint = 10
  const val COLUMNINDEX_estimatedTime = 11
  const val COLUMNINDEX_status = 12

  // UI save table
  const val COLUMN_open = "open"

  const val COLUMNINDEX_open = 1
}
