package com.j.jface.lifecycle;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MenuItem;

/**
 * An base class for a wrapped activity
 */

public class WrappedActivity
{
  @NonNull protected final Activity mA;

  public static class Args
  {
    @NonNull public final Activity activity;
    @Nullable public final Bundle icicle;
    public Args(@NonNull final Activity a, @Nullable final Bundle b)
    {
      activity = a; icicle = b;
    }
  }

  protected WrappedActivity(@NonNull final Args a)
  {
    mA = a.activity;
  }

  // Stubs for wrapped activities that don't want to implement.
  public void onSaveInstanceState(@NonNull final Bundle savedInstanceState) {}
  public void onConfigurationChanged(final Configuration c) {}
  public boolean onOptionsItemSelected(final MenuItem i) { return false; }
  public void onPostCreate(final Bundle b) {}
  public void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] results) {}
  public void onPause() {}
  public void onResume() {}
}
