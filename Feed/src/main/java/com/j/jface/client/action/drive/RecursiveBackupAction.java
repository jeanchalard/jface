package com.j.jface.client.action.drive;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.drive.query.SortOrder;
import com.google.android.gms.drive.query.SortableField;
import com.j.jface.client.Client;
import com.j.jface.client.action.Action;
import com.j.jface.client.action.ResultAction;
import com.j.jface.client.action.drive.ResolveDriveResourcesAction.ResolveFirstFileAction;
import com.j.jface.client.action.ui.ReportActionWithSnackbar;

import java.io.File;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import static com.j.jface.Future.FAILURE;
import static com.j.jface.Future.NOT_DONE;
import static com.j.jface.Future.SUCCESS;

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
  private static class FileSpec
  {
    @NonNull public final String name;
    @NonNull public final WithPath.DriveFolder parent;
    private FileSpec(@NonNull final String name, @NonNull final WithPath.DriveFolder parent) { this.name = name; this.parent = parent; }
  }

  private static final int ONEDAY_MILLIS = 86_400 * 1_000;
  private static final String ITERATIVE_SAVE_TRUNK = "save";
  private static final int LAST_ITERATIVE_SAVE_NUMBER = 14;
  private static final String PREVIOUS_RUN_NAME = "previousRun";
  private static final String LAST_RUN_NAME = "lastRun";
  private static final String LATEST_NAME = "latest";

  @NonNull private final View mViewToSnackbarInto;
  @NonNull private final List<ResultAction> mPreconditions;
  @NonNull private final ReportActionWithSnackbar mReportStartAction;
  @NonNull private final ResolveOrCreateDriveFolderAction mSavesFolder;
  @NonNull private final ResolveOrCreateDriveFolderAction mHistoryFolder;
  @NonNull private final ResolveOrCreateDriveFolderAction mArchiveFolder;
  @NonNull private final ResolveFirstFileAction mMostRecentArchive;
  @NonNull private final ResolveDriveResourcesAction mHistoryFiles;
  @NonNull private final ResolveFirstFileAction mPreviousRun;
  @NonNull private final ResolveFirstFileAction mLastRun;
  @NonNull private final ResolveFirstFileAction mLatest;

  @Nullable private ArrayList<ResultAction> mFileOps;

  public RecursiveBackupAction(@NonNull final Client client, @Nullable final Action then, @NonNull final View viewToSnackbarInto)
  {
    super(client, then);
    mViewToSnackbarInto = viewToSnackbarInto;
    mReportStartAction = new ReportActionWithSnackbar(client, this, viewToSnackbarInto, "Starting backup");
    mSavesFolder = new ResolveOrCreateDriveFolderAction(client, this, "Jormungand/Saves");
    mHistoryFolder = new ResolveOrCreateDriveFolderAction(client, this, mSavesFolder, "History");
    mArchiveFolder = new ResolveOrCreateDriveFolderAction(client, this, mSavesFolder, "Archive");

    mMostRecentArchive = new ResolveFirstFileAction(client, this, mArchiveFolder, null, new SortOrder.Builder().addSortDescending(SortableField.CREATED_DATE).build());
    mHistoryFiles = new ResolveDriveResourcesAction(client, this, mHistoryFolder, Filters.contains(SearchableField.TITLE, ITERATIVE_SAVE_TRUNK), new SortOrder.Builder().addSortAscending(SortableField.TITLE).build());
    mPreviousRun = new ResolveFirstFileAction(client, this, mHistoryFolder, Filters.eq(SearchableField.TITLE, PREVIOUS_RUN_NAME));
    mLastRun = new ResolveFirstFileAction(client, this, mHistoryFolder, Filters.eq(SearchableField.TITLE, LAST_RUN_NAME));
    mLatest = new ResolveFirstFileAction(client, this, mSavesFolder, Filters.eq(SearchableField.TITLE, LATEST_NAME));

    mPreconditions = Arrays.asList(new ResultAction[] { mReportStartAction, mSavesFolder, mHistoryFolder, mArchiveFolder, mMostRecentArchive, mHistoryFiles, mPreviousRun, mLastRun, mLatest });

    mFileOps = null;
  }

  @Override public void run(@NonNull final GoogleApiClient gClient)
  {
    if (null != mFileOps && mFileOps.isEmpty()) return; // Not very clean, but it'll hold
    for (final ResultAction r : mPreconditions)
      switch (r.status())
      {
        case FAILURE :
          new ReportActionWithSnackbar(mClient, null, mViewToSnackbarInto, "Backup ended with error : " + r.getError()).enqueue();
          return;
        case NOT_DONE :
          r.enqueue();
          return;
        case SUCCESS :
          // Awesome ! Continue.
      }
    // When the control comes here all preconditions are resolved.

    if (null == mFileOps)
    {
      final WithPath.DriveFolder archiveFolder = mArchiveFolder.get();
      final WithPath.DriveFolder historyFolder = mHistoryFolder.get();
      if (null == archiveFolder || null == historyFolder) return; // This is not supposed to be possible ; get() on a ResolveOrCreate action only returns null if failure.
      final HashMap<WithPath.Metadata, FileSpec> renames = new HashMap<>();
      final HashMap<String, WithPath.Metadata> fileList = new HashMap<>();
      final WithPath.Metadata mostRecent = mMostRecentArchive.get();
      final WithPath.Metadata previousRun = mPreviousRun.get();
      final ArrayList<WithPath.Metadata> historyFiles = mHistoryFiles.get();

      if (null != mostRecent && isAtLeastThisOld(mostRecent.metadata.getCreatedDate(), 14 * ONEDAY_MILLIS))
        if (!historyFiles.isEmpty())
        {
          final WithPath.Metadata lastFile = historyFiles.get(historyFiles.size() - 1);
          renames.put(lastFile, new FileSpec(datedName(lastFile), archiveFolder));
        }

      if (!historyFiles.isEmpty())
      {
        final WithPath.Metadata firstFile = historyFiles.get(0);
        if (isAtLeastThisOld(firstFile.metadata.getCreatedDate(), ONEDAY_MILLIS))
        {
          for (final WithPath.Metadata f : historyFiles)
          {
            final String name = f.metadata.getTitle();
            if (!ITERATIVE_SAVE_TRUNK.equals(name.substring(0, 4))) continue;
            final int num = Integer.parseInt(name.substring(4, 6));
            if (num < 0 || num >= LAST_ITERATIVE_SAVE_NUMBER) continue;
            renames.put(f, new FileSpec(String.format(Locale.JAPAN, "%s%02d", ITERATIVE_SAVE_TRUNK, num + 1), historyFolder));
          }
          if (null != previousRun)
            renames.put(previousRun, new FileSpec(ITERATIVE_SAVE_TRUNK + "00", historyFolder));
        }
      }

      final WithPath.Metadata lastRun = mLastRun.get();
      if (null != lastRun) renames.put(lastRun, new FileSpec(PREVIOUS_RUN_NAME, historyFolder));

      final WithPath.Metadata latest = mLatest.get();
      if (null != latest) renames.put(latest, new FileSpec(LAST_RUN_NAME, historyFolder));

      if (null != mostRecent) fileList.put(mostRecent.path, mostRecent);
      for (final WithPath.Metadata f : historyFiles) fileList.put(f.path, f);
      if (null != previousRun) fileList.put(previousRun.path, previousRun);
      if (null != lastRun) fileList.put(lastRun.path, lastRun);
      if (null != latest) fileList.put(latest.path, latest);

      HashSet<String> destinations = new HashSet<>();
      for (final FileSpec spec : renames.values()) destinations.add(spec.parent.path + File.separator + spec.name);

      // No Collection.partition method :(
      mFileOps = new ArrayList<>();
      for (final WithPath.Metadata f : renames.keySet())
        if (destinations.contains(f.path))
          mFileOps.add(new MoveFileAction(mClient, this, f, renames.get(f).name, renames.get(f).parent));
        else
          mFileOps.add(new CopyFileAction(mClient, this, f, renames.get(f).name, renames.get(f).parent));
      for (final WithPath.Metadata f : renames.keySet()) destinations.remove(f.path);
      for (final String path : destinations)
      {
        final WithPath.Metadata file = fileList.get(path);
        if (null != file)
          mFileOps.add(new TrashFileAction(mClient, this, file));
      }
      for (final Action a : mFileOps) a.enqueue();
    }
    else
    {
      for (final ResultAction a : mFileOps)
      {
        final int status = a.status();
        if (FAILURE == status)
        {
          mFileOps.clear();
          new ReportActionWithSnackbar(mClient, null, mViewToSnackbarInto, a.getError()).enqueue();
          finish();
        }
        else if (NOT_DONE == status) return;
      }
      new ReportActionWithSnackbar(mClient, null, mViewToSnackbarInto, "Backup ended successfully.").enqueue();
      finish();
    }
  }

  private static boolean isAtLeastThisOld(@NonNull final Date date, final long milliseconds)
  {
    // Using the local device clock. It's fine, my device is always on time, and I don't care
    // too much if there is some drift and even the occasional failure by one or two days.
    return (System.currentTimeMillis() - date.getTime() >= milliseconds);
  }

  private static String datedName(@NonNull final WithPath.Metadata file)
  {
    final ZonedDateTime date = file.metadata.getCreatedDate().toInstant().atZone(ZoneOffset.UTC);
    return String.format(Locale.JAPAN, "%04d%02d%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
  }
}
