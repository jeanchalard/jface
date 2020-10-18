package com.j.jface.face.layers;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;

import com.j.jface.Const;
import com.j.jface.face.DataStore;
import com.j.jface.face.R;
import com.j.jface.face.models.HeartModel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HeartLayer
{
  private static final int ZOOMIN_DELAY_MS = 300;
  private static final int FADEOUT_DELAY_MS = HeartModel.FADE_OUT_MS;

  private static final int TEXTSIZE = 40;
  private static final int TEXTMARGIN = 4;

  @Nullable private Bitmap cache = null;
  @NonNull private final Paint paint;
  @NonNull private final HeartModel model;
  @NonNull private final Resources res;
  @NonNull private final DataStore dataStore;
  @NonNull private final Interpolator zoomInInterpolator = new AccelerateDecelerateInterpolator();
  @NonNull private final Interpolator fadeOutInterpolator = new AccelerateInterpolator();

  public HeartLayer(@NonNull final HeartModel model, @NonNull final Resources res, @NonNull final DataStore dataStore)
  {
    this.model = model;
    this.res = res;
    this.dataStore = dataStore;
    this.paint = new Paint();
    paint.setColor(0xFFFFFFFF);
    paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
    paint.setAntiAlias(true);
    paint.setTextSize(TEXTSIZE);
  }

  private Bitmap getBitmap()
  {
    final Bitmap drawable = ((BitmapDrawable)res.getDrawable(R.drawable.heart)).getBitmap();
    final Bitmap heart = drawable.copy(drawable.getConfig(), true);

    final Canvas c = new Canvas(heart);
    float y = Const.SCREEN_SIZE / 2 - (TEXTSIZE + TEXTMARGIN / 2) * dataStore.mHeartMessage.length / 2;
    for (final String label : dataStore.mHeartMessage)
    {
      final float textWidth = paint.measureText(label);
      c.drawText(label, (heart.getWidth() - textWidth) / 2, y, paint);
      y += TEXTSIZE + TEXTMARGIN;
    }
    return heart;
  }

  @NonNull private final Rect src = new Rect();
  @NonNull private final Rect dst = new Rect();
  public void draw(@NonNull final Canvas canvas)
  {
    if (!model.isActive()) return;
    final Bitmap tmp = cache;
    final Bitmap heart;
    if (null == tmp)
    {
      heart = getBitmap();
      src.bottom = heart.getHeight();
      src.right = heart.getWidth();
      cache = heart;
    }
    else heart = tmp;

    final float zoom;
    if (model.sinceActive() < ZOOMIN_DELAY_MS)
      zoom = zoomInInterpolator.getInterpolation(((float)model.sinceActive()) / ZOOMIN_DELAY_MS);
    else
      zoom = 1.0f;
    final int size = (int)(zoom * heart.getWidth() / 2);
    final int center = Const.SCREEN_SIZE / 2;
    dst.bottom = center + size;
    dst.right = center + size;
    dst.left = center - size;
    dst.top = center - size;
    final float alpha;
    if (model.toInactive() < FADEOUT_DELAY_MS)
      alpha = fadeOutInterpolator.getInterpolation(((float)model.toInactive()) / FADEOUT_DELAY_MS);
    else
      alpha = 1.0f;
    paint.setAlpha((int)(255 * alpha));

    canvas.drawBitmap(heart, src, dst, paint);
    if (model.toInactive() >= FADEOUT_DELAY_MS)
    {
      cache.recycle();
      cache = null;
    }
  }
}
