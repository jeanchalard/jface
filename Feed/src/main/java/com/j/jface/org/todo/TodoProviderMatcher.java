package com.j.jface.org.todo;

import android.content.UriMatcher;
import android.support.annotation.NonNull;

// URI matcher for the Todo provider.
public class TodoProviderMatcher extends UriMatcher
{
  @NonNull public static final TodoProviderMatcher Matcher = new TodoProviderMatcher();

  public static final int ALL_CONTENT = 1;
  public static final int INDIVIDUAL_TODO = 2;

  public TodoProviderMatcher() { super(UriMatcher.NO_MATCH); }
  static
  {
    Matcher.addURI(TodoProviderContract.BASE_URI_HOST, TodoProviderContract.TABLE, ALL_CONTENT);
    Matcher.addURI(TodoProviderContract.BASE_URI_HOST, TodoProviderContract.TABLE + "/*", INDIVIDUAL_TODO);
  }
}
