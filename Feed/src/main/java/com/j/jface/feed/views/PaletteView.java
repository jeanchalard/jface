package com.j.jface.feed.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.util.ArrayList;

public class PaletteView extends androidx.appcompat.widget.AppCompatImageView
{
  @NonNull final private Bitmap mPaletteBitmap;
  @NonNull final private Matrix mImageMatrix;
  public interface OnColorSetListener { public void onColorSet(final int color); }
  public PaletteView(@NonNull final Context context) { this(context, null, 0); }
  public PaletteView(@NonNull final Context context, @Nullable final AttributeSet attrs) { this(context, attrs, 0); }
  public PaletteView(@NonNull final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr)
  {
    super(context, attrs, defStyleAttr);
    mPaletteBitmap = ((BitmapDrawable)getDrawable()).getBitmap();
    mImageMatrix = new Matrix();
    getImageMatrix().invert(mImageMatrix);
  }

  @NonNull final ArrayList<OnColorSetListener> mListeners = new ArrayList<>();
  public void addOnColorSetListener(@NonNull final OnColorSetListener listener) { mListeners.add(listener); }
  public void removeOnColorSetListener(@NonNull final OnColorSetListener listener) { mListeners.remove(listener); }

  @Override public boolean onTouchEvent(@NonNull final MotionEvent event)
  {
    final float[] pts = new float[] { event.getX(), event.getY() };
    mImageMatrix.mapPoints(pts);
    if (pts[0] < 0 || pts[1] < 0 || pts[0] >= mPaletteBitmap.getWidth() || pts[1] >= mPaletteBitmap.getHeight()) return true;
    final int color = mPaletteBitmap.getPixel((int)pts[0], (int)pts[1]);
    for (final OnColorSetListener listener : mListeners) listener.onColorSet(color);
    performClick();
    return true;
  }

  @Override public boolean performClick()
  {
    return super.performClick();
  }
}
