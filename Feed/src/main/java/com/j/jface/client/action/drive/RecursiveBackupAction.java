package com.j.jface.client.action.drive;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.MetadataBufferResult;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveFolder.DriveFolderResult;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.drive.query.SortOrder;
import com.google.android.gms.drive.query.SortableField;
import com.j.jface.client.Client;
import com.j.jface.client.action.Action;

import static com.j.jface.Future.FAILURE;
import static com.j.jface.Future.NOT_DONE;

/**
 * An action implementing the recursive checks and copies of backups.
 *
 * This action will yield periodically (pretty often) so as to let other actions proceed.
 *
 * Copies always :
 * Saves/latest → Saves/history/lastRun
 * Saves/history/lastRun → Saves/history/previousRun
 * Then :
 * Saves/history/previousRun → Saves/history/save00 if save00 is older than a day, otherwise stop here.
 * Saves/history/save{n} → Saves/history/save{n+1} where n is an int between 00 and 13.
 * Then :
 * If the last backup in Saves/archive/ is more than 14 days old,
 * Saves/history/save14 → Saves/archive/{year}{month}{day}.
 */

public class RecursiveBackupAction extends Action
{
  private static final int ONEDAY = 86400;

  @NonNull final ResolveDriveFolder mSavesFolder;
  @NonNull final ResolveDriveFolder mHistoryFolder;
  @NonNull final ResolveDriveFolder mArchiveFolder;

  public RecursiveBackupAction(@NonNull final Client client, @Nullable final Action then)
  {
    super(client, then);
    mSavesFolder = new ResolveDriveFolder(client, this, "Jormungand/Saves");
    mHistoryFolder = new ResolveDriveFolder(client, this, mSavesFolder, "History");
    mArchiveFolder = new ResolveDriveFolder(client, this, mSavesFolder, "Archive");
  }

  @Override public void run(@NonNull final GoogleApiClient gClient)
  {
    if (FAILURE == mSavesFolder.status() || FAILURE == mHistoryFolder.status() || FAILURE == mArchiveFolder.status())
    {
      // TODO : display something to the user.
      return;
    }
    if (NOT_DONE == mSavesFolder.status()) { mSavesFolder.enqueue(); return; }
    if (NOT_DONE == mHistoryFolder.status()) { mHistoryFolder.enqueue(); return; }
    if (NOT_DONE == mArchiveFolder.status()) { mArchiveFolder.enqueue(); return; }
  }
}
