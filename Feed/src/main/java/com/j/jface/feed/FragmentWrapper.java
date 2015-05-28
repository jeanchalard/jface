package com.j.jface.feed;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class FragmentWrapper<T extends WrappedFragment> extends Fragment
{
  private final Object arg;
  public FragmentWrapper(final Object a)
  {
    arg = a;
  }

  public FragmentWrapper()
  {
    throw new RuntimeException("Nopes. Stop making crap.");
  }

  @Override public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle icicle)
  {
    try
    {
      final Class c = wrappedClass(getClass());
      Constructor cr = c.getConstructor(WrappedFragment.Args.class, arg.getClass());
      T i = (T)cr.newInstance(new WrappedFragment.Args(this, inflater, container, icicle), arg);
      return i.getView();
    }
    catch (java.lang.InstantiationException | IllegalAccessException
     | InvocationTargetException | NoSuchMethodException e)
    {
      throw new RuntimeException(e); // Nopes dude, never happens unless you do crap in the constructor
    }
  }

  private static Class<?> wrappedClass(final Class<?> c)
  {
    final ParameterizedType pt = (ParameterizedType)c.getGenericSuperclass();
    final Type t = pt.getActualTypeArguments()[0];
    return (Class)t;
  }
}
