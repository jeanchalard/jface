package com.j.jface;

import android.support.annotation.NonNull;

import java.util.concurrent.Semaphore;

public class FutureValue<T>
{
  private T mValue;
  private Semaphore mSem = new Semaphore(0);

  public boolean isDone()
  {
    return null != mValue;
  }

  @NonNull public T get()
  {
    while (null == mValue)
    {
      mSem.acquireUninterruptibly();
      mSem.release();
    }
    return mValue;
  }

  public void set(final T value)
  {
    mValue = value;
    mSem.release();
  }
}
