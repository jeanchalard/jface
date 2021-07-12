package com.j.jface.lifecycle;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;

/**
 * A wrapper for activities.
 */
public abstract class AppCompatActivityWrapper<T extends WrappedActivity> extends AppCompatActivity
{
  @Nullable private T mW;

  // Activity callbacks
  @Override protected void onCreate(@Nullable final Bundle icicle)
  {
    mW = WrapUtils.buildFromParent(getClass(),
     new Class[]{WrappedActivity.Args.class},
     new Object[]{new WrappedActivity.Args(this, icicle, getIntent())});
    // Do not pass the icicle to avoid the framework trying to use its broken fragment-recreation mechanics
    super.onCreate(null);
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();
    assert mW != null;
    mW.onDestroy();
  }

  @SuppressLint("MissingSuperCall")
  @Override public void onSaveInstanceState(@NonNull final Bundle savedInstanceState)
  {
    // Do not call super, because the framework would try to save fragment state, but its way of doing it is crap
    assert mW != null;
    mW.onSaveInstanceState(savedInstanceState);
  }

  @Override public void onConfigurationChanged(final Configuration c)
  {
    super.onConfigurationChanged(c);
    assert mW != null;
    mW.onConfigurationChanged(c);
  }

  @Override public boolean onOptionsItemSelected(final MenuItem i)
  {
    assert mW != null;
    if (mW.onOptionsItemSelected(i)) return true;
    return super.onOptionsItemSelected(i);
  }

  @Override public void onPostCreate(final Bundle b)
  {
    super.onPostCreate(b);
    assert mW != null;
    mW.onPostCreate(b);
  }

  @Override public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] results)
  {
    super.onRequestPermissionsResult(requestCode, permissions, results);
    assert mW != null;
    mW.onRequestPermissionsResult(requestCode, permissions, results);
  }

  @Override protected void onActivityResult(final int requestCode, final int resultCode, final Intent data)
  {
    super.onActivityResult(requestCode, resultCode, data);
    assert mW != null;
    mW.onActivityResult(requestCode, resultCode, data);
  }

  @Override public void onPause()
  {
    super.onPause();
    assert mW != null;
    mW.onPause();
  }

  @Override public void onResume()
  {
    super.onResume();
    assert mW != null;
    mW.onResume();
  }
}
