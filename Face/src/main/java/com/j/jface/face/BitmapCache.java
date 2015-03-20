package com.j.jface.face;

import android.graphics.Bitmap;

public class BitmapCache
{
  final Bitmap mCache;
  final int mDate;
  public BitmapCache() {
    mCache = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    mDate = -1;
  }
}
