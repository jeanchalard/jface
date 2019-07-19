package com.j.jface.client.action.drive;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * An action taking a Filter and returning the Drive objects matching it.
 */
public class ResolveDriveResourcesAction extends ResultAction<ArrayList<WithPath.Metadata>> implements ResultCallback<DriveApi.MetadataBufferResult>
{
  @NonNull final ResultAction<WithPath.DriveFolder> mParent;
  @Nullable final Filter mFilter;
  @Nullable final SortOrder mOrder;

  public ResolveDriveResourcesAction(@NonNull final Client client, @Nullable final Action then, @Nullable final ResultAction<WithPath.DriveFolder> parent, @Nullable final Filter filter, @Nullable final SortOrder order)
  {
    super(client, then);
    mParent = null == parent ? new ResolveRootFolder(client, this) : parent;
    mFilter = filter;
    mOrder = order;
  }

  public ResolveDriveResourcesAction(@NonNull final Client client, @NonNull final Action then, @NonNull final String path) { this(client, then, path.split(File.separator)); }
  public ResolveDriveResourcesAction(@NonNull final Client client, @NonNull final Action then, @NonNull final String path, @Nullable final SortOrder order) { this(client, then, path.split(File.separator), order); }
  public ResolveDriveResourcesAction(@NonNull final Client client, @NonNull final Action then, @NonNull final String[] path) { this(client, then, path, null); }
  public ResolveDriveResourcesAction(@NonNull final Client client, @NonNull final Action then, @NonNull final String[] path, @Nullable final SortOrder order)
  {
    super(client, then);
    final boolean isRoot = path.length < 1 || (path.length < 3 && TextUtils.isEmpty(path[0]));
    mParent = isRoot ? new ResolveRootFolder(client, this) : new ResolveFirstFolderAction(client, this, Arrays.copyOfRange(path, 0, path.length - 1), order);
    mFilter = path.length < 1 ? null : Filters.eq(SearchableField.TITLE, path[path.length - 1]);
    mOrder = order;
  }

  @Override public void run(@NonNull final GoogleApiClient client)
  {
    if (FAILURE == mParent.status()) { fail(mParent.getError()); return; }
    if (NOT_DONE == mParent.status()) { mParent.enqueue(); return; }

    final WithPath.DriveFolder parentFolder = mParent.get();
    if (null == parentFolder)
    {
      fail("Can't find parent");
      return;
    }

    final Query.Builder builder = new Query.Builder()
     .addFilter(Filters.in(SearchableField.PARENTS, parentFolder.folder.getDriveId()))
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

    final WithPath.DriveFolder parent = mParent.get();
    final ArrayList<WithPath.Metadata> results = new ArrayList<>();
    if (null == parent) { fail("Already resolved folder is null ?!"); return; } // Can't happen because it would have been detected earlier, in theory
    for (final Metadata m : mbr) results.add(new WithPath.Metadata(m, parent.path + File.separator + m.getTitle()));
    finish(results);
  }

  // Make get() non-null for this class. This class never calls finish() with null, but get() called after fail() might return null.
  // Nobody is supposed to call get() after status is failure, but this is safer (and more convenient for callers who thanks to this can trust it's nonnull).
  @Override @NonNull public ArrayList<WithPath.Metadata> get()
  {
    final ArrayList<WithPath.Metadata> m = super.get();
    return null == m ? new ArrayList<WithPath.Metadata>() : m;
  }

  public static abstract class ResolveFirstItemAction<T> extends ResultAction<T>
  {
    @NonNull private final ResolveDriveResourcesAction mInnerResolver;

    public ResolveFirstItemAction(@NonNull final Client client, @Nullable final Action then, @Nullable ResultAction<WithPath.DriveFolder> parent, @Nullable final Filter filter, @Nullable final SortOrder order)
    {
      super(client, then);
      mInnerResolver = new ResolveDriveResourcesAction(client, this, parent, filter, order);
    }
    public ResolveFirstItemAction(@NonNull final Client client, @Nullable final Action then, @NonNull final String[] strings, @Nullable final SortOrder order)
    {
      super(client, then);
      mInnerResolver = new ResolveDriveResourcesAction(client, this, strings, order);
    }

    @Override public void run(@NonNull final GoogleApiClient gClient)
    {
      if (FAILURE == mInnerResolver.status()) { fail(mInnerResolver.getError()); return; }
      if (NOT_DONE == mInnerResolver.status()) { mInnerResolver.enqueue(); return; }
      // Success
      final ArrayList<WithPath.Metadata> result = mInnerResolver.get();
      finish(result.isEmpty() ? null : resultFromMetadata(result.get(result.size() - 1)));
    }

    public abstract T resultFromMetadata(@NonNull final WithPath.Metadata m);
  }

  public static class ResolveFirstFileAction extends ResolveFirstItemAction<WithPath.Metadata>
  {
    public ResolveFirstFileAction(@NonNull Client client, @Nullable Action then, @Nullable ResultAction<WithPath.DriveFolder> parent, @Nullable Filter filter)
    { this(client, then, parent, filter, null); }
    public ResolveFirstFileAction(@NonNull Client client, @Nullable Action then, @Nullable ResultAction<WithPath.DriveFolder> parent, @Nullable Filter filter, @Nullable SortOrder order)
    { super(client, then, parent, filter, order); }
    public ResolveFirstFileAction(@NonNull final Client client, @Nullable final Action then, @NonNull final String path)
    { super(client, then, path.split(File.separator), null); }

    @Override public WithPath.Metadata resultFromMetadata(@NonNull WithPath.Metadata m) { return m; }
  }

  public static class ResolveFirstFolderAction extends ResolveFirstItemAction<WithPath.DriveFolder>
  {
    @NonNull private static final Filter FOLDER_FILTER = Filters.eq(SearchableField.MIME_TYPE, "application/vnd.google-apps.folder");

    public ResolveFirstFolderAction(@NonNull final Client client, @Nullable final Action then, @Nullable ResultAction<WithPath.DriveFolder> parent, @Nullable final Filter filter)
    { this(client, then, parent, filter, null); }
    public ResolveFirstFolderAction(@NonNull final Client client, @Nullable final Action then, @NonNull final String[] strings, @Nullable final SortOrder order)
    { super(client, then, strings, order); }

    public ResolveFirstFolderAction(@NonNull final Client client, @Nullable final Action then, @Nullable ResultAction<WithPath.DriveFolder> parent, @Nullable final Filter filter, @Nullable final SortOrder order)
    {
      super(client, then, parent, null == filter ? FOLDER_FILTER : Filters.and(filter, FOLDER_FILTER), order);
    }

    public WithPath.DriveFolder resultFromMetadata(@NonNull final WithPath.Metadata m)
    {
      return new WithPath.DriveFolder(m.metadata.getDriveId().asDriveFolder(), m.path);
    }
  }
}
