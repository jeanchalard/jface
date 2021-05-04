package com.j.jface.lifecycle;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {}
  public void onDestroy() {}
  public void onPause() {}
  public void onResume() {}
  public boolean isResumed()
  {
    try
    {
      final Method method = AppCompatActivity.class.getMethod("isResumed");
      return (Boolean)method.invoke(mA);
    } catch (NoSuchMethodException|IllegalAccessException|InvocationTargetException e) {}
    return false;
  }
}
