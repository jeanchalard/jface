package com.j.jface.client.action.drive;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.DriveFolder.DriveFolderResult;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filter;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.SearchableField;
import com.j.jface.client.Client;
import com.j.jface.client.action.Action;
import com.j.jface.client.action.ResultAction;
import com.j.jface.client.action.drive.ResolveDriveResourcesAction.ResolveFirstFolderAction;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * An action that resolves a DriveFolder, creating it in the process if necessary.
 */
public class ResolveOrCreateDriveFolderAction extends ResultAction<WithPath.DriveFolder> implements ResultCallback<DriveFolderResult>
{
  @NonNull private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
  @NonNull private final String mName;
  @NonNull private final ResultAction<WithPath.DriveFolder> mParent;
  @NonNull private final ResolveFirstFolderAction mResolved;

  public ResolveOrCreateDriveFolderAction(@NonNull final Client client, @Nullable final Action then, @NonNull final String path)
  {
    this(client, then, Arrays.asList(path.split(File.separator)));
  }
  public ResolveOrCreateDriveFolderAction(@NonNull final Client client, @Nullable final Action then, @NonNull final List<String> path)
  {
    super(client, then);
    mName = path.get(path.size() - 1);
    // I wish I could call through to this(Client, Action, getParent(), path). But getParent() would have to be able to
    // create the ResolveOrCreateDriveFolderAction, which means it needs 'this', which can't be called until super() has gone through.
    if (path.size() <= 1)
      mParent = new ResolveRootFolder(client, this);
    else
      mParent = new ResolveOrCreateDriveFolderAction(client, this, path.subList(0, path.size() - 1));
    mResolved = new ResolveFirstFolderAction(client, this, mParent, getFilterForName(mName));
  }

  public ResolveOrCreateDriveFolderAction(@NonNull final Client client, @Nullable final Action then, @NonNull ResultAction<WithPath.DriveFolder> parent, @NonNull final String path)
  {
    this(client, then, parent, Arrays.asList(path.split(File.separator)));
  }
  public ResolveOrCreateDriveFolderAction(@NonNull final Client client, @Nullable final Action then, @NonNull ResultAction<WithPath.DriveFolder> parent, @NonNull final List<String> path)
  {
    super(client, then);
    mName = path.get(path.size() - 1);
    mParent = parent;
    mResolved = new ResolveFirstFolderAction(client, this, mParent, getFilterForName(mName));
  }

  private static Filter getFilterForName(@NonNull final String name)
  {
    return Filters.and(
     Filters.eq(SearchableField.TITLE, name),
     Filters.eq(SearchableField.MIME_TYPE, FOLDER_MIME_TYPE)
    );
  }

  @Override public void run(@NonNull final GoogleApiClient client)
  {
    if (FAILURE == mResolved.status()) { fail(mResolved.getError()); return; }

    // mParent will be resolved by mResolvedMetadata, as mResolvedMetadata has a dependency to it.
    if (NOT_DONE == mResolved.status()) { mResolved.enqueue(); return; }

    final WithPath.DriveFolder resolved = mResolved.get();
    if (null != resolved)
    {
      // Existed already, was successfully resolved
      finish(resolved);
      return;
    }

    // No directory by this name found
    final WithPath.DriveFolder parent = mParent.get();
    if (null == parent) { fail("Couldn't create parent directory"); return; }
    final MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(mName).build();
    parent.folder.createFolder(client, changeSet).setResultCallback(this);
    // finish() (or fail()) will be called by onResult.
  }

  @Override public void onResult(@NonNull final DriveFolderResult result)
  {
    if (!result.getStatus().isSuccess())
    {
      final String error = result.getStatus().getStatusMessage();
      if (null == error) fail("ResolveDriveResourceAction : Unknown error : " + result);
      else fail(error);
      return;
    }

    final WithPath.DriveFolder parent = mParent.get();
    if (null == parent) { fail("Already resolved folder is null ?!"); return; } // Can't happen because it would have been detected earlier, in theory
    finish(new WithPath.DriveFolder(result.getDriveFolder(), parent.path + File.separator + mName));
  }
}
