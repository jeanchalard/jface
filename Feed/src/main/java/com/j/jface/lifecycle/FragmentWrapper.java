package com.j.jface.lifecycle;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import static com.j.jface.lifecycle.WrappedFragment.*;

public class FragmentWrapper<T extends WrappedFragment> extends Fragment
{
  @Nullable private T mW;
  private final Class<T> wrappedClass;
  private final Object arg;

  @SuppressLint("ValidFragment")
  public FragmentWrapper(final Class<T> wrapped, final Object a)
  {
    wrappedClass = wrapped;
    arg = a;
  }

  public FragmentWrapper()
  {
    throw new RuntimeException("Nopes. Stop doing crap.");
  }

  @SuppressWarnings("unchecked") @Override public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle icicle)
  {
    mW = (T)WrapUtils.build(wrappedClass,
     new Class[]{Args.class, arg.getClass()},
     new Object[]{new Args(this, inflater, container, icicle), arg});
    return mW.getView();
  }

  @Override public void onActivityResult(final int requestCode, final int resultCode, final Intent data)
  {
    super.onActivityResult(requestCode, resultCode, data);
    assert mW != null;
    mW.onActivityResult(requestCode, resultCode, data);
  }

  @Override public void onResume()
  {
    super.onResume();
    assert mW != null;
    mW.onResume();
  }

  @Override public void onPause()
  {
    super.onPause();
    assert mW != null;
    mW.onPause();
  }
}
