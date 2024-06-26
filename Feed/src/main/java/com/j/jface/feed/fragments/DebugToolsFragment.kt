package com.j.jface.feed.fragments

import android.os.Handler
import android.os.Message
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.TimePicker
import com.google.android.gms.wearable.DataMap
import com.google.firebase.messaging.FirebaseMessaging
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.feed.views.FaceView
import com.j.jface.feed.views.SnackbarRegistry
import com.j.jface.firebase.Firebase
import com.j.jface.lifecycle.WrappedFragment
import com.j.jface.wear.Wear
import kotlinx.coroutines.MainCoroutineDispatcher
import java.io.IOException
import java.time.Instant
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId

const val MSG_UPDATE_TIME = 1
const val DEBUG_FENCES_PATH = "${Const.DATA_PATH}/${Const.DATA_KEY_DEBUG_FENCES}"
const val DEBUG_TIME_OFFSET_PATH = "${Const.DATA_PATH}/${Const.DATA_KEY_DEBUG_TIME_OFFSET}"

class DebugToolsFragment(a : WrappedFragment.Args, private val mWear : Wear) : WrappedFragment(a.inflater.inflate(R.layout.fragment_debug_tools, a.container, false)), View.OnClickListener, NumberPicker.OnValueChangeListener, TimePicker.OnTimeChangedListener
{
  private val mFragment = a.fragment
  private var mOffset = 0L
  private var mTicking = false
  private val mDaysOffsetUI : NumberPicker = mView.findViewById(R.id.daysOffsetUI)
  private val mHoursUI : NumberPicker = mView.findViewById(R.id.hoursUI)
  private val mMinutesUI : NumberPicker = mView.findViewById(R.id.minutesUI)
  private val mSecondsUI : NumberPicker = mView.findViewById(R.id.secondsUI)
  private val mOffsetLabel : TextView = mView.findViewById(R.id.offsetLabel)
  private val mFenceUIs = arrayOf(R.id.fence1, R.id.fence2, R.id.fence3, R.id.fence4).map { mView.findViewById<CheckBox>(it) }
  private val mHandler = TabDebugToolsHandler(this)
  private val mFace : FaceView = mView.findViewById(R.id.face_simulator) as FaceView
  private val mFaceToggles = arrayOf(R.id.debug_btn_coarse, R.id.debug_btn_burnin, R.id.debug_btn_ambient, R.id.debug_btn_visible).map { mView.findViewById<Button>(it) }
  private val mWearUpdateListener = object : Firebase.WearDataUpdateListener() {
    override fun onWearDataUpdated(path : String, data : DataMap) = this@DebugToolsFragment.onWearDataUpdated(path, data)
  }

  private class TabDebugToolsHandler(private val p : DebugToolsFragment) : Handler()
  {
    override fun handleMessage(message : Message)
    {
      when (message.what)
      {
        MSG_UPDATE_TIME ->
        {
          p.tick()
          sendEmptyMessageDelayed(MSG_UPDATE_TIME, 1000)
        }
      }
    }
  }

  init
  {
    mView.findViewById<View>(R.id.button_now).setOnClickListener(this)
    mDaysOffsetUI.minValue = 0
    mDaysOffsetUI.maxValue = 14
    mHoursUI.minValue = 0
    mHoursUI.maxValue = 23
    mMinutesUI.minValue = 0
    mMinutesUI.maxValue = 59
    mSecondsUI.minValue = 0
    mSecondsUI.maxValue = 59
    tick()
    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, 1000)

    addListeners()

    if (Const.RIO_MODE) mFenceUIs[2].text = "渋谷"

    val nodeNameTextView = mView.findViewById<TextView>(R.id.nodeId_textView)
    mWear.getNodeName(mFragment.context!!) { nodeName ->
      a.fragment.activity!!.runOnUiThread { nodeNameTextView.text = "Node id : ${nodeName}" }
    }

