package com.j.jface.feed.fragments

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.provider.MediaStore
import android.view.View
import android.widget.SeekBar
import com.google.android.gms.wearable.Asset
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.lifecycle.WrappedFragment
import com.j.jface.wear.Wear
import com.ortiz.touch.TouchImageView
import java.io.ByteArrayOutputStream

class ImageEditorFragment(a : Args, iea : ImageEditorArgs) : WrappedFragment(a.inflater.inflate(R.layout.fragment_image_editor, a.container, false)), SeekBar.OnSeekBarChangeListener
{
  data class ImageEditorArgs(val wear : Wear, val receivedData : Intent)
  private val mWear = iea.wear
  private val mFragment = a.fragment
  private val mViewFinder : View = mView.findViewById(R.id.view_finder)
  private val mBlackener : View = mView.findViewById(R.id.blackener)
  private val mBrightnessBar : SeekBar = mView.findViewById(R.id.select_image_brightness) as SeekBar
  init
  {
    val bitmap = MediaStore.Images.Media.getBitmap(a.fragment.context!!.contentResolver, iea.receivedData.data)
    val imageView = mView.findViewById(R.id.edited_image) as TouchImageView
    imageView.setImageBitmap(bitmap)
    mView.findViewById<View>(R.id.image_set_button).setOnClickListener { imageChosen(imageView) }
    mBlackener.alpha = 0f
    mBrightnessBar.progress = 1000
    mBrightnessBar.setOnSeekBarChangeListener(this)
  }

  private fun imageChosen(imageView : TouchImageView)
  {
    val src = (imageView.drawable as BitmapDrawable).bitmap
    val portion = imageView.zoomedRect

    val wAdjustRatio = mViewFinder.width.toFloat() / imageView.width
    val hAdjustRatio = mViewFinder.height.toFloat() / imageView.height
    val xAdjust = portion.width() * (1 - wAdjustRatio) / 2
    val yAdjust = portion.height() * (1 - hAdjustRatio) / 2
    portion.left += xAdjust ; portion.right -= xAdjust
    portion.top += yAdjust ; portion.bottom -= yAdjust

    val srcRect = Rect((portion.left * src.width).toInt(), (portion.top * src.height).toInt(), (portion.right * src.width).toInt(), (portion.bottom * src.height).toInt())

    val matrix = Matrix()
    val scale = Const.SCREEN_SIZE.toFloat() / srcRect.width() // height is the same
    matrix.postScale(scale, scale)
    matrix.preTranslate(-srcRect.left.toFloat(), -srcRect.top.toFloat())

    val paint = Paint()
    paint.isFilterBitmap = true
    paint.isAntiAlias = true
    val scaledBrightness = 255 * mBrightnessBar.progress.toFloat() / 1000
    paint.colorFilter = LightingColorFilter(Color.rgb(scaledBrightness.toInt(), scaledBrightness.toInt(), scaledBrightness.toInt()), 0)
    val c = Canvas()
    val background = Bitmap.createBitmap(Const.SCREEN_SIZE, Const.SCREEN_SIZE, Bitmap.Config.ARGB_8888)
    background.density = src.density
    c.setBitmap(background)
    c.concat(matrix)
    c.drawColor(-0x01000000)
    c.drawBitmap(src, 0f, 0f, paint)
    c.setBitmap(null)

    val data = ByteArrayOutputStream()
    background.compress(Bitmap.CompressFormat.PNG, 100, data)
    val asset = Asset.createFromBytes(data.toByteArray())
    mWear.putDataToCloud(Const.DATA_PATH + "/" + Const.DATA_KEY_BACKGROUND, Const.DATA_KEY_BACKGROUND, asset)
    mFragment.fragmentManager!!.popBackStack()
  }

  override fun onStartTrackingTouch(seekBar : SeekBar?) {}
  override fun onStopTrackingTouch(seekBar : SeekBar?) {}
  override fun onProgressChanged(seekBar : SeekBar?, progress : Int, fromUser : Boolean)
  {
    mBlackener.alpha = 1f - progress.toFloat() / 1000
  }
}
