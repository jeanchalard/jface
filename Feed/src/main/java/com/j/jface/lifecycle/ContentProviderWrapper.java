package com.j.jface.lifecycle;

import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;

// The class that sanitizes the content provider lifecycle.
@SuppressLint("Registered")
public class ContentProviderWrapper<T extends WrappedContentProvider> extends ContentProvider
{
  @Nullable private T mW;

  @Override public boolean onCreate()
  {
    mW = WrapUtils.buildFromParent(getClass(),
     new Class[]{WrappedContentProvider.Args.class},
     new Object[]{new WrappedContentProvider.Args(this)});
    return true;
  }

  @Nullable @Override public Cursor query(@Nullable final Uri uri, @Nullable final String[] projection, @Nullable final String selection, @Nullable final String[] selectionArgs, @Nullable final String sortOrder)
  {
    assert mW != null;
    if (null == uri) return null;
    return mW.query(uri, projection, selection, selectionArgs, sortOrder);
  }

  @Nullable @Override public String getType(@Nullable final Uri uri)
  {
    assert mW != null;
    if (null == uri) return null;
    return mW.getType(uri);
  }

  @Nullable @Override public Uri insert(@Nullable final Uri uri, @Nullable final ContentValues values)
  {
    assert mW != null;
    if (null == uri || null == values) return null;
    return mW.insert(uri, values);
  }

  @Override public int delete(@Nullable final Uri uri, @Nullable final String selection, @Nullable final String[] selectionArgs)
  {
    assert mW != null;
    if (null == uri) return 0;
    return mW.delete(uri, selection, selectionArgs);
  }

  @Override public int update(@Nullable final Uri uri, @Nullable final ContentValues values, @Nullable final String selection, @Nullable final String[] selectionArgs)
  {
    assert mW != null;
    if (null == uri || null == values) return 0;
    return mW.update(uri, values, selection, selectionArgs);
  }
}
