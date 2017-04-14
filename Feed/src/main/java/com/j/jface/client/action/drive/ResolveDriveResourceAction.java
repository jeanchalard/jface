package com.j.jface.client.action.drive;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.query.Filter;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.drive.query.SortOrder;
import com.j.jface.client.Client;
import com.j.jface.client.action.Action;
import com.j.jface.client.action.ResultAction;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * An action taking a Filter and returning the first Drive object matching it.
 */
public abstract class ResolveDriveResourceAction<T> extends ResultAction<T> implements ResultCallback<DriveApi.MetadataBufferResult>
{
  @Nullable final Filter mFilter;
  @Nullable final SortOrder mOrder;

  public ResolveDriveResourceAction(@NonNull final Client client, @Nullable final Action then, @Nullable final Filter filter)
  {
    this(client, then, filter, null);
  }
  public ResolveDriveResourceAction(@NonNull final Client client, @Nullable final Action then, @Nullable final Filter filter, @Nullable final SortOrder order)
  {
    super(client, then);
    mFilter = filter;
    mOrder = order;
  }

  protected void startResolve(@NonNull final GoogleApiClient client, @NonNull final ResultAction<DriveFolder> parent)
  {
    if (FAILURE == parent.status()) { fail(parent.getError()); return; }

    if (NOT_DONE == parent.status())
    {
      parent.enqueue();
      return;
    }

    final DriveFolder parentFolder = parent.get();
    if (null == parentFolder)
    {
      fail("Can't find parent");
      return;
    }

    final Query.Builder builder = new Query.Builder()
     .addFilter(Filters.in(SearchableField.PARENTS, parentFolder.getDriveId()))
     .addFilter(Filters.eq(SearchableField.TRASHED, false));
    if (null != mFilter) builder.addFilter(mFilter);
    if (null != mOrder) builder.setSortOrder(mOrder);
    Drive.DriveApi.query(client, builder.build()).setResultCallback(this);
  }

  @Override public void onResult(@NonNull final DriveApi.MetadataBufferResult result)
  {
    if (!result.getStatus().isSuccess())
    {
      final String error = result.getStatus().getStatusMessage();
      if (null == error) fail("ResolveDriveResourceAction : Unknown error : " + result);
      else fail(error);
      return;
    }

    final MetadataBuffer mbr = result.getMetadataBuffer();
    if (null == mbr) { fail("Successfully returned MetadataBuffer is null."); return; }
    finish(resultFromMetadataBuffer(mbr));
  }

  protected abstract @Nullable T resultFromMetadataBuffer(@NonNull final MetadataBuffer mbr);

  public static class ResolveFirstFolderAction extends ResolveDriveResourceAction<DriveFolder>
  {
    @NonNull private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
    @Nullable final ResultAction<DriveFolder> mParent;

    public ResolveFirstFolderAction(@NonNull final Client client, @Nullable final Action then, @Nullable ResultAction<DriveFolder> parent, @Nullable final Filter filter)
    { this(client, then, parent, filter, null); }
    public ResolveFirstFolderAction(@NonNull final Client client, @Nullable final Action then, @Nullable ResultAction<DriveFolder> parent, @Nullable final Filter filter, @Nullable final SortOrder order)
    {
      super(client, then, null == filter ? Filters.eq(SearchableField.MIME_TYPE, FOLDER_MIME_TYPE) : Filters.and(filter, Filters.eq(SearchableField.MIME_TYPE, FOLDER_MIME_TYPE)), order);
      mParent = parent;
    }

    @Override public void run(@NonNull final GoogleApiClient client)
    {
      if (null == mParent) { finish(Drive.DriveApi.getRootFolder(client)); return; }
      startResolve(client, mParent);
    }

    protected @Nullable DriveFolder resultFromMetadataBuffer(@NonNull final MetadataBuffer mbr)
    {
      for (final Metadata m : mbr) return m.getDriveId().asDriveFolder();
      return null;
    }

    public static ResolveFirstFolderAction root(@NonNull final Client client, @Nullable final Action then) { return new ResolveFirstFolderAction(client, then, null, null, null); }
  }

  public static class ResolveFirstFileAction extends ResolveDriveResourceAction<Metadata>
  {
    @NonNull final ResultAction<DriveFolder> mParent;

    public ResolveFirstFileAction(@NonNull final Client client, @Nullable final Action then, @NonNull ResultAction<DriveFolder> parent, @Nullable final Filter filter)
    { this(client, then, parent, filter, null); }
    public ResolveFirstFileAction(@NonNull final Client client, @Nullable final Action then, @NonNull ResultAction<DriveFolder> parent, @Nullable final Filter filter, @Nullable final SortOrder order)
    {
      super(client, then, filter, order);
      mParent = parent;
    }

    @Override public void run(@NonNull final GoogleApiClient client)
    {
      startResolve(client, mParent);
    }

    protected @Nullable Metadata resultFromMetadataBuffer(@NonNull final MetadataBuffer mbr)
    {
      for (final Metadata m : mbr) return m;
      return null;
    }
  }

  public static class ResolveFilesAction extends ResolveDriveResourceAction<ArrayList<Metadata>>
  {
    @NonNull final ResultAction<DriveFolder> mParent;

    public ResolveFilesAction(@NonNull final Client client, @Nullable final Action then, @NonNull ResultAction<DriveFolder> parent, @Nullable final Filter filter)
    { this(client, then, parent, filter, null); }
    public ResolveFilesAction(@NonNull final Client client, @Nullable final Action then, @NonNull ResultAction<DriveFolder> parent, @Nullable final Filter filter, @Nullable final SortOrder order)
    {
      super(client, then, filter, order);
      mParent = parent;
    }

    @Override public void run(@NonNull final GoogleApiClient client)
    {
      startResolve(client, mParent);
    }

    protected @NonNull ArrayList<Metadata> resultFromMetadataBuffer(@NonNull final MetadataBuffer mbr)
    {
      final ArrayList<Metadata> results = new ArrayList<>();
      for (final Metadata m : mbr) results.add(m);
      return results;
    }

    // Make get() non-null for this class. This class never calls finish() with null, but get() called after fail() might return null.
    // Nobody is supposed to call get() after status is failure, but this is safer (and more convenient for callers which can trust it's nonnull).
    @Override @NonNull public ArrayList<Metadata> get()
    {
      final ArrayList<Metadata> m = super.get();
      return null == m ? new ArrayList<Metadata>() : m;
    }
  }
}
