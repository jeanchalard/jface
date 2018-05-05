package com.j.jface.face;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.j.jface.Const;
import com.j.jface.R;

public class DrawTools
{
  private static final Typeface BOLD_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
  private static final Typeface NORMAL_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

  public final float timePosY;
  public final float departurePosY;
  public final float iconToDepartureXPadding;

  public final Paint imagePaint;
  public final Paint minutesPaint;
  public final Paint secondsPaint;
  public final Paint departurePaint;
  public final Paint statusPaint;
  public final Paint userMessagePaint;

  public final Path watchContourPath;

  private final Bitmap hibiyaIcon;
  private final Bitmap keiseiIcon;
  private final Bitmap keiōIcon;
  private final Bitmap mitaIcon;
  private final Bitmap ōedoIcon;

  public DrawTools(@Nullable final Resources resources) {
    imagePaint = new Paint();
    imagePaint.setColor(0xFF000000);
    minutesPaint = createTextPaint(0xFFFFFFFF, NORMAL_TYPEFACE);
    secondsPaint = createTextPaint(0xFF888888, NORMAL_TYPEFACE);
    departurePaint = createTextPaint(0xFFCCCCCC, NORMAL_TYPEFACE);
    statusPaint = createTextPaint(0xFFCCCCCC, NORMAL_TYPEFACE);
    statusPaint.setTextAlign(Paint.Align.CENTER);
    departurePaint.setTextAlign(Paint.Align.CENTER);
    userMessagePaint = createTextPaint(Const.USER_MESSAGE_DEFAULT_COLOR, NORMAL_TYPEFACE);
    userMessagePaint.setTextAlign(Paint.Align.CENTER);

    final int statusFontSize = 18;
    final int contourPadding = 2;
    watchContourPath = new Path();
    watchContourPath.addArc(statusFontSize + contourPadding, statusFontSize + contourPadding, // top, left
     Const.SCREEN_SIZE - statusFontSize - contourPadding, Const.SCREEN_SIZE - statusFontSize - contourPadding, // bottom, right
     -269, 358); // startAngle, sweepAngle

    if (null != resources) {
      hibiyaIcon = ((BitmapDrawable)resources.getDrawable(R.drawable.hibiya)).getBitmap();
      keiseiIcon = ((BitmapDrawable)resources.getDrawable(R.drawable.keisei)).getBitmap();
      keiōIcon = ((BitmapDrawable)resources.getDrawable(R.drawable.keiou)).getBitmap();
      mitaIcon = ((BitmapDrawable)resources.getDrawable(R.drawable.mita)).getBitmap();
      ōedoIcon = ((BitmapDrawable)resources.getDrawable(R.drawable.ooedo)).getBitmap();
    } else {
      hibiyaIcon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
      keiseiIcon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
      keiōIcon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
      mitaIcon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
      ōedoIcon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }

    // We can afford to do this because we know what device we are running on, but otherwise we should
    // read this stuff from resources instead. All the values below are in pixels.
    minutesPaint.setTextSize(Const.SCREEN_SIZE > 300 ? 54 : 48);
    final int timeSize = Const.SCREEN_SIZE > 300 ? 23 : 20;
    secondsPaint.setTextSize(timeSize);
    departurePaint.setTextSize(timeSize);
    statusPaint.setTextSize(statusFontSize);
    userMessagePaint.setTextSize(28);

    timePosY = Const.ROUND_SCREEN ? 85 : 75;
    departurePosY = timePosY + 36;
    iconToDepartureXPadding = 7;
  }

  @Nullable public Bitmap getIconForKey(final String key) {
    switch (key) {
      case Const.日比谷線_北千住_平日:
      case Const.日比谷線_北千住_休日:
      case Const.日比谷線_六本木_平日:
      case Const.日比谷線_六本木_休日:
        return hibiyaIcon;
      case Const.京成線_千住大橋_上野方面_平日:
      case Const.京成線_千住大橋_上野方面_休日:
      case Const.京成線_千住大橋_成田方面_平日:
      case Const.京成線_千住大橋_成田方面_休日:
      case Const.京成線_日暮里_千住大橋方面_平日:
      case Const.京成線_日暮里_千住大橋方面_休日:
        return keiseiIcon;
      case Const.京王線_稲城駅_新宿方面_平日:
      case Const.京王線_稲城駅_新宿方面_休日:
        return keiōIcon;
      case Const.都営三田線_本蓮沼_目黒方面_平日:
      case Const.都営三田線_本蓮沼_目黒方面_休日:
        return mitaIcon;
      case Const.大江戸線_六本木_新宿方面_平日:
        return ōedoIcon;
      default:
        return null;
    }
  }

  @NonNull private Paint createTextPaint(final int defaultInteractiveColor, final Typeface typeface)
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
