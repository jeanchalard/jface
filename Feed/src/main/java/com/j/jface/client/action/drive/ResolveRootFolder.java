package com.j.jface.client.action.drive;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.j.jface.client.Client;
import com.j.jface.client.action.Action;
import com.j.jface.client.action.ResultAction;

/**
 * A very simple ResultAction which resolves the root folder.
 */
public class ResolveRootFolder extends ResultAction<WithPath.DriveFolder>
{
  public ResolveRootFolder(@NonNull final Client client, @Nullable final Action then)
  {
    super(client, then);
  }

  @Override public void run(@NonNull final GoogleApiClient client)
  {
    finish(new WithPath.DriveFolder(Drive.DriveApi.getRootFolder(client), ""));
  }
}
