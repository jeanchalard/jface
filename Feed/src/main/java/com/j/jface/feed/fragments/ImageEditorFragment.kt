package com.j.jface.feed.fragments

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.provider.MediaStore
import com.google.android.gms.wearable.Asset
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.client.Client
import com.j.jface.feed.views.SquareImageView
import com.j.jface.lifecycle.WrappedFragment
import com.ortiz.touch.TouchImageView
import java.io.ByteArrayOutputStream

class ImageEditorFragment(a : Args, iea : ImageEditorArgs) : WrappedFragment(a.inflater.inflate(R.layout.fragment_image_editor, a.container, false))
{
  data class ImageEditorArgs(val client : Client, val receivedData : Intent)
  private val mFragment = a.fragment
  private val mClient = iea.client
  private val mViewFinder = mView.findViewById(R.id.view_finder) as SquareImageView
  init
  {
    val bitmap = MediaStore.Images.Media.getBitmap(a.fragment.context.contentResolver, iea.receivedData.data)
    val imageView = mView.findViewById(R.id.edited_image) as TouchImageView
    imageView.setImageBitmap(bitmap)
    mView.findViewById(R.id.image_set_button).setOnClickListener { imageChosen(imageView) }
  }

  fun makeSquare(r : Rect)
  {
    val offset = Math.abs(r.width() - r.height()) / 2
    if (r.width() > r.height())
    { r.left += offset ; r.right -= offset }
    else
    { r.top += offset ; r.bottom -= offset }
  }

  fun imageChosen(imageView : TouchImageView)
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
    val c = Canvas()
    val background = Bitmap.createBitmap(Const.SCREEN_SIZE, Const.SCREEN_SIZE, Bitmap.Config.ARGB_8888)
    background.density = src.density
    c.setBitmap(background)
    c.concat(matrix)
    c.drawColor(-0x01000000)
    c.drawBitmap(src, 0f, 0f, paint)
    c.setBitmap(null)

    val data = ByteArrayOutputStream();
    background.compress(Bitmap.CompressFormat.PNG, 100, data)
    val asset = Asset.createFromBytes(data.toByteArray())
    mClient.putData(Const.DATA_PATH + "/" + Const.DATA_KEY_BACKGROUND, Const.DATA_KEY_BACKGROUND, asset)
    mFragment.fragmentManager.popBackStack()
  }
}