    mView.findViewById<View>(R.id.button_delete_FCM_token).setOnClickListener {
      Thread {
        try
        {
          FirebaseMessaging.getInstance().deleteToken()
        }
        catch (e : IOException)
        {
          Log.e("Can't delete token", "" + e)
        }

        FirebaseMessaging.getInstance().token // Force token generation
      }.start()
    }
  }

  private fun addListeners()
  {
    mHoursUI.setOnValueChangedListener(this)
    mMinutesUI.setOnValueChangedListener(this)
    mSecondsUI.setOnValueChangedListener(this)
    mDaysOffsetUI.setOnValueChangedListener(this)
    mFenceUIs.forEach { it.setOnClickListener(this) }
    mFaceToggles.forEach { it.setOnClickListener(this) }
  }

  private fun removeListeners()
  {
    mHoursUI.setOnValueChangedListener(null)
    mMinutesUI.setOnValueChangedListener(null)
    mSecondsUI.setOnValueChangedListener(null)
    mDaysOffsetUI.setOnValueChangedListener(null)
    mFenceUIs.forEach { it.setOnClickListener(null) }
    mFaceToggles.forEach { it.setOnClickListener(null) }
  }

  private fun <R> withoutListeners(block : () -> R)
  {
    try
    {
      removeListeners()
      block()
    }
    finally
    {
      addListeners()
    }
  }

  override fun onResume()
  {
    SnackbarRegistry.setSnackbarParent(mView)
    mWearUpdateListener.resume()
  }

  override fun onPause()
  {
    SnackbarRegistry.unsetSnackbarParent(mView)
    mWearUpdateListener.pause()
  }

  override fun onDestroy() {
    mFace.onDestroy()
  }

  private fun tick()
  {
    mTicking = true
    val now = System.currentTimeMillis() + mOffset
    val zdt = OffsetDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault())
    val offset = mOffset
    mTicking = false

    mFragment.activity?.runOnUiThread {
      mHoursUI.value = zdt.hour
      mMinutesUI.value = zdt.minute
      mSecondsUI.value = zdt.second
      mOffsetLabel.text = java.lang.Long.toString(offset)
    }
  }

  private fun baseDay(timestamp : Long, utcOffset : Long) : Long = (timestamp + utcOffset * 1000) / DateUtils.DAY_IN_MILLIS

  fun onWearDataUpdated(path : String, data : DataMap)
  {
    when (path)
    {
      DEBUG_TIME_OFFSET_PATH -> withoutListeners {
        data.get<Long>(Const.DATA_KEY_DEBUG_TIME_OFFSET)?.let { mOffset = it }
        val now = System.currentTimeMillis()
        val utcOffset = Const.MILLISECONDS_TO_UTC
        mDaysOffsetUI.value = (baseDay(now + mOffset, utcOffset) - baseDay(now, utcOffset)).toInt()
        tick()
      }
      DEBUG_FENCES_PATH -> withoutListeners {
          val fences = data.get<Long>(Const.DATA_KEY_DEBUG_FENCES)?.toInt() ?: 0
          for (i in mFenceUIs.indices) mFenceUIs[i].isChecked = 0 != (fences and (1 shl i))
      }
    }
    mFace.invalidate();
  }

  private fun updateOffset()
  {
    if (mTicking) return
    mHandler.removeMessages(MSG_UPDATE_TIME)
    val now = System.currentTimeMillis()
    val offsetTime = OffsetDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault()).with(LocalTime.of(mHoursUI.value, mMinutesUI.value, mSecondsUI.value)).toEpochSecond() * 1000 + now % 1000
    val dayOffset = mDaysOffsetUI.value
    val offsetDateTime = offsetTime + dayOffset * 86400000
    mOffset = offsetDateTime - now
    mHandler.sendEmptyMessage(MSG_UPDATE_TIME)
    mWear.putDataToCloud(DEBUG_TIME_OFFSET_PATH, Const.DATA_KEY_DEBUG_TIME_OFFSET, mOffset)
  }

  private fun updateFences()
  {
    var fences = 0
    for (i in mFenceUIs.indices)
      if (mFenceUIs[i].isChecked)
        fences = fences or (1 shl i)
    mWear.putDataToCloud(DEBUG_FENCES_PATH, Const.DATA_KEY_DEBUG_FENCES, fences.toLong())
  }

  override fun onClick(v : View)
  {
    when (v.id)
    {
      R.id.button_now ->
      {
        mOffset = 0
        tick()
        updateOffset()
        mHandler.removeMessages(MSG_UPDATE_TIME)
        mHandler.sendEmptyMessage(MSG_UPDATE_TIME)
        updateFences()
      }
      R.id.debug_btn_coarse -> mFace.toggleCoarse()
      R.id.debug_btn_burnin -> mFace.toggleBurnin()
      R.id.debug_btn_ambient -> mFace.toggleAmbient()
      R.id.debug_btn_visible -> mFace.toggleVisible()
      else -> updateFences()
    }
    mFace.invalidate();
  }

  override fun onValueChange(picker : NumberPicker, oldVal : Int, newVal : Int) = updateOffset()
  override fun onTimeChanged(view : TimePicker, hourOfDay : Int, minute : Int) = updateOffset()
}
