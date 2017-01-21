package com.j.jface.lifecycle;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragmentWrapper<T extends WrappedFragment> extends Fragment
{
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
    return ((T)WrapUtils.build(getClass(),
     new Class[]{WrappedFragment.Args.class, arg.getClass()},
     new Object[]{new WrappedFragment.Args(this, inflater, container, icicle), arg})).getView();
  }
}
