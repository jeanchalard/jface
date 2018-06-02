package com.j.jface.feed.fragments

import android.app.Fragment
import android.content.Intent
import android.os.Handler
import android.os.Message
import android.text.format.Time
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.TimePicker
import com.google.android.gms.wearable.DataMap
import com.google.firebase.iid.FirebaseInstanceId
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.action.InformUserAction
import com.j.jface.feed.views.SnackbarRegistry
import com.j.jface.firebase.Firebase
import com.j.jface.lifecycle.WrappedFragment
import com.j.jface.org.todo.TodoProvider
import com.j.jface.wear.Wear
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

const val MSG_UPDATE_TIME = 1
const val GRACE_FOR_UPDATE = 200
const val DEBUG_FENCES_PATH = "${Const.DATA_PATH}/${Const.DATA_KEY_DEBUG_FENCES}"
const val DEBUG_TIME_OFFSET_PATH = "${Const.DATA_PATH}/${Const.DATA_KEY_DEBUG_TIME_OFFSET}"

class DebugToolsFragment(a : WrappedFragment.Args, private val mWear : Wear) : WrappedFragment(a.inflater.inflate(R.layout.fragment_debug_tools, a.container, false)), View.OnClickListener, NumberPicker.OnValueChangeListener, TimePicker.OnTimeChangedListener
{
  private val mFragment : Fragment = a.fragment
  private var mOffset : Long = 0
  private var mTicking = false
  private val mTime1 : Time = Time()
  private val mTime2 : Time = Time()
  private val mDaysOffsetUI : NumberPicker = mView.findViewById(R.id.daysOffsetUI)
  private val mHoursUI : NumberPicker = mView.findViewById(R.id.hoursUI)
  private val mMinutesUI : NumberPicker = mView.findViewById(R.id.minutesUI)
  private val mSecondsUI : NumberPicker = mView.findViewById(R.id.secondsUI)
  private val mOffsetLabel : TextView = mView.findViewById(R.id.offsetLabel)
  private val mFenceUIs : Array<CheckBox> = arrayOf(mView.findViewById(R.id.fence1), mView.findViewById(R.id.fence2), mView.findViewById(R.id.fence3), mView.findViewById(R.id.fence4))
  private val mHandler = TabDebugToolsHandler(this)
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

    if (Const.RIO_MODE) mFenceUIs[2].text = "六本木"

    val nodeNameTextView = mView.findViewById<TextView>(R.id.nodeId_textView)
    mWear.getNodeName(mFragment.context) { nodeName ->
      a.fragment.activity.runOnUiThread { nodeNameTextView.text = "Node id : ${nodeName}" }
    }

    mView.findViewById<View>(R.id.button_copy_todo_from_storage).setOnClickListener {
      val intent = Intent()
      intent.type = "*/*"
      intent.action = Intent.ACTION_GET_CONTENT
      a.fragment.startActivityForResult(intent, Const.DESTROY_DATABASE_AND_REPLACE_WITH_FILE_CONTENTS_RESULT_CODE)
    }

    mView.findViewById<View>(R.id.button_delete_FCM_token).setOnClickListener {
      Thread {
        try
        {
          FirebaseInstanceId.getInstance().deleteInstanceId()
        }
        catch (e : IOException)
        {
          Log.e("Can't delete token", "" + e)
        }

        FirebaseInstanceId.getInstance().token // Force token generation
      }.start()
    }

