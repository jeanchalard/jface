package com.j.jface.feed.fragments

import android.content.Intent
import android.os.Handler
import android.os.Message
import android.text.format.DateUtils
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
import java.time.Instant
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId

const val MSG_UPDATE_TIME = 1
const val GRACE_FOR_UPDATE = 200L
const val DEBUG_FENCES_PATH = "${Const.DATA_PATH}/${Const.DATA_KEY_DEBUG_FENCES}"
const val DEBUG_TIME_OFFSET_PATH = "${Const.DATA_PATH}/${Const.DATA_KEY_DEBUG_TIME_OFFSET}"

class DebugToolsFragment(a : WrappedFragment.Args, private val mWear : Wear) : WrappedFragment(a.inflater.inflate(R.layout.fragment_debug_tools, a.container, false)), View.OnClickListener, NumberPicker.OnValueChangeListener, TimePicker.OnTimeChangedListener
{
  private val mFragment = a.fragment
  private var mOffset = 0L
  private var mTicking = false
  private var mTime1 = System.currentTimeMillis()
  private var mTime2 = System.currentTimeMillis()
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
      mWear.getData("${Const.LOCATION_PATH}/千住大橋") { _, data ->
        a.fragment.activity.runOnUiThread { mView.findViewById<TextView>(R.id.text_data_state).text = data.getBoolean(Const.DATA_KEY_INSIDE).toString() }
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
    mTime1 = System.currentTimeMillis() + mOffset
    val zdt = OffsetDateTime.ofInstant(Instant.ofEpochMilli(mTime1), ZoneId.systemDefault())
    mHoursUI.value = zdt.hour
    mMinutesUI.value = zdt.minute
    mSecondsUI.value = zdt.second

    mTime2 = System.currentTimeMillis()
    mOffsetLabel.text = java.lang.Long.toString(mOffset)
    mTicking = false
  }

  private fun baseDay(timestamp : Long, utcOffset : Long) : Long = (timestamp + utcOffset * 1000) / DateUtils.DAY_IN_MILLIS

  fun onWearDataUpdated(path : String, data : DataMap)
  {
    when (path)
    {
      DEBUG_TIME_OFFSET_PATH -> withoutListeners {
        mOffset = data[Const.DATA_KEY_DEBUG_TIME_OFFSET]
        val now = System.currentTimeMillis()
        mTime1 = now
        val utcOffset = Const.MILLISECONDS_TO_UTC
        mDaysOffsetUI.value = (baseDay(now + mOffset, utcOffset) - baseDay(now, utcOffset)).toInt()
        tick()
      }
      DEBUG_FENCES_PATH -> withoutListeners {
          val fences = data.get<Long>(Const.DATA_KEY_DEBUG_FENCES).toInt()
          for (i in mFenceUIs.indices) mFenceUIs[i].isChecked = 0 != (fences and (1 shl i))
      }
    }
  }

  private fun updateOffset(grace : Long)
  {
    if (mTicking) return
    mHandler.removeMessages(MSG_UPDATE_TIME)
    mTime2 = System.currentTimeMillis()
    mTime1 = OffsetDateTime.now().with(LocalTime.of(mHoursUI.value, mMinutesUI.value, mSecondsUI.value)).toEpochSecond() * 1000
    val dayOffset = mDaysOffsetUI.value
    mTime1 += dayOffset * 86400000 - grace
    mOffset = mTime1 - mTime2
    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, grace)
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
        updateOffset(0L)
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
