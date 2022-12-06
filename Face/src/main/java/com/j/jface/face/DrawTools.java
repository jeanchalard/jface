package com.j.jface.face;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;

import com.j.jface.Const;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DrawTools
{
  private static final int WEIGHT_NORMAL = 400;
  private static final int WEIGHT_MEDIUM = 500;
  private static final int WEIGHT_SEMI_BOLD = 600;
  // Requires API 28 : test if it works with my new watch
  //  private static final Typeface BOLD_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, WEIGHT_SEMI_BOLD, false /* italic */);
  //  private static final Typeface NORMAL_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, WEIGHT_NORMAL, false /* italic */);
  private static final Typeface BOLD_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
  private static final Typeface NORMAL_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

  public final float timePosY;
  public final float departurePosY;
  public final float iconToDepartureXPadding;
  public final float departureLinesAdditionalPadding;

  public final Paint imagePaint;
  public final Paint minutesPaint;
  public final Paint secondsPaint;
  public final Paint departurePaint;
  public final Paint statusPaint;
  public final Paint userMessagePaint;
  public final Paint checkpointPaint;

  public final Path watchContourPath;

  private final Bitmap jrIcon;
  private final Bitmap hibiyaIcon;
  private final Bitmap keiseiIcon;
  private final Bitmap keiōIcon;
  private final Bitmap mitaIcon;
  private final Bitmap ōedoIcon;

  public DrawTools(@Nullable final Resources resources) {
    imagePaint = new Paint();
    imagePaint.setColor(0xFF000000);
    minutesPaint = createTextPaint(0xFFFFFFFF, BOLD_TYPEFACE);
    secondsPaint = createTextPaint(0xFFC7C7C7, BOLD_TYPEFACE);
    departurePaint = createTextPaint(0xFFCCCCCC, NORMAL_TYPEFACE);
    statusPaint = createTextPaint(0xFFCCCCCC, NORMAL_TYPEFACE);
    statusPaint.setTextAlign(Paint.Align.CENTER);
    departurePaint.setTextAlign(Paint.Align.CENTER);
    userMessagePaint = createTextPaint(Const.USER_MESSAGE_DEFAULT_COLOR, NORMAL_TYPEFACE);
    userMessagePaint.setTextAlign(Paint.Align.CENTER);
    checkpointPaint = createTextPaint(0xFFFFFFFF, BOLD_TYPEFACE);
    checkpointPaint.setTextAlign(Paint.Align.CENTER);

    // We can afford to do this because we know what device we are running on, but otherwise we should
    // read this stuff from resources instead. All the values below are in pixels.
    final int timeSize = Const.SCREEN_SIZE > 300 ? 84 : 48;
    final int departureSize = Const.SCREEN_SIZE > 300 ? 32 : 20;

    final int statusFontSize = 28;
    final int contourPadding = 2;
    watchContourPath = new Path();
    watchContourPath.addArc(statusFontSize + contourPadding, statusFontSize + contourPadding, // top, left
     Const.SCREEN_SIZE - statusFontSize - contourPadding, Const.SCREEN_SIZE - statusFontSize - contourPadding, // bottom, right
     -269, 358); // startAngle, sweepAngle

    if (null != resources) {
      final int iconSize = departureSize;
      jrIcon = Bitmap.createScaledBitmap(((BitmapDrawable)resources.getDrawable(R.drawable.jr)).getBitmap(), iconSize, iconSize, true /* filter */);
      hibiyaIcon = Bitmap.createScaledBitmap(((BitmapDrawable)resources.getDrawable(R.drawable.hibiya)).getBitmap(), iconSize, iconSize, true /* filter */);
      keiseiIcon = Bitmap.createScaledBitmap(((BitmapDrawable)resources.getDrawable(R.drawable.keisei)).getBitmap(), iconSize, iconSize, true /* filter */);
      keiōIcon = Bitmap.createScaledBitmap(((BitmapDrawable)resources.getDrawable(R.drawable.keiou)).getBitmap(), iconSize, iconSize, true /* filter */);
      mitaIcon = Bitmap.createScaledBitmap(((BitmapDrawable)resources.getDrawable(R.drawable.mita)).getBitmap(), iconSize, iconSize, true /* filter */);
      ōedoIcon = Bitmap.createScaledBitmap(((BitmapDrawable)resources.getDrawable(R.drawable.ooedo)).getBitmap(), iconSize, iconSize, true /* filter */);
    } else {
      jrIcon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
      hibiyaIcon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
      keiseiIcon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
      keiōIcon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
      mitaIcon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
      ōedoIcon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }

    minutesPaint.setTextSize(timeSize);
    secondsPaint.setTextSize(departureSize);
    departurePaint.setTextSize(departureSize);
    statusPaint.setTextSize(statusFontSize);
    userMessagePaint.setTextSize(36);
    checkpointPaint.setTextSize(72);

    timePosY = Const.ROUND_SCREEN ? 135 : 75;
    departurePosY = timePosY + 60;
    iconToDepartureXPadding = 7;
    departureLinesAdditionalPadding = Const.SCREEN_SIZE > 400 ? 6 : 0;
  }

  @Nullable public Bitmap getIconForKey(final String key) {
    switch (key) {
      case Const.山手線_日暮里_渋谷方面_平日:
      case Const.山手線_日暮里_渋谷方面_休日:
      case Const.山手線_渋谷_日暮里方面_平日:
      case Const.山手線_渋谷_日暮里方面_休日:
        return jrIcon;
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

  public void onAmbientModeChanged(final boolean inAmbientMode, final boolean lowBitAmbient)
  {
    final boolean antiAlias = lowBitAmbient ? !inAmbientMode : true;
    imagePaint.setAntiAlias(antiAlias);
    minutesPaint.setAntiAlias(antiAlias);
    secondsPaint.setAntiAlias(antiAlias);
    departurePaint.setAntiAlias(antiAlias);
    statusPaint.setAntiAlias(antiAlias);
    userMessagePaint.setAntiAlias(antiAlias);
  }
}
