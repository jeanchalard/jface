package com.j.jface;

/**
 * A simpler Future
 */
public interface Future<T>
{
  public static final int NOT_DONE = 0;
  public static final int SUCCESS = 1;
  public static final int FAILURE = 2;

  public T get();
  public int status();
}
