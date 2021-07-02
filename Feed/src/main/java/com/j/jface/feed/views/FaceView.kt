package com.j.jface.feed.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceService.PROPERTY_BURN_IN_PROTECTION
import android.support.wearable.watchface.WatchFaceService.PROPERTY_LOW_BIT_AMBIENT
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.j.jface.Const
import com.j.jface.face.WatchFace

class FaceView @JvmOverloads constructor(context : Context, attrs : AttributeSet? = null, defStyleAttr : Int = 0) : View(context, attrs, defStyleAttr), WatchFace.Invalidator
{
  private val face = WatchFace(context, this)
  private var coarse = false
  private var burnin = false
  private var ambient = false
  private var visible = true

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
    postDelayed({face.onTimeTick()}, 1000L)
  }

  fun onDestroy() = face.onDestroy()

  override fun onTouchEvent(event : MotionEvent?) : Boolean {
    if (null == event) return false
    if (event.actionMasked == MotionEvent.ACTION_DOWN) return true
    val scale = (width).toFloat() / Const.SCREEN_SIZE
    val x = (event.x / scale).toInt()
    val y = (event.y / scale).toInt()
    if (event.actionMasked != MotionEvent.ACTION_UP) return false
    face.onTapCommand(WatchFaceService.TAP_TYPE_TAP, x, y, SystemClock.uptimeMillis())
    return true
  }

  private fun sendOnPropertiesChanged() = face.onPropertiesChanged(Bundle().apply {
    putBoolean(PROPERTY_LOW_BIT_AMBIENT, coarse)
    putBoolean(PROPERTY_BURN_IN_PROTECTION, burnin)
  })

  fun toggleCoarse() {
    coarse = !coarse
    sendOnPropertiesChanged()
  }

  fun toggleBurnin() {
    burnin = !burnin
    sendOnPropertiesChanged()
  }

  fun toggleAmbient() {
    ambient = !ambient
    face.onAmbientModeChanged(ambient)
  }

  fun toggleVisible() {
    visible = !visible
    face.onVisibilityChanged(visible)
  }
}
