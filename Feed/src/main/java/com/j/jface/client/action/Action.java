package com.j.jface.client.action;

import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;

public interface Action
{
  public void run(@NonNull final GoogleApiClient client);
}
