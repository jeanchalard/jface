package com.j.jface.org

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

import com.j.jface.R
import com.j.jface.clamp

/**
 * A view for expanders with dashed lines, possibly a + or - sign in a square.
 * All of this is set through levels.
 */

class ExpanderView @JvmOverloads constructor(context : Context, attrs : AttributeSet? = null) : View(context, attrs)
{
  companion object
  {
    private const val RIGHT_EXTEND = 20
    private const val DEPTH_INDENT = 30
    private const val SQUARE_VERTICAL_PADDING = 4

    const val CONNECTIONS_NONE = 0
    const val CONNECTIONS_UP = 1
    const val CONNECTIONS_DOWN = 2
    const val CONNECTIONS_BOTH = 3

    const val EXPANSIONS_NONE = 0 // A leaf.
    const val EXPANSIONS_CLOSED = 1 // Closed : a + in a box
    const val EXPANSIONS_OPEN = 2 // Open : a - in a box
  }

  private var mConnections = 0 // Whether to draw the dashed lines up and down
  private var mExpansions = 0

  private var mDepth = 0

  private val mDashedPaint = Paint()
  private val mSolidPaint = Paint()
  private val mPath = Path()

  fun setConnections(connections : Int)
  {
    mConnections = connections
    invalidate()
  }

  fun setExpansions(expansions : Int)
  {
    mExpansions = expansions
    invalidate()
  }

  fun setDepth(depth : Int)
  {
    mDepth = depth
    invalidate()
    requestLayout()
  }

  init
  {
    val c = context.getColor(R.color.color_primary)
    mDashedPaint.pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
    mDashedPaint.color = c
    mDashedPaint.style = Paint.Style.STROKE
    mSolidPaint.color = c
    mSolidPaint.style = Paint.Style.STROKE
  }

  public override fun onDraw(canvas : Canvas)
  {
    val height = height
    val centerX = DEPTH_INDENT + mDepth * DEPTH_INDENT + SQUARE_VERTICAL_PADDING
    val centerY = height / 2
    val squareSize = oddMin(width - centerX - RIGHT_EXTEND, height - 2 * SQUARE_VERTICAL_PADDING, height / 3)
    mPath.reset()
    for (depth in 0 until mDepth)
    {
      val x = DEPTH_INDENT + depth * DEPTH_INDENT + SQUARE_VERTICAL_PADDING
      mPath.moveTo(x.toFloat(), 0f)
      mPath.lineTo(x.toFloat(), height.toFloat())
    }
    val squareAvoidance = if (EXPANSIONS_NONE == mExpansions) 0 else squareSize / 2 + 2
    if (0 != mConnections and CONNECTIONS_UP)
    {
      mPath.moveTo(centerX.toFloat(), 0f)
      mPath.lineTo(centerX.toFloat(), (centerY - squareAvoidance).toFloat())
    }
    if (0 != mConnections and CONNECTIONS_DOWN)
    {
      mPath.moveTo(centerX.toFloat(), (centerY + squareAvoidance).toFloat())
      mPath.lineTo(centerX.toFloat(), height.toFloat())
    }
    mPath.moveTo((centerX + squareAvoidance).toFloat(), centerY.toFloat())
    mPath.lineTo((centerX + RIGHT_EXTEND * 2).toFloat(), centerY.toFloat())
    if (EXPANSIONS_OPEN == mExpansions)
    {
      mPath.moveTo((centerX + DEPTH_INDENT).toFloat(), centerY.toFloat())
      mPath.lineTo((centerX + DEPTH_INDENT).toFloat(), height.toFloat())
    }
    canvas.drawPath(mPath, mDashedPaint)
    if (EXPANSIONS_NONE == mExpansions) return
    val leftCornerX = centerX - squareSize / 2
    val leftCornerY = centerY - squareSize / 2
    canvas.drawRect(leftCornerX.toFloat(), leftCornerY.toFloat(), (leftCornerX + squareSize).toFloat(), (leftCornerY + squareSize).toFloat(), mSolidPaint)
    canvas.drawLine((centerX - squareSize / 4).toFloat(), centerY.toFloat(), (centerX + squareSize / 4).toFloat(), centerY.toFloat(), mSolidPaint)
    if (EXPANSIONS_CLOSED == mExpansions)
      canvas.drawLine(centerX.toFloat(), (centerY - squareSize / 4).toFloat(), centerX.toFloat(), (centerY + squareSize / 4).toFloat(), mSolidPaint)
  }

  override fun onMeasure(widthMeasureSpec : Int, heightMeasureSpec : Int)
  {
    val h = MeasureSpec.getSize(heightMeasureSpec)
    val squareSize = oddMin(h - 2 * SQUARE_VERTICAL_PADDING, h / 3)
    val minW = DEPTH_INDENT + mDepth * DEPTH_INDENT + RIGHT_EXTEND * 3
    val maxW = squareSize + minW
    val w = if (MeasureSpec.UNSPECIFIED == MeasureSpec.getMode(widthMeasureSpec))
      maxW
    else
      clamp(minW, MeasureSpec.getSize(widthMeasureSpec), maxW)
    setMeasuredDimension(w, h)
  }

  private fun oddMin(vararg values : Int) : Int
  {
    val min = values.minOrNull() ?: Integer.MAX_VALUE
    return if (0 != (min and 1)) min else min - 1
  }
}
