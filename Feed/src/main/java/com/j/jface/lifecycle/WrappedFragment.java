package com.j.jface.lifecycle;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public abstract class WrappedFragment
{
  @NonNull protected final View mView;

  public static class Args
  {
    @NonNull public final Fragment fragment;
    public final LayoutInflater inflater;
    public final ViewGroup container;
    public final Bundle icicle;
    public Args(@NonNull final Fragment f, final LayoutInflater i, final ViewGroup c, final Bundle b)
    {
      fragment = f; inflater = i; container = c; icicle = b;
    }
  }

  protected WrappedFragment(@NonNull final View v)
  {
    mView = v;
  }

//    Must also implement :
//    public WrappedFragment(@NonNull final Args a, final T b)
//    ...where T is whatever is passed to the constructor or the wrapper, or suffer a runtime crash.

  @NonNull public View getView() { return mView; }
  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {}
  protected void onResume() {}
  protected void onPause() {}
  protected void onDestroy() {}
}
