package com.j.jface.face;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.NonNull;

public class DrawTools
{
  private static final Typeface BOLD_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
  private static final Typeface NORMAL_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

  public final Paint imagePaint;
  public final Paint minutesPaint;
  public final Paint secondsPaint;
  public final Paint departurePaint;
  public final Paint statusPaint;

  public DrawTools() {
    imagePaint = new Paint();
    imagePaint.setColor(0xFF000000);
    minutesPaint = createTextPaint(0xFFFFFFFF, NORMAL_TYPEFACE);
    secondsPaint = createTextPaint(0xFF888888, NORMAL_TYPEFACE);
    departurePaint = createTextPaint(0xFFCCCCCC, NORMAL_TYPEFACE);
    statusPaint = createTextPaint(0xFFCCCCCC, NORMAL_TYPEFACE);

    minutesPaint.setTextSize(38);
    secondsPaint.setTextSize(23);
    departurePaint.setTextSize(23);
  }

  @NonNull
  private Paint createTextPaint(final int defaultInteractiveColor, final Typeface typeface)
  {
    Paint paint = new Paint();
    paint.setColor(defaultInteractiveColor);
    paint.setTypeface(typeface);
    paint.setAntiAlias(true);
    return paint;
  }

  public void onPropertiesChanged(final boolean burnInProtection, final boolean lowBitAmbient)
  {
    minutesPaint.setTypeface(burnInProtection ? NORMAL_TYPEFACE : BOLD_TYPEFACE);
  }

  public void onAmbientModeChanged(final boolean inAmbientMode, final boolean lowBitAmbient)
  {
    minutesPaint.setAntiAlias(lowBitAmbient ? !inAmbientMode : true);
  }
}
