package com.j.jface.lifecycle;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MenuItem;

/**
 * A wrapper for activities.
 */
public abstract class ActivityWrapper<T extends WrappedActivity> extends Activity
{
  @Nullable private T mW;

  // Activity callbacks
  @Override protected void onCreate(@Nullable final Bundle icicle)
  {
    super.onCreate(null);
    mW = (T)WrapUtils.build(getClass(),
     new Class[]{WrappedActivity.Args.class},
     new Object[]{new WrappedActivity.Args(this, icicle)});
  }

  @Override public void onSaveInstanceState(@NonNull final Bundle savedInstanceState)
  {
    mW.onSaveInstanceState(savedInstanceState);
  }

  @Override public void onConfigurationChanged(final Configuration c)
  {
    super.onConfigurationChanged(c);
    mW.onConfigurationChanged(c);
  }

  @Override public boolean onOptionsItemSelected(final MenuItem i)
  {
    if (mW.onOptionsItemSelected(i)) return true;
    return super.onOptionsItemSelected(i);
  }

  @Override public void onPostCreate(final Bundle b)
  {
    super.onPostCreate(b);
    mW.onPostCreate(b);
  }

  @Override public void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] results)
  {
    super.onRequestPermissionsResult(requestCode, permissions, results);
    mW.onRequestPermissionsResult(requestCode, permissions, results);
  }
}
