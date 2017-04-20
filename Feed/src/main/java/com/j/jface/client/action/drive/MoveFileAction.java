package com.j.jface.client.action.drive;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource.MetadataResult;
import com.google.android.gms.drive.MetadataChangeSet;
import com.j.jface.FutureValue;
import com.j.jface.client.Client;
import com.j.jface.client.action.Action;
import com.j.jface.client.action.ResultAction;

import java.util.HashSet;

/**
 * An action that moves a drive file to a new name.
 */
class MoveFileAction extends ResultAction<Integer> implements ResultCallback<Result>
{
  @NonNull final WithPath.Metadata mFile;
  @NonNull final String mNewName;
  @NonNull final WithPath.DriveFolder mParent;
  @NonNull final FutureValue<MetadataResult> mTitleChangeResult;

  public MoveFileAction(@NonNull final Client client, @Nullable final Action dependency, @NonNull final WithPath.Metadata file, @NonNull final String newName, @NonNull final WithPath.DriveFolder parent)
  {
    super(client, dependency);
    mFile = file;
    mNewName = newName;
    mParent = parent;
    mTitleChangeResult = new FutureValue<>();
  }

  @Override public void run(@NonNull final GoogleApiClient client)
  {
    if (NOT_DONE == mTitleChangeResult.status())
    {
      final MetadataChangeSet cs = new MetadataChangeSet.Builder()
       .setTitle(mNewName)
       .build();
      mFile.metadata.getDriveId().asDriveFile().updateMetadata(client, cs).setResultCallback(this);
      return;
    }

    if (FAILURE == mTitleChangeResult.status())
    {
      fail("Error trying to move file " + mFile.path + " to " + mNewName + " : " + ResultHelpers.errorMessage(mTitleChangeResult.get(), "Unknown error while changing title"));
      return;
    }

    if (mFile.path.startsWith(mParent.path)) { finish(SUCCESS); return; } // Already in this folder.

    // Move folder.
    final HashSet<DriveId> parents = new HashSet<>();
    parents.add(mParent.folder.getDriveId());
    mFile.metadata.getDriveId().asDriveFile().setParents(client, parents).setResultCallback(this);
  }

  @Override public void onResult(@NonNull final Result result)
  {
    if (result instanceof MetadataResult)
    {
      mTitleChangeResult.set((MetadataResult)result);
      enqueue();
    }
    else if (result instanceof Status)
    {
      final Status status = (Status)result;
      if (status.isSuccess())
        finish(SUCCESS);
      else
        fail(null == status.getStatusMessage() ? "Unknown error while changing parents" : status.getStatusMessage());
    }
    else fail("Unknown result");
  }
}
