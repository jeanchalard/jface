package com.j.jface.client.action;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.api.GoogleApiClient;
import com.j.jface.Future;
import com.j.jface.FutureValue;
import com.j.jface.client.Client;

/**
 * An action with a result of type T.
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

  protected void finish(@Nullable final T value)
  {
    result.set(value);
    super.finish();
  }
  protected void fail(@NonNull final String error) { result.fail(error); }

  @Nullable public T get() { return result.get(); }
  public int status() { return result.status(); }
  @NonNull public String getError() { return result.getError(); }

  /**
   * A result action which is already done at the time it starts.
   * Pass the result to the constructor.
   */
  public static class Done<T> extends ResultAction<T>
  {
    public Done(@NonNull final Client client, @Nullable T value)
    {
      super(client, null);
      finish(value);
    }
    @Override public void run(@NonNull final GoogleApiClient client) {}
  }
}
