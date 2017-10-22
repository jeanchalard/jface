package com.j.jface.lifecycle;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragmentWrapper<T extends WrappedFragment> extends Fragment
{
  @Nullable private T mW;
  private final Object arg;

  @SuppressLint("ValidFragment")
  public FragmentWrapper(final Object a)
  {
    arg = a;
  }

  public FragmentWrapper()
  {
    throw new RuntimeException("Nopes. Stop doing crap.");
  }

  @Override public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle icicle)
  {
    mW = ((T)WrapUtils.build(getClass(),
     new Class[]{WrappedFragment.Args.class, arg.getClass()},
     new Object[]{new WrappedFragment.Args(this, inflater, container, icicle), arg}));
    return mW.getView();
  }

  @Override public void onActivityResult(final int requestCode, final int resultCode, final Intent data)
  {
    super.onActivityResult(requestCode, resultCode, data);
    assert mW != null;
    mW.onActivityResult(requestCode, resultCode, data);
  }
}
