package com.j.jface.feed.fragments

import android.app.Fragment
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.action.GThread
import com.j.jface.lifecycle.FragmentWrapper
import com.j.jface.lifecycle.WrappedFragment

class ImageSelectorFragment(a : Args, gThread : GThread) : WrappedFragment(a.inflater.inflate(R.layout.fragment_image_selector, a.container, false))
{
  private val mGThread = gThread
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
    mGThread.getBitmap(Const.DATA_PATH + "/" + Const.CONFIG_KEY_BACKGROUND, Const.CONFIG_KEY_BACKGROUND, ::setImage)
    (mView.findViewById(R.id.select_image_none) as Button).setOnClickListener { removeBackground() }
  }

  fun onClickChangePicture()
  {
    val intent = Intent()
    intent.type = "image/*"
    intent.action = Intent.ACTION_GET_CONTENT
    mFragment.startActivityForResult(intent, Const.CHOOSE_IMAGE_RESULT_CODE)
  }

  override fun onActivityResult(requestCode : Int, resultCode : Int, data : Intent?)
  {
    if (null == data || resultCode != Const.CHOOSE_IMAGE_RESULT_CODE) return
    val f = FragmentWrapper<ImageEditorFragment>(ImageEditorFragment::class.java, ImageEditorFragment.ImageEditorArgs(mGThread, data))
    mFragment.fragmentManager.beginTransaction()
     .addToBackStack("ImageEditor")
     .replace(R.id.dataFeedContents, f)
     .commit()
  }

  fun setImage(path : String, key : String, bitmap : Bitmap?)
  {
    mFragment.activity?.runOnUiThread {
      mImageButton.visibility = View.VISIBLE
      mSpinner.visibility = View.GONE
      if (null != bitmap)
        mImageButton.setImageBitmap(bitmap)
      else
        mImageButton.setImageDrawable(mFragment.resources.getDrawable(R.drawable.black_box, mFragment.activity.theme))
    }
  }

  fun removeBackground()
  {
    mGThread.deleteData(Const.DATA_PATH + "/" + Const.CONFIG_KEY_BACKGROUND)
    mImageButton.setImageDrawable(mFragment.resources.getDrawable(R.drawable.black_box, mFragment.activity.theme))
  }
}
