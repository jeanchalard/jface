package com.j.jface.client.action;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.j.jface.Future;
import com.j.jface.FutureValue;
import com.j.jface.client.Client;

/**
 * An action with a result.
 */
public abstract class ResultAction<T> extends Action implements Future<T>
{
  private final FutureValue<T> result;
  public ResultAction(@NonNull final Client client, @Nullable final Action dependency)
  {
    super(client, dependency);
    result = new FutureValue<>();
  }

  @Override protected void finish()
  {
    throw new RuntimeException("Can't finish a ResultAction without a result.");
  }

  protected void finish(final T value)
  {
    result.set(value);
  }

  @Nullable public T get() { return result.get(); }
  public int status() { return result.status(); }
}
