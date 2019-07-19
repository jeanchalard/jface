package com.j.jface.client.action.drive;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.j.jface.client.Client;
import com.j.jface.client.action.Action;
import com.j.jface.client.action.ResultAction;

/**
 * An action that moves a drive file to a new name.
 */
class TrashFileAction extends ResultAction<Integer> implements ResultCallback<Status>
{
  @NonNull final WithPath.Metadata mFile;

  public TrashFileAction(@NonNull final Client client, @Nullable final Action dependency, @NonNull final WithPath.Metadata file)
  {
    super(client, dependency);
    mFile = file;
  }

  @Override public void run(@NonNull final GoogleApiClient client)
  {
    mFile.metadata.getDriveId().asDriveResource().trash(client).setResultCallback(this);
  }

  @Override public void onResult(@NonNull final Status status)
  {
    if (status.isSuccess()) finish(SUCCESS);
    else fail(ResultHelpers.errorMessage(status, "Unknown error in trashing file"));
  }
}
