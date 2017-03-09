package com.j.jface.org.todo;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.j.jface.Const;

// Contract constants for the todo provider.
public class TodoProviderContract
{
  @NonNull public static final String BASE_URI_SCHEME = "content";
  @NonNull public static final String BASE_URI_HOST = Const.APP_PACKAGE + ".provider";
  @NonNull public static final String TABLE_NAME = "todo";
  @NonNull public static final String UI_METADATA_TABLE_NAME = "uiMetadata";
  @NonNull public static final Uri BASE_URI_TODO = Uri.parse(BASE_URI_SCHEME + "://" + BASE_URI_HOST + "/" + TABLE_NAME);
  @NonNull public static final Uri BASE_URI_METADATA = Uri.parse(BASE_URI_SCHEME + "://" + BASE_URI_HOST + "/" + UI_METADATA_TABLE_NAME);

  @NonNull public static final String MIMETYPE_TODOLIST = "vnd.android.cursor.dir/vnd.com.j.jface.todolist";
  @NonNull public static final String MIMETYPE_TODO = "vnd.android.cursor.dir/vnd.com.j.jface.todo";

  @NonNull public static final String COLUMN_id = "id";
  @NonNull public static final String COLUMN_ord = "ord";
  @NonNull public static final String COLUMN_creationTime = "creationTime";
  @NonNull public static final String COLUMN_updateTime = "updateTime";
  @NonNull public static final String COLUMN_completionTime = "completionTime";
  @NonNull public static final String COLUMN_text = "text";
  @NonNull public static final String COLUMN_depth = "depth";
  @NonNull public static final String COLUMN_lifeline = "lifeline";
  @NonNull public static final String COLUMN_deadline = "deadline";
  @NonNull public static final String COLUMN_hardness = "hardness";
  @NonNull public static final String COLUMN_constraint = "constraintSpec"; // "constraint" is a reserved SQL word :/
  @NonNull public static final String COLUMN_estimatedTime = "estimatedTime";
  @NonNull public static final String COLUMN_status = "status";

  public static final int COLUMNINDEX_id = 0;
  public static final int COLUMNINDEX_ord = 1;
  public static final int COLUMNINDEX_creationTime = 2;
  public static final int COLUMNINDEX_updateTime = 3;
  public static final int COLUMNINDEX_completionTime = 4;
  public static final int COLUMNINDEX_text = 5;
  public static final int COLUMNINDEX_depth = 6;
  public static final int COLUMNINDEX_lifeline = 7;
  public static final int COLUMNINDEX_deadline = 8;
  public static final int COLUMNINDEX_hardness = 9;
  public static final int COLUMNINDEX_constraint = 10;
  public static final int COLUMNINDEX_estimatedTime = 11;
  public static final int COLUMNINDEX_status = 12;

  // UI save table
  @NonNull public static final String COLUMN_open = "open";

  public static final int COLUMNINDEX_open = 1;
}
