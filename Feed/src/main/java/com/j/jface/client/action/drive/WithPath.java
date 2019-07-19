package com.j.jface.client.action.drive;

import androidx.annotation.NonNull;

public abstract class WithPath
{
  @NonNull public final String path;
  public WithPath(@NonNull final String path) { this.path = path; }
  public static class DriveFolder extends WithPath
  {
    @NonNull public final com.google.android.gms.drive.DriveFolder folder;
    public DriveFolder(@NonNull final com.google.android.gms.drive.DriveFolder folder, @NonNull final String path) { super(path); this.folder = folder; }
  }
  public static class Metadata extends WithPath
  {
    @NonNull public final com.google.android.gms.drive.Metadata metadata;
    public Metadata(@NonNull final com.google.android.gms.drive.Metadata metadata, @NonNull final String path) { super(path); this.metadata = metadata; }
  }
}
