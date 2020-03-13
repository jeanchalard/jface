package com.j.jface.feed.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.j.jface.Const
import com.j.jface.face.WatchFace

class FaceView @JvmOverloads constructor(context : Context, attrs : AttributeSet? = null, defStyleAttr : Int = 0) : View(context, attrs, defStyleAttr), WatchFace.Invalidator
{
  private val face = WatchFace(context, this)

  override fun onMeasure(widthMeasureSpec : Int, heightMeasureSpec : Int)
  {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    val width = measuredWidth
    setMeasuredDimension(width, width)
  }

  private val bounds = Rect(0, 0, Const.SCREEN_SIZE, Const.SCREEN_SIZE)
  override fun onDraw(canvas : Canvas?)
  {
    super.onDraw(canvas)
    if (null == canvas) return
    val scale = (width).toFloat() / Const.SCREEN_SIZE
    canvas.scale(scale, scale)
    face.onDraw(canvas, bounds)
    postDelayed({invalidate()}, 1000L)
  }
}
