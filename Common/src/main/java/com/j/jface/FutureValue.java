package com.j.jface;

import android.support.annotation.Nullable;

import java.util.concurrent.Semaphore;

public class FutureValue<T> implements Future<T>
{
  private class Obj { @Nullable private final T value; private final int status; public Obj(@Nullable final T value, final int status) { this.value = value; this.status = status; } }
  private Obj obj = new Obj(null, NOT_DONE);
  private Semaphore sem = new Semaphore(0);

  public int status() { return obj.status; }

  @Nullable public T get()
  {
    if (NOT_DONE != obj.status) return obj.value;
    sem.acquireUninterruptibly();
    sem.release();
    return obj.value;
  }

  public void set(final T value)
  {
    if (sem.tryAcquire()) throw new RuntimeException("FutureValue.set/fail called multiple times ; old \"" + obj.value + "\" ; new \"" + value + "\"");
    obj = new Obj(value, SUCCESS);
    sem.release();
  }

  public void fail()
  {
    if (sem.tryAcquire()) throw new RuntimeException("FutureValue.fail/set called multiple times ; old \"" + obj.value + "\"");
    obj = new Obj(null, FAILURE);
    sem.release();
  }
}
