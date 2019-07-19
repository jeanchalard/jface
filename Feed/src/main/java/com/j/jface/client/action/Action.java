package com.j.jface.client.action;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.api.GoogleApiClient;
import com.j.jface.client.Client;

public abstract class Action
{
  @NonNull protected final Client mClient;
  @Nullable private final Action mDependency;

  public Action(@NonNull final Client client, @Nullable final Action dependency)
  {
    mClient = client;
    mDependency = dependency;
  }

  public void enqueue()
  {
    mClient.enqueue(this);
  }

  protected void finish()
  {
    if (null != mDependency) mClient.enqueue(mDependency);
  }

  abstract public void run(@NonNull final GoogleApiClient client);
}
