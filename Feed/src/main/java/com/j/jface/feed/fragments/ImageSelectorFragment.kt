package com.j.jface.feed.fragments

import android.util.Log
import com.j.jface.R
import com.j.jface.client.Client
import com.j.jface.lifecycle.WrappedFragment

class ImageSelectorFragment(a : Args, client : Client) :
        WrappedFragment(a.inflater.inflate(R.layout.fragment_image_selector, a.container, false))
{
    private val mClient : Client

    init
    {
        mClient = client
        mView.findViewById(R.id.select_image_button).setOnClickListener { onClickChangePicture() }
    }

    fun onClickChangePicture()
    {
        Log.e("Test", "retest")
    }
}
