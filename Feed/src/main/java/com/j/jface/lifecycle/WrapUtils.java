package com.j.jface.lifecycle;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Utils for wrappers
 */
public class WrapUtils
{
  public static Class<?> wrappedClass(final Class<?> c)
  {
    final ParameterizedType pt = (ParameterizedType)c.getGenericSuperclass();
    final Type t = pt.getActualTypeArguments()[0];
    return (Class)t;
  }

  public static Object build(final Class parent, final Class<?> argTypes[], final Object[] args)
  {
    try
    {
      final Class c = WrapUtils.wrappedClass(parent);
      Constructor cr = c.getConstructor(argTypes);
      return cr.newInstance(args);
    }
    catch (java.lang.InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
    {
      throw new RuntimeException(e); // Nopes dude, never happens unless you do crap in the constructor
    }
  }
}
