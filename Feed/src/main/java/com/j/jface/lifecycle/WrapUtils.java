package com.j.jface.lifecycle;

import android.support.annotation.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Utils for wrappers
 */
public class WrapUtils
{
  private static <T> Class<T> wrappedClass(final Class<?> c)
  {
    final ParameterizedType pt = (ParameterizedType)c.getGenericSuperclass();
    final Type t = pt.getActualTypeArguments()[0];
    //noinspection unchecked
    return (Class<T>)t;
  }

  @NonNull
  public static <T> T buildFromParent(final Class<?> parent, final Class<?>[] argTypes, final Object[] args)
  {
    return build(WrapUtils.wrappedClass(parent), argTypes, args);
  }

  @NonNull
  public static <T> T build(final Class<T> c, final Class<?> argTypes[], final Object[] args)
  {
    try
    {
      Constructor<T> cr = c.getConstructor(argTypes);
      return cr.newInstance(args);
    }
    catch (java.lang.InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | RuntimeException e)
    {
      throw new RuntimeException(e); // Nopes dude, never happens unless you do crap in the constructor
    }
  }
}
