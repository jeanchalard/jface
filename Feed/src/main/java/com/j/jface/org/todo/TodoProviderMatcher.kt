package com.j.jface.org.todo

import android.content.UriMatcher

// URI matcher for the Todo provider.
internal class TodoProviderMatcher private constructor() : UriMatcher(UriMatcher.NO_MATCH)
{
  companion object
  {
    val Matcher = TodoProviderMatcher()
    const val INDIVIDUAL_TODO_METADATA = 1

    init
    {
      Matcher.addURI(TodoProviderContract.BASE_URI_HOST, TodoProviderContract.UI_METADATA_TABLE_NAME + "/*", INDIVIDUAL_TODO_METADATA)
    }
  }
}
