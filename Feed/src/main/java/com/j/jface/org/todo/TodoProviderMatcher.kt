package com.j.jface.org.todo

import android.content.UriMatcher

// URI matcher for the Todo provider.
internal class TodoProviderMatcher private constructor() : UriMatcher(UriMatcher.NO_MATCH)
{
  companion object
  {
    val Matcher = TodoProviderMatcher()

    const val ALL_CONTENT = 1
    const val INDIVIDUAL_TODO = 2
    const val INDIVIDUAL_TODO_METADATA = 3

    init
    {
      Matcher.addURI(TodoProviderContract.BASE_URI_HOST, TodoProviderContract.TABLE_NAME, ALL_CONTENT)
      Matcher.addURI(TodoProviderContract.BASE_URI_HOST, TodoProviderContract.TABLE_NAME + "/*", INDIVIDUAL_TODO)
      Matcher.addURI(TodoProviderContract.BASE_URI_HOST, TodoProviderContract.UI_METADATA_TABLE_NAME + "/*", INDIVIDUAL_TODO_METADATA)
    }
  }
}
