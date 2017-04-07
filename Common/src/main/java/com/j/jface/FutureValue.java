package com.j.jface;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.omg.CORBA.UNKNOWN;

import java.util.concurrent.Semaphore;

public class FutureValue<T> implements Future<T>
{
  private class Obj
  {
    @NonNull public static final String UNKNOWN_ERROR = "FutureValue : Unknown error";
    @Nullable private final T value;
    private final int status;
    @NonNull private final String error;
    public Obj(@Nullable final T value, final int status, @NonNull final String error) { this.value = value; this.status = status; this.error = error; }
  }
  private Obj obj = new Obj(null, NOT_DONE, Obj.UNKNOWN_ERROR);
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
    obj = new Obj(value, SUCCESS, Obj.UNKNOWN_ERROR);
    sem.release();
  }

  public void fail(@NonNull final String error)
  {
    if (sem.tryAcquire()) throw new RuntimeException("FutureValue.fail/set called multiple times ; old \"" + obj.value + "\"");
    obj = new Obj(null, FAILURE, error);
    sem.release();
  }

  @NonNull public String getError()
  {
    if (obj.status != FAILURE) throw new RuntimeException("Don't call error() if the FutureValue doesn't have FAILURE status.");
    return obj.error;
  }
}
