package com.j.jface.org.todo;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.j.jface.Const;

// Contract constants for the todo provider.
public class TodoProviderContract
{
  @NonNull public static final String BASE_URI_SCHEME = "content";
  @NonNull public static final String BASE_URI_HOST = Const.APP_PACKAGE + ".provider";
  @NonNull public static final String TABLE = "todo";
  @NonNull public static final Uri BASE_URI = Uri.parse(BASE_URI_SCHEME + "://" + BASE_URI_HOST + "/" + TABLE);

  @NonNull public static final String MIMETYPE_TODOLIST = "vnd.android.cursor.dir/vnd.com.j.jface.todolist";
  @NonNull public static final String MIMETYPE_TODO = "vnd.android.cursor.dir/vnd.com.j.jface.todo";

  @NonNull public static final String COLUMN_id = "id";
  @NonNull public static final String COLUMN_creationTime = "creationTime";
  @NonNull public static final String COLUMN_updateTime = "updateTime";
  @NonNull public static final String COLUMN_text = "text";
  @NonNull public static final String COLUMN_parent = "parent";
  @NonNull public static final String COLUMN_lifeline = "lifeline";
  @NonNull public static final String COLUMN_deadline = "deadline";
  @NonNull public static final String COLUMN_hardness = "hardness";
  @NonNull public static final String COLUMN_timeConstraint = "timeConstraint";
  @NonNull public static final String COLUMN_where = "fenceName";
  @NonNull public static final String COLUMN_estimatedTime = "estimatedTime";
  @NonNull public static final String COLUMN_status = "status";

  @NonNull public static final int COLUMNINDEX_id = 0;
  @NonNull public static final int COLUMNINDEX_creationTime = 1;
  @NonNull public static final int COLUMNINDEX_updateTime = 2;
  @NonNull public static final int COLUMNINDEX_text = 3;
  @NonNull public static final int COLUMNINDEX_parent = 4;
  @NonNull public static final int COLUMNINDEX_lifeline = 5;
  @NonNull public static final int COLUMNINDEX_deadline = 6;
  @NonNull public static final int COLUMNINDEX_hardness = 7;
  @NonNull public static final int COLUMNINDEX_timeConstraint = 8;
  @NonNull public static final int COLUMNINDEX_where = 9;
  @NonNull public static final int COLUMNINDEX_estimatedTime = 10;
  @NonNull public static final int COLUMNINDEX_status = 11;
}
