package com.j.jface.client.action.drive;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder.DriveFileResult;
import com.google.android.gms.drive.MetadataChangeSet;
import com.j.jface.FutureValue;
import com.j.jface.Util;
import com.j.jface.client.Client;
import com.j.jface.client.action.Action;
import com.j.jface.client.action.ResultAction;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An action that moves a drive file to a new name.
 */
class CopyFileAction extends ResultAction<Integer> implements ResultCallback<Result>
{
  @NonNull final WithPath.Metadata mFile;
  @NonNull final String mNewName;
  @NonNull final WithPath.DriveFolder mParent;
  @NonNull final FutureValue<DriveContents> mSource;
  @NonNull final FutureValue<DriveContents> mDestination;
  public CopyFileAction(@NonNull final Client client, @Nullable final Action dependency, @NonNull final WithPath.Metadata file, @NonNull final String newName, @NonNull final WithPath.DriveFolder parent)
  {
    super(client, dependency);
    mFile = file;
    mNewName = newName;
    mParent = parent;
    mSource = new FutureValue<>();
    mDestination = new FutureValue<>();
  }

  @Override public void run(@NonNull final GoogleApiClient client)
  {
    if (FAILURE == mSource.status()) { fail(mSource.getError()); return; }
    if (NOT_DONE == mSource.status()) { mFile.metadata.getDriveId().asDriveFile().open(client, DriveFile.MODE_READ_ONLY, null).setResultCallback(this); return; }

    if (FAILURE == mDestination.status()) { fail(mDestination.getError()); return; }
    if (NOT_DONE == mDestination.status()) { Drive.DriveApi.newDriveContents(client).setResultCallback(this); return; }

    final DriveContents source = mSource.get();
    final DriveContents destination = mDestination.get();

    if (null == source || null == destination) { fail("Resolved stream is null"); return; };

    final InputStream sourceStream = source.getInputStream();
    final OutputStream destinationStream = destination.getOutputStream();

    try
    {
      Util.copy(sourceStream, destinationStream);
    }
    catch (final IOException e)
    {
      fail("Impossible to copy source file to destination : " + e.toString());
      return;
    }

    final MetadataChangeSet cs = new MetadataChangeSet.Builder().setTitle(mNewName).build();
    mParent.folder.createFile(client, cs, destination).setResultCallback(this);
  }

  @Override public void onResult(@NonNull final Result result)
  {
    if (!result.getStatus().isSuccess())
    {
      fail(ResultHelpers.errorMessage(result, result instanceof DriveContentsResult ? "Unknown error in retrieving file" : "Unknown error in uploading copy of file"));
      return;
    }

    if (result instanceof DriveContentsResult)
    {
      final DriveContents contents = ((DriveContentsResult)result).getDriveContents();
      if (NOT_DONE == mSource.status()) mSource.set(contents);
      else mDestination.set(contents);
      enqueue();
    }
    else if (result instanceof DriveFileResult)
      finish(SUCCESS);
    else fail("Unknown result in copying file");
  }
}
