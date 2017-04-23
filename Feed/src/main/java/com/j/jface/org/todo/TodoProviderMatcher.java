package com.j.jface.org.todo;

import android.content.UriMatcher;
import android.support.annotation.NonNull;

// URI matcher for the Todo provider.
class TodoProviderMatcher extends UriMatcher
{
  @NonNull public static final TodoProviderMatcher Matcher = new TodoProviderMatcher();

  public static final int ALL_CONTENT = 1;
  public static final int INDIVIDUAL_TODO = 2;
  public static final int INDIVIDUAL_TODO_METADATA = 3;

  public TodoProviderMatcher() { super(UriMatcher.NO_MATCH); }
  static
  {
    Matcher.addURI(TodoProviderContract.BASE_URI_HOST, TodoProviderContract.TABLE_NAME, ALL_CONTENT);
    Matcher.addURI(TodoProviderContract.BASE_URI_HOST, TodoProviderContract.TABLE_NAME + "/*", INDIVIDUAL_TODO);
    Matcher.addURI(TodoProviderContract.BASE_URI_HOST, TodoProviderContract.UI_METADATA_TABLE_NAME + "/*", INDIVIDUAL_TODO_METADATA);
  }
}