    mView.findViewById<View>(R.id.button_fetch_data_state).setOnClickListener {
      mWear.getData("${Const.DATA_PATH}/${Const.DATA_KEY_USER_MESSAGE}") { _, data ->
        a.fragment.activity.runOnUiThread { mView.findViewById<TextView>(R.id.text_data_state).text = data.getString(Const.DATA_KEY_USER_MESSAGE) }
      }
    }
  }

  private fun addListeners()
  {
    mHoursUI.setOnValueChangedListener(this)
    mMinutesUI.setOnValueChangedListener(this)
    mSecondsUI.setOnValueChangedListener(this)
    mDaysOffsetUI.setOnValueChangedListener(this)
    for (c in mFenceUIs) c.setOnClickListener(this)
  }

  private fun removeListeners()
  {
    mHoursUI.setOnValueChangedListener(null)
    mMinutesUI.setOnValueChangedListener(null)
    mSecondsUI.setOnValueChangedListener(null)
    mDaysOffsetUI.setOnValueChangedListener(null)
    for (c in mFenceUIs) c.setOnClickListener(null)
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
      removeListeners()
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

  override fun onActivityResult(requestCode : Int, resultCode : Int, data : Intent?)
  {
    if (null == data || Const.DESTROY_DATABASE_AND_REPLACE_WITH_FILE_CONTENTS_RESULT_CODE != requestCode) return
    val fileUri = data.data
    var exception : FileNotFoundException? = null
    if (null != fileUri)
    {
      var inputStream : InputStream? = null
      try
      {
        inputStream = mFragment.context.contentResolver.openInputStream(fileUri)
      }
      catch (e : FileNotFoundException)
      {
        exception = e
      }

      if (null == exception && null != inputStream)
      // Do this on the UI thread because YOLO. This is only for debug so we don't care.
        if (TodoProvider.destroyDatabaseAndReplaceWithFileContents(mFragment.context, inputStream))
        {
          InformUserAction(mFragment.activity, "Database dumped, restart JOrg", "Kill",
           View.OnClickListener { System.exit(0) }, null).invoke()
          return
        }
    }
    InformUserAction(mFragment.activity, "File can't be opened." + if (null == exception) "" else " " + exception.toString(), null, null, null).invoke()
  }

  private fun tick()
  {
    mTicking = true
    mTime1.set(System.currentTimeMillis() + mOffset)
    mHoursUI.value = mTime1.hour
    mMinutesUI.value = mTime1.minute
    mSecondsUI.value = mTime1.second

    mTime2.setToNow()
    mOffsetLabel.text = java.lang.Long.toString(mOffset)
    mTicking = false
  }

  fun onWearDataUpdated(path : String, data : DataMap)
  {
    when (path)
    {
      DEBUG_TIME_OFFSET_PATH -> withoutListeners {
        mOffset = data[Const.DATA_KEY_DEBUG_TIME_OFFSET]
        mTime1.setToNow()
        val now = mTime1.toMillis(true)
        val utcOffset = Const.MILLISECONDS_TO_UTC
        mDaysOffsetUI.value = Time.getJulianDay(now + mOffset, utcOffset) - Time.getJulianDay(now, utcOffset)
        tick()
      }
      DEBUG_FENCES_PATH -> withoutListeners {
          val fences = data.get<Long>(Const.DATA_KEY_DEBUG_FENCES).toInt()
          for (i in mFenceUIs.indices) mFenceUIs[i].isChecked = 0 != (fences and (1 shl i))
      }
    }
  }

  private fun updateOffset(grace : Int)
  {
    if (mTicking) return
    mHandler.removeMessages(MSG_UPDATE_TIME)
    mTime2.setToNow()
    val second = mSecondsUI.value
    val minute = mMinutesUI.value
    val hour = mHoursUI.value
    mTime1.set(second, minute, hour, mTime2.monthDay, mTime2.month, mTime2.year)
    val dayOffset = mDaysOffsetUI.value
    mTime1.set(mTime1.toMillis(true) + dayOffset * 86400000 - grace)
    mOffset = mTime1.toMillis(true) - mTime2.toMillis(true)
    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, grace.toLong())
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
        updateOffset(0)
        mHandler.removeMessages(MSG_UPDATE_TIME)
        mHandler.sendEmptyMessage(MSG_UPDATE_TIME)
        updateFences()
      }
      else -> updateFences()
    }
  }

  override fun onValueChange(picker : NumberPicker, oldVal : Int, newVal : Int) = updateOffset(GRACE_FOR_UPDATE)
  override fun onTimeChanged(view : TimePicker, hourOfDay : Int, minute : Int) = updateOffset(GRACE_FOR_UPDATE)
}
