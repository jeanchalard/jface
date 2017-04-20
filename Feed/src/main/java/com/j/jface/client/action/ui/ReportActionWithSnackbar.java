package com.j.jface.client.action.ui;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.google.android.gms.common.api.GoogleApiClient;
import com.j.jface.client.Client;
import com.j.jface.client.action.Action;
import com.j.jface.client.action.ResultAction;

/**
 * An action showing a snack bar.
 * This is useful for long-running actions that need to report status and/or
 * completion ta the user.
 */
public class ReportActionWithSnackbar extends ResultAction<Integer>
{
  @NonNull final View mView;
  @NonNull final String mMessage;

  public ReportActionWithSnackbar(@NonNull final Client client, @Nullable final Action then, @NonNull final View viewToSnackbarInto, @NonNull final String message)
  {
    super(client, then);
    mView = viewToSnackbarInto;
    mMessage = message;
  }

  @Override public void run(@NonNull final GoogleApiClient client)
  {
    Snackbar.make(mView, mMessage, Snackbar.LENGTH_SHORT).show();
    finish(SUCCESS);
  }
}
