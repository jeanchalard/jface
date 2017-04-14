package com.j.jface.client.action.drive;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.drive.query.SortOrder;
import com.google.android.gms.drive.query.SortableField;
import com.j.jface.client.Client;
import com.j.jface.client.action.Action;
import com.j.jface.client.action.ResultAction;
import com.j.jface.client.action.drive.ResolveDriveResourceAction.ResolveFilesAction;
import com.j.jface.client.action.drive.ResolveDriveResourceAction.ResolveFirstFileAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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
  private static final int ONEDAY_MILLIS = 86_400 * 1_000;

  @NonNull final List<ResultAction> mPreconditions;
  @NonNull final ResolveOrCreateDriveFolderAction mSavesFolder;
  @NonNull final ResolveOrCreateDriveFolderAction mHistoryFolder;
  @NonNull final ResolveOrCreateDriveFolderAction mArchiveFolder;
  @NonNull final ResolveFirstFileAction mMostRecentArchive;
  @NonNull final ResolveFilesAction mHistoryFiles;
  @NonNull final ResolveFirstFileAction mPreviousRun;
  @NonNull final ResolveFirstFileAction mLastRun;
  @NonNull final ResolveFirstFileAction mLatest;

  public RecursiveBackupAction(@NonNull final Client client, @Nullable final Action then)
  {
    super(client, then);
    mSavesFolder = new ResolveOrCreateDriveFolderAction(client, this, "Jormungand/Saves");
    mHistoryFolder = new ResolveOrCreateDriveFolderAction(client, this, mSavesFolder, "History");
    mArchiveFolder = new ResolveOrCreateDriveFolderAction(client, this, mSavesFolder, "Archive");

    mMostRecentArchive = new ResolveFirstFileAction(client, this, mArchiveFolder, null, new SortOrder.Builder().addSortDescending(SortableField.CREATED_DATE).build());
    mHistoryFiles = new ResolveFilesAction(client, this, mHistoryFolder, Filters.contains(SearchableField.TITLE, "save"), new SortOrder.Builder().addSortAscending(SortableField.TITLE).build());
    mPreviousRun = new ResolveFirstFileAction(client, this, mHistoryFolder, Filters.eq(SearchableField.TITLE, "previousRun"));
    mLastRun = new ResolveFirstFileAction(client, this, mHistoryFolder, Filters.eq(SearchableField.TITLE, "lastRun"));
    mLatest = new ResolveFirstFileAction(client, this, mSavesFolder, Filters.eq(SearchableField.TITLE, "latest"));

    mPreconditions = Arrays.asList(new ResultAction[] { mSavesFolder, mHistoryFolder, mArchiveFolder, mMostRecentArchive, mHistoryFiles, mPreviousRun, mLastRun, mLatest });
  }

  @Override public void run(@NonNull final GoogleApiClient gClient)
  {
    for (final ResultAction r : mPreconditions)
      switch (r.status())
      {
        case FAILURE :
          // TODO : display something to the user.
          return;
        case NOT_DONE :
          r.enqueue();
          return;
        case SUCCESS :
          // Awesome ! Continue.
      }
    // When the control comes here all preconditions are resolved.

    final Metadata mostRecent = mMostRecentArchive.get();
    final ArrayList<Metadata> historyFiles = mHistoryFiles.get();

    if (null != mostRecent && isAtLeastThisOld(mostRecent.getCreatedDate(), 14 * ONEDAY_MILLIS))
      if (!historyFiles.isEmpty())
      {
        final Metadata lastFile = historyFiles.get(historyFiles.size() - 1);
        // rename(lastFile, datedName(lastFile));
      }

    if (!historyFiles.isEmpty())
    {
      final Metadata firstFile = historyFiles.get(0);
      if (isAtLeastThisOld(firstFile.getCreatedDate(), ONEDAY_MILLIS))
      {

      }
    }
  }

  private static boolean isAtLeastThisOld(@NonNull final Date date, final long milliseconds)
  {
    // Using the local device clock. It's fine, my device is always on time, and I don't care
    // too much if there is some drift and even the occasional failure by one or two days.
    return (System.currentTimeMillis() - date.getTime() >= milliseconds);
  }

  private static String datedName(@NonNull final Metadata file)
  {
    final Date date = file.getCreatedDate();
    final GregorianCalendar calendar = new GregorianCalendar();
    calendar.setTime(date);
    return String.format(Locale.JAPANESE, "%04d%02d%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
  }
}
