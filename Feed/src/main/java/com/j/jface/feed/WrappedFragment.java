package com.j.jface.feed;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class WrappedFragment
{
  @NonNull protected final View mView;

  public static class Args
  {
    public final Fragment fragment;
    public final LayoutInflater inflater;
    public final ViewGroup container;
    public final Bundle icicle;
    public Args(final Fragment f, final LayoutInflater i, final ViewGroup c, final Bundle b)
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
}
