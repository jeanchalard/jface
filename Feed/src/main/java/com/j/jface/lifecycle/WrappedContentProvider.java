package com.j.jface.lifecycle;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

// A base class for a content provider with a sane lifecycle
public class WrappedContentProvider
{
  @NonNull protected final ContentProvider mC;

  public static class Args
  {
    @NonNull public final ContentProvider contentProvider;
    public Args(@NonNull final ContentProvider a)
    {
      contentProvider = a;
    }
  }

  protected WrappedContentProvider(@NonNull final WrappedContentProvider.Args a)
  {
    mC = a.contentProvider;
  }

  @Nullable public Cursor query(@NonNull final Uri uri, @Nullable final String[] projection, @Nullable final String selection, @Nullable final String[] selectionArgs, @Nullable final String sortOrder)
  {
    return null;
  }

  @Nullable public String getType(@NonNull final Uri uri)
  {
    return null;
  }

  @Nullable public Uri insert(@NonNull final Uri uri, @NonNull final ContentValues contentValues)
  {
    return null;
  }

  public int delete(@NonNull final Uri uri, @Nullable final String selection, @Nullable final String[] selectionArgs)
  {
    return 0;
  }

  public int update(@NonNull final Uri uri, @NonNull final ContentValues values, @Nullable final String selection, @Nullable final String[] selectionArgs)
  {
    return 0;
  }
}
