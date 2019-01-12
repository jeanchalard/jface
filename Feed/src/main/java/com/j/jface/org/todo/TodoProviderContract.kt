package com.j.jface.org.todo

import android.net.Uri

import com.j.jface.Const

internal object TodoProviderContract
{
  const val BASE_URI_SCHEME = "content"
  const val BASE_URI_HOST = Const.APP_PACKAGE + ".provider"
  const val UI_METADATA_TABLE_NAME = "uiMetadata"
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
  const val COLUMN_estimatedMinutes = "estimatedMinutes"

  // UI save table
  const val COLUMN_open = "open"

  const val COLUMNINDEX_open = 1
}
