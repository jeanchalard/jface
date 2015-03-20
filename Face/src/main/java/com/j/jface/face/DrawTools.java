package com.j.jface.face;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import com.j.jface.Const;
import com.j.jface.R;

public class DrawTools
{
  private static final Typeface BOLD_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
  private static final Typeface NORMAL_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

  public final float timePosY;
  public final float departurePosX;
  public final float departurePosY;
  public final float iconToDepartureXPadding;

  public final Paint imagePaint;
  public final Paint minutesPaint;
  public final Paint secondsPaint;
  public final Paint departurePaint;
  public final Paint statusPaint;

  public final Path watchContourPath;

  public final Drawable background;
  public final Bitmap hibiyaIcon;
  public final Bitmap keiseiIcon;

  public DrawTools(final Resources resources) {
    imagePaint = new Paint();
    imagePaint.setColor(0xFF000000);
    minutesPaint = createTextPaint(0xFFFFFFFF, NORMAL_TYPEFACE);
    secondsPaint = createTextPaint(0xFF888888, NORMAL_TYPEFACE);
    departurePaint = createTextPaint(0xFFCCCCCC, NORMAL_TYPEFACE);
    statusPaint = createTextPaint(0xFFCCCCCC, NORMAL_TYPEFACE);
    statusPaint.setTextAlign(Paint.Align.CENTER);

    final int statusFontSize = 18;
    final int contourPadding = 2;
    watchContourPath = new Path();
    watchContourPath.addArc(statusFontSize + contourPadding, statusFontSize + contourPadding, // top, left
     Const.SCREEN_SIZE - statusFontSize - contourPadding, Const.SCREEN_SIZE - statusFontSize - contourPadding, // bottom, right
     -269, 358); // startAngle, sweepAngle

    if (null != resources) {
      background = resources.getDrawable(R.drawable.bg);
      background.setBounds(0, 0, Const.SCREEN_SIZE, Const.SCREEN_SIZE);
      hibiyaIcon = ((BitmapDrawable)resources.getDrawable(R.drawable.hibiya)).getBitmap();
      keiseiIcon = ((BitmapDrawable)resources.getDrawable(R.drawable.keisei)).getBitmap();
    } else {
      background = new ColorDrawable();
      hibiyaIcon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
      keiseiIcon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }

    // We can afford to do this because we know what device we are running on, but otherwise we should
    // read this stuff from resources instead. All the values below are in pixels.
    minutesPaint.setTextSize(38);
    secondsPaint.setTextSize(23);
    departurePaint.setTextSize(23);
    statusPaint.setTextSize(statusFontSize);

    timePosY = 75;
    departurePosX = 82;
    departurePosY = 120;
    iconToDepartureXPadding = 7;
  }

  public Bitmap getIconForStatus(final Status status) {
    switch (status) {
      case COMMUTE_MORNING_平日:
      case COMMUTE_EVENING_休日:
        return hibiyaIcon;
      case HOME_平日:
      case HOME_休日:
        return keiseiIcon;
      default:
        return null;
    }
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
