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
import com.j.jface.feed.views.FaceView
import com.j.jface.lifecycle.FragmentWrapper
import com.j.jface.lifecycle.WrappedFragment
import com.j.jface.wear.Wear

class FaceFragment(a : Args, private val mWear : Wear) : WrappedFragment(a.inflater.inflate(R.layout.fragment_face, a.container, false))
{
  private val mFragment : Fragment = a.fragment
  private val mFace : FaceView = mView.findViewById(R.id.faceFragment_face) as FaceView


}
