package com.j.jface.client.action.drive;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveApi.MetadataBufferResult;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.j.jface.Util;
import com.j.jface.client.Client;
import com.j.jface.client.action.Action;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An action that writes a file to Drive with the given data.
 */
public class WriteFileAction implements Action
{
  @NonNull private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";

  @NonNull final String mFilepath;
  @NonNull final InputStream mSource;
  public WriteFileAction(@NonNull final String filepath, @NonNull final InputStream source)
  {
    mSource = source;
    mFilepath = filepath;
  }

  @NonNull private DriveId findOrCreateFolder(@NonNull final GoogleApiClient gClient, @NonNull final DriveId parent, @NonNull final String name)
  {
    final Query q = new Query.Builder()
     .addFilter(Filters.eq(SearchableField.TITLE, name))
     .addFilter(Filters.in(SearchableField.PARENTS, parent))
     .addFilter(Filters.eq(SearchableField.MIME_TYPE, FOLDER_MIME_TYPE))
     .addFilter(Filters.eq(SearchableField.TRASHED, false))
     .build();
    final MetadataBufferResult result = Drive.DriveApi.query(gClient, q).await();
    if (result.getStatus().isSuccess())
      for (final Metadata m : result.getMetadataBuffer())
        if (m.isFolder()) return m.getDriveId();
    final MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(name).build();
    return parent.asDriveFolder().createFolder(gClient, changeSet).await().getDriveFolder().getDriveId();
  }

  @Nullable private DriveContents findDriveContentsOrReturnNull(@NonNull final GoogleApiClient gClient, @NonNull final DriveId parent, @NonNull final String name)
  {
    final Query q = new Query.Builder()
     .addFilter(Filters.eq(SearchableField.TITLE, name))
     .addFilter(Filters.in(SearchableField.PARENTS, parent))
     .build();
    final MetadataBufferResult result = Drive.DriveApi.query(gClient, q).await();
    if (result.getStatus().isSuccess())
      for (final Metadata m : result.getMetadataBuffer())
      {
        final DriveContentsResult r = m.getDriveId().asDriveFile().open(gClient, DriveFile.MODE_WRITE_ONLY, null).await();
        if (r.getStatus().isSuccess()) return r.getDriveContents();
      }
    return null;
  }

  @Override public void run(@NonNull final GoogleApiClient gClient)
  {
    final ArrayList<String> folderNamesToResolve = new ArrayList<>(Arrays.asList(mFilepath.split("/")));
    final String filename = folderNamesToResolve.remove(folderNamesToResolve.size() - 1);
    final ArrayList<DriveId> folderIds = new ArrayList<>(folderNamesToResolve.size() + 1);
    folderIds.add(Drive.DriveApi.getRootFolder(gClient).getDriveId());

    // Find or create folder hierarchy
    while (!folderNamesToResolve.isEmpty())
    {
      final String folderName = folderNamesToResolve.remove(0);
      final DriveId id = findOrCreateFolder(gClient, folderIds.get(folderIds.size() - 1), folderName);
      folderIds.add(id);
    }

    final DriveContents contents;
    final boolean isNewFile;
    final DriveContents existingContents = findDriveContentsOrReturnNull(gClient, folderIds.get(folderIds.size() - 1), filename);
    if (null != existingContents)
    {
      contents = existingContents;
      isNewFile = false;
    }
    else
    {
      contents = Drive.DriveApi.newDriveContents(gClient).await().getDriveContents();
      isNewFile = true;
    }
    final OutputStream out = contents.getOutputStream();
    try
    {
      Util.copy(mSource, out);
    }
    catch (IOException e)
    {
      Log.e("Jorg", "Can't write file : " + e);
    }
    final Status status;
    if (isNewFile)
    {
      final MetadataChangeSet newFileChanges = new MetadataChangeSet.Builder().setTitle(filename).build();
      status = folderIds.get(folderIds.size() - 1).asDriveFolder().createFile(gClient, newFileChanges, contents).await().getStatus();
    }
    else
      status = contents.commit(gClient, null).await();
    if (!status.isSuccess())
      Log.e("Jorg", "Save unsuccessful : " + status.getStatusMessage());
  }
}
