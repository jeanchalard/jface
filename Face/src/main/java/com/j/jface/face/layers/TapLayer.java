package com.j.jface.face.layers;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import com.j.jface.Const;
import com.j.jface.face.models.TapModel;

import androidx.annotation.NonNull;

public class TapLayer
{
  private static final float BASE_ALPHA = 0.3f;
  private static final int FADEIN_DELAY_MS = 300;
  private static final int FADEOUT_DELAY_MS = 500;
  @NonNull private final Interpolator fadeInInterpolator = new DecelerateInterpolator();
  @NonNull private final Interpolator fadeOutInterpolator = new AccelerateInterpolator();
  @NonNull private final Paint greenPaint = makeFillPaint(0xFF7FFF7F);
  @NonNull private final Paint redPaint = makeFillPaint(0xFFFF7F7F);
  @NonNull private final Paint bluePaint = makeFillPaint(0xFF7F7FFF);
  @NonNull private final Paint yellowPaint = makeFillPaint(0xFFFFFF7F);

  private static Paint makeFillPaint(final int color)
  {
    final Paint p = new Paint();
    p.setColor(color);
    p.setStyle(Paint.Style.FILL);
    return p;
  }

  @NonNull private final TapModel tapModel;
  public TapLayer(@NonNull final TapModel tapModel)
  {
    this.tapModel = tapModel;
  }

  public void draw(@NonNull final Canvas canvas)
  {
    if (tapModel.toInactive() < 0) return;
    float alpha;
    float proportionSinceActive = ((float)tapModel.sinceActive()) / FADEIN_DELAY_MS;
    float proportionToInactive = ((float)tapModel.toInactive()) / FADEOUT_DELAY_MS;
    if (proportionSinceActive >= 0 && proportionSinceActive <= 1.0f)
      alpha = fadeInInterpolator.getInterpolation(proportionSinceActive);
    else if (proportionToInactive >= 0 && proportionToInactive <= 1.0f)
      alpha = fadeOutInterpolator.getInterpolation(proportionToInactive);
    else
      alpha = 1.0f;

    alpha *= BASE_ALPHA;
    final int intAlpha = (int)(255 * alpha);

    greenPaint.setAlpha(intAlpha);
    redPaint.setAlpha(intAlpha);
    bluePaint.setAlpha(intAlpha);
    yellowPaint.setAlpha(intAlpha);
    canvas.drawArc(15, 10, Const.SCREEN_SIZE - 5, Const.SCREEN_SIZE - 10, 315, 90, true, greenPaint);
    canvas.drawArc(10, 5, Const.SCREEN_SIZE - 10, Const.SCREEN_SIZE - 20, 225, 90, true, redPaint);
    canvas.drawArc(5, 10, Const.SCREEN_SIZE - 20, Const.SCREEN_SIZE - 10, 135, 90, true, bluePaint);
    canvas.drawArc(10, 15, Const.SCREEN_SIZE - 10, Const.SCREEN_SIZE - 5, 45, 90, true, yellowPaint);
  }
}
