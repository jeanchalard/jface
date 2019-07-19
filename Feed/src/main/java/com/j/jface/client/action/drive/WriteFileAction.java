package com.j.jface.client.action.drive;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder.DriveFileResult;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.SearchableField;
import com.j.jface.FutureValue;
import com.j.jface.Util;
import com.j.jface.client.Client;
import com.j.jface.client.action.ResultAction;
import com.j.jface.client.action.drive.ResolveDriveResourcesAction.ResolveFirstFileAction;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * An action that writes a file to Drive with the given data.
 */
public class WriteFileAction extends ResultAction<Integer> implements ResultCallback<Result>
{
  @NonNull final String mFilename;
  @NonNull final InputStream mSource;
  @NonNull final ResultAction<WithPath.DriveFolder> mParent;
  @NonNull final ResultAction<WithPath.Metadata> mResolvedMetadata;
  @NonNull final FutureValue<DriveContents> mResolvedContents;
  public WriteFileAction(@NonNull final Client client, @NonNull final String filepath, @NonNull final InputStream source)
  {
    super(client, null);
    mSource = source;
    final List<String> path = Arrays.asList(filepath.split("/"));
    mFilename = path.get(path.size() - 1);
    if (path.size() <= 1)
      mParent = new ResolveRootFolder(client, this);
    else
      mParent = new ResolveOrCreateDriveFolderAction(client, this, path.subList(0, path.size() - 1));
    mResolvedMetadata = new ResolveFirstFileAction(client, this, mParent, Filters.eq(SearchableField.TITLE, mFilename));
    mResolvedContents = new FutureValue<>();
  }

  @Override public void run(@NonNull final GoogleApiClient gClient)
  {
    if (FAILURE == mParent.status()) { fail(mParent.getError()); return; }
    if (NOT_DONE == mParent.status()) { mParent.enqueue(); return; }
    if (FAILURE == mResolvedMetadata.status()) { fail(mResolvedMetadata.getError()); return; }
    if (NOT_DONE == mResolvedMetadata.status()) { mResolvedMetadata.enqueue(); return; }

    if (FAILURE == mResolvedContents.status()) { fail(mResolvedContents.getError()); return; }
    if (NOT_DONE == mResolvedContents.status())
    {
      final WithPath.Metadata metadata = mResolvedMetadata.get();
      if (null == metadata)
        Drive.DriveApi.newDriveContents(gClient).setResultCallback(this);
      else
        metadata.metadata.getDriveId().asDriveFile().open(gClient, DriveFile.MODE_WRITE_ONLY, null).setResultCallback(this); // File exists, find drive contents
      return;
    }

    final DriveContents contents = mResolvedContents.get();
    if (null == contents) { fail("Resolved contents is null in write file action"); return; }

    final OutputStream out = contents.getOutputStream();
    try
    {
      Util.copy(mSource, out);
    }
    catch (IOException e)
    {
      fail("IOException in writing file : " + e);
    }

    if (null == mResolvedMetadata.get())
    {
      // New file
      final WithPath.DriveFolder parent = mParent.get();
      if (null == parent) { fail("Resolved parent is null in write file action"); return; }
      final MetadataChangeSet newFileChanges = new MetadataChangeSet.Builder().setTitle(mFilename).build();
      parent.folder.createFile(gClient, newFileChanges, contents).setResultCallback(this);
    }
    else
      contents.commit(gClient, null).setResultCallback(this);
  }

  @Override public void onResult(@NonNull final Result result)
  {
    if (result instanceof DriveContentsResult)
    {
      if (!result.getStatus().isSuccess()) { fail(ResultHelpers.errorMessage(result, "Failure in getting drive contents in write file action")); return; }
      mResolvedContents.set(((DriveContentsResult)result).getDriveContents());
      enqueue();
    }
    else if (result instanceof Status || result instanceof DriveFileResult)
    {
      if (!result.getStatus().isSuccess()) { fail(ResultHelpers.errorMessage(result, "Failure in writing drive contents in write file action")); return; }
      finish(SUCCESS);
    }
    else fail("Unknown result type in write file action : " + result.getClass());
  }
}
