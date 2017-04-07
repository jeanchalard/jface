package com.j.jface.client.action.drive;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveApi.MetadataBufferResult;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.j.jface.Future;
import com.j.jface.FutureValue;
import com.j.jface.client.Client;
import com.j.jface.client.action.Action;
import com.j.jface.client.action.ResultAction;

import java.util.Arrays;
import java.util.List;

/**
 * An action that resolves a DriveFolder, creating it in the process if necessary.
 */
public class ResolveDriveFolder extends ResultAction<DriveFolder> implements ResultCallback
{
  @NonNull private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
  @NonNull final String mName;
  @NonNull final Future<DriveFolder> mParent;
  @NonNull final FutureValue<MetadataBuffer> mQueryResult;

  public ResolveDriveFolder(@NonNull final Client client, @Nullable final Action then, @NonNull final String path)
  {
    this(client, then, Arrays.asList(path.split("/")));
  }

  public ResolveDriveFolder(@NonNull final Client client, @Nullable final Action then, @NonNull final List<String> path)
  {
    super(client, then);
    // I wish I could call through to this(Client, Action, getParent(), path). But getParent() would have to be able to
    // create the ResolveDriveFolder, which means it needs 'this', which can't be called until super() has gone through.
    if (path.size() <= 1)
      mParent = new FutureValue<>();
    else
      mParent = new ResolveDriveFolder(client, this, path.subList(0, path.size() - 1));
    mQueryResult = new FutureValue<>();
    mName = path.get(path.size() - 1);
  }

  public ResolveDriveFolder(@NonNull final Client client, @Nullable final Action then, @NonNull Future<DriveFolder> parent, @NonNull final String path)
  {
    this(client, then, parent, Arrays.asList(path.split("/")));
  }

  public ResolveDriveFolder(@NonNull final Client client, @Nullable final Action then, @NonNull Future<DriveFolder> parent, @NonNull final List<String> path)
  {
    super(client, then);
    mParent = parent;
    mQueryResult = new FutureValue<>();
    mName = path.get(path.size() - 1);
  }

  @Override public void run(@NonNull final GoogleApiClient client)
  {
    if (FAILURE == mParent.status()) { fail(mParent.getError()); return; }
    else if (FAILURE == mQueryResult.status()) { fail(mQueryResult.getError()); return; }

    if (NOT_DONE == mParent.status())
    {
      if (mParent instanceof Action) { ((Action)mParent).enqueue(); return; }
      else ((FutureValue<DriveFolder>)mParent).set(Drive.DriveApi.getRootFolder(client));
    }

    final DriveFolder parent = mParent.get();
    if (NOT_DONE == mQueryResult.status())
    {
      final Query q = new Query.Builder()
       .addFilter(Filters.eq(SearchableField.TITLE, mName))
       .addFilter(Filters.in(SearchableField.PARENTS, parent.getDriveId()))
       .addFilter(Filters.eq(SearchableField.MIME_TYPE, FOLDER_MIME_TYPE))
       .addFilter(Filters.eq(SearchableField.TRASHED, false))
       .build();
      Drive.DriveApi.query(client, q).setResultCallback(this);
      return;
    }

    final MetadataBuffer mbr = mQueryResult.get();
    if (null == mbr) { fail("Successfully returned MetadataBuffer is null."); return; }
    for (final Metadata m : mbr)
      if (m.isFolder())
      {
        finish(m.getDriveId().asDriveFolder());
        return;
      }

    // No directory by this name found
    final MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(mName).build();
    parent.createFolder(client, changeSet).setResultCallback(this);
    // finish() (or fail()) will be called by onResult.
  }

  @Override public void onResult(@NonNull final Result result)
  {
    if (!result.getStatus().isSuccess())
    {
      final String error = result.getStatus().getStatusMessage();
      if (null == error) fail("ResolveDriveFolder : Unknown error : " + result);
      else fail(error);
      return;
    }
    if (result instanceof MetadataBufferResult)
    {
      mQueryResult.set(((MetadataBufferResult)result).getMetadataBuffer());
      enqueue();
      return;
    }
    else // result instanceof DriveFolderResult
      finish(((DriveFolder.DriveFolderResult)result).getDriveFolder());
  }
}
