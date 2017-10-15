package com.j.jface.feed.fragments

import android.app.Activity
import android.app.Fragment
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ShapeDrawable
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.client.Client
import com.j.jface.lifecycle.WrappedFragment

class ImageSelectorFragment(a : Args, client : Client) : WrappedFragment(a.inflater.inflate(R.layout.fragment_image_selector, a.container, false)), Client.GetBitmapCallback
{
  companion object
  {
    const val CHOOSE_IMAGE_INTENT = 100
  }
  private val mClient : Client = client
  private val mFragment : Fragment = a.fragment
  private val mImageButton : ImageButton = mView.findViewById(R.id.select_image_button) as ImageButton
  private val mSpinner : ProgressBar = mView.findViewById(R.id.select_image_wait) as ProgressBar
  init
  {
    mImageButton.setOnClickListener { onClickChangePicture() }
    mImageButton.visibility = View.GONE
    mImageButton.minimumWidth = Const.SCREEN_SIZE
    mImageButton.minimumHeight = Const.SCREEN_SIZE
    mSpinner.visibility = View.VISIBLE
    mSpinner.minimumWidth = Const.SCREEN_SIZE
    mSpinner.minimumHeight = Const.SCREEN_SIZE
    mClient.getBitmap(Const.DATA_PATH + "/" + Const.CONFIG_KEY_BACKGROUND, Const.CONFIG_KEY_BACKGROUND, this)
  }

  fun onClickChangePicture()
  {
    val intent = Intent()
    intent.type = "image/*"
    intent.action = Intent.ACTION_GET_CONTENT
    (mFragment.context as Activity).startActivityForResult(intent, CHOOSE_IMAGE_INTENT)
  }

  override fun run(path : String, key : String, bitmap : Bitmap?)
  {
    mFragment.activity.runOnUiThread {
      mImageButton.visibility = View.VISIBLE
      mSpinner.visibility = View.GONE
      if (null != bitmap)
        mImageButton.setImageBitmap(bitmap)
      else
        mImageButton.setImageDrawable(mFragment.resources.getDrawable(R.drawable.black_box, mFragment.activity.theme))
    }
  }
}
