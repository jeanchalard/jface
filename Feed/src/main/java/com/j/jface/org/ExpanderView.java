package com.j.jface.org;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.j.jface.R;

/**
 * A view for expanders with dashed lines, possibly a + or - sign in a square.
 * All of this is set through levels.
 */

public class ExpanderView extends View
{
  private static final int RIGHT_EXTEND = 20;
  private static final int DEPTH_INDENT = 30;
  private static final int SQUARE_VERTICAL_PADDING = 4;

  public static final int CONNECTIONS_NONE = 0;
  public static final int CONNECTIONS_UP = 1;
  public static final int CONNECTIONS_DOWN = 2;
  public static final int CONNECTIONS_BOTH = 3;
  private int mConnections; // Whether to draw the dashed lines up and down

  public static final int EXPANSIONS_NONE = 0; // A leaf.
  public static final int EXPANSIONS_CLOSED = 1; // Closed : a + in a box
  public static final int EXPANSIONS_OPEN = 2; // Open : a - in a box
  private int mExpansions;

  private int mDepth;

  @NonNull private final Paint mDashedPaint = new Paint();
  @NonNull private final Paint mSolidPaint = new Paint();
  @NonNull private final Path mPath = new Path();

  public void setConnections(final int connections) { mConnections = connections; invalidate(); }
  public void setExpansions(final int expansions) { mExpansions = expansions; invalidate(); }
  public void setDepth(final int depth) { mDepth = depth; invalidate(); requestLayout(); }

  public ExpanderView(@NonNull final  Context context)
  {
    this(context, null);
  }

  public ExpanderView(@NonNull final Context context, @Nullable final AttributeSet attrs)
  {
    super(context, attrs);
    mDashedPaint.setPathEffect(new DashPathEffect(new float[]{5, 5}, 0));
    final int c = context.getColor(R.color.color_primary);
    mDashedPaint.setColor(c);
    mDashedPaint.setStyle(Paint.Style.STROKE);
    mSolidPaint.setColor(c);
    mSolidPaint.setStyle(Paint.Style.STROKE);
  }

  @Override public void onDraw(@NonNull final Canvas canvas)
  {
    final int width = getWidth();
    final int height = getHeight();
    final int centerX = DEPTH_INDENT + mDepth * DEPTH_INDENT + SQUARE_VERTICAL_PADDING;
    final int centerY = height / 2;
    final int squareSize = oddMin(width - centerX - RIGHT_EXTEND, height - 2 * SQUARE_VERTICAL_PADDING, height / 3);
    mPath.reset();
    for (int depth = 0; depth < mDepth; ++depth)
    {
      final int x = DEPTH_INDENT + depth * DEPTH_INDENT + SQUARE_VERTICAL_PADDING;
      mPath.moveTo(x, 0);
      mPath.lineTo(x, height);
    }
    final int squareAvoidance = EXPANSIONS_NONE == mExpansions ? 0 : squareSize / 2 + 2;
    if (0 != (mConnections & CONNECTIONS_UP))
    {
      mPath.moveTo(centerX, 0);
      mPath.lineTo(centerX, centerY - squareAvoidance);
    }
    if (0 != (mConnections & CONNECTIONS_DOWN))
    {
      mPath.moveTo(centerX, centerY + squareAvoidance);
      mPath.lineTo(centerX, height);
    }
    mPath.moveTo(centerX + squareAvoidance, centerY);
    mPath.lineTo(centerX + RIGHT_EXTEND * 2, centerY);
    if (EXPANSIONS_OPEN == mExpansions)
    {
      mPath.moveTo(centerX + DEPTH_INDENT, centerY);
      mPath.lineTo(centerX + DEPTH_INDENT, height);
    }
    canvas.drawPath(mPath, mDashedPaint);
    if (EXPANSIONS_NONE == mExpansions) return;
    final int leftCornerX = centerX - squareSize / 2;
    final int leftCornerY = centerY - squareSize / 2;
    canvas.drawRect(leftCornerX, leftCornerY, leftCornerX + squareSize, leftCornerY + squareSize, mSolidPaint);
    canvas.drawLine(centerX - squareSize / 4, centerY, centerX + squareSize / 4, centerY, mSolidPaint);
    if (EXPANSIONS_CLOSED == mExpansions)
      canvas.drawLine(centerX, centerY - squareSize / 4, centerX, centerY + squareSize / 4, mSolidPaint);
  }

  @Override protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec)
  {
    final int h = MeasureSpec.getSize(heightMeasureSpec);
    final int squareSize = oddMin(h - 2 * SQUARE_VERTICAL_PADDING, h / 3);
    final int minW = DEPTH_INDENT + mDepth * DEPTH_INDENT + RIGHT_EXTEND * 3;
    final int maxW = squareSize + minW;
    final int w;
    if (MeasureSpec.UNSPECIFIED == MeasureSpec.getMode(widthMeasureSpec))
      w = maxW;
    else
    {
      final int specW = MeasureSpec.getSize(widthMeasureSpec);
      w = Math.min(Math.max(minW, specW), maxW);
    }
    setMeasuredDimension(w, h);
  }

  private int oddMin(final int... values)
  {
    int min = Integer.MAX_VALUE;
    for (int i : values)
      if (i < min) min = i;
    return 0 != (min & 1) ? min : min - 1;
  }
}
