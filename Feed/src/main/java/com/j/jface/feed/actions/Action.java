package com.j.jface.feed.actions;

import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;

public interface Action
{
  void run(@NonNull final GoogleApiClient client);
}
