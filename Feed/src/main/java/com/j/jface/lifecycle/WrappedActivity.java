package com.j.jface.lifecycle;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

// A base class for a wrapped activity with a sane lifecycle
public class WrappedActivity
{
  @NonNull protected final AppCompatActivity mA;

  public static class Args
  {
    @NonNull public final AppCompatActivity activity;
    @Nullable public final Bundle icicle;
    public Args(@NonNull final AppCompatActivity a, @Nullable final Bundle b)
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
