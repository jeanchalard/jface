package com.j.jface.feed.fragments

import android.app.Activity
import android.app.Fragment
import android.content.Intent
import com.j.jface.R
import com.j.jface.client.Client
import com.j.jface.lifecycle.WrappedFragment

class ImageSelectorFragment(a : Args, client : Client) : WrappedFragment(a.inflater.inflate(R.layout.fragment_image_selector, a.container, false))
{
  companion object
  {
    const val CHOOSE_IMAGE_INTENT = 100
  }
  private val mClient : Client = client
  private val mFragment : Fragment = a.fragment

  init
  {
    mView.findViewById(R.id.select_image_button).setOnClickListener { onClickChangePicture() }
  }

  fun onClickChangePicture()
  {
    val intent = Intent()
    intent.type = "image/*"
    intent.action = Intent.ACTION_GET_CONTENT
    (mFragment.context as Activity).startActivityForResult(intent, CHOOSE_IMAGE_INTENT)
  }
}
