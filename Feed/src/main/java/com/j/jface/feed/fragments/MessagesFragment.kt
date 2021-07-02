package com.j.jface.feed.fragments

import android.text.Editable
import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.widget.Button
import android.widget.EditText
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.j.jface.Const
import com.j.jface.Const.DATA_KEY_CHECKPOINTS
import com.j.jface.Const.DATA_KEY_HEART_MESSAGE
import com.j.jface.Const.DATA_KEY_USER_MESSAGE
import com.j.jface.Const.DATA_PATH
import com.j.jface.R
import com.j.jface.feed.views.PaletteView
import com.j.jface.feed.views.SnackbarRegistry
import com.j.jface.firebase.Firebase
import com.j.jface.lifecycle.WrappedFragment
import com.j.jface.wear.Wear
import java.util.ArrayList
import java.util.Arrays
import java.util.concurrent.Semaphore

/**
 * A fragment for showing and managing arbitrary messages.
 */
val MESSAGE_PATH = "${DATA_PATH}/${DATA_KEY_USER_MESSAGE}"
val HEART_PATH = "${DATA_PATH}/${DATA_KEY_HEART_MESSAGE}"

class MessagesFragment(a : WrappedFragment.Args, private val mWear : Wear) : WrappedFragment(a.inflater.inflate(R.layout.fragment_messages, a.container, false)), PaletteView.OnColorSetListener, DataClient.OnDataChangedListener {
  private val mF = a.fragment
  private val mUserMessageDataEdit : EditText = mView.findViewById(R.id.messagesFragment_userMessageDataEdit)
  private val mCheckpointsDataEdit : EditText = mView.findViewById(R.id.messagesFragment_checkpointsDataEdit)
  // Very ugly but I'm not sure how else to deal with user vs auto updates. Not even sure it's right to
  // count this versus a simple volatile boolean, because I'm not positive updates can't actually be
  // batched :/
  private val expectedMessageUpdatesCount = Semaphore(0)
  private val expectedCheckpointsUpdatesFromUI = Semaphore(0)
  private val expectedCheckpointsUpdatesFromWear = Semaphore(0)
  private val mPalette : PaletteView
  private val mWearUpdateListener = object : Firebase.WearDataUpdateListener()
  {
    override fun onWearDataUpdated(path : String, data : DataMap) = this@MessagesFragment.onWearDataUpdated(path, data)
  }
  private val userMessageUpdater = object : TextWatcher
  {
    override fun afterTextChanged(s : Editable)
    {
      if (expectedMessageUpdatesCount.tryAcquire()) return // Was updated from a different Wear node
      val starts = getLineStartOffsets(s.toString())
      val colors = ArrayList<Int>(starts.size)
      for (i in starts.indices) colors.add(Const.USER_MESSAGE_DEFAULT_COLOR)
      for (span in s.getSpans(0, s.length - 1, ForegroundColorSpan::class.java)) {
        val spanStart = s.getSpanStart(span)
        val spanEnd = s.getSpanEnd(span)
        val index = Arrays.binarySearch(starts, spanStart)
        if (index >= 0 && spanStart != spanEnd) {
          colors[index] = span.foregroundColor
          val expectedEnd = if (index + 1 >= starts.size) s.length else starts[index + 1]
          if (spanEnd != expectedEnd) {
            s.removeSpan(span)
            s.setSpan(span, spanStart, expectedEnd, Spanned.SPAN_PARAGRAPH)
          }
        } else
          s.removeSpan(span) // Removed the new line on which this was anchored
      }
      val dataMap = DataMap()
      dataMap.putString(DATA_KEY_USER_MESSAGE, mUserMessageDataEdit.text.toString())
      dataMap.putIntegerArrayList(Const.DATA_KEY_USER_MESSAGE_COLORS, colors)
      mWear.putDataToCloud("${DATA_PATH}/${DATA_KEY_USER_MESSAGE}", dataMap)
    }

    override fun beforeTextChanged(s : CharSequence, start : Int, count : Int, after : Int) {}
    override fun onTextChanged(s : CharSequence, start : Int, before : Int, count : Int) {}
  }
  private val checkpointsUpdater = object : TextWatcher
  {
    override fun afterTextChanged(s : Editable)
    {
      if (expectedCheckpointsUpdatesFromWear.tryAcquire()) return // We were expecting an update.
      expectedCheckpointsUpdatesFromUI.release()
      mWear.putDataToCloud("${DATA_PATH}/${DATA_KEY_CHECKPOINTS}", DataMap().apply { putString(DATA_KEY_CHECKPOINTS, mCheckpointsDataEdit.text.toString()) })
    }
    override fun beforeTextChanged(s : CharSequence, start : Int, count : Int, after : Int) {}
    override fun onTextChanged(s : CharSequence, start : Int, before : Int, count : Int) {}
  }

  init
  {
    mUserMessageDataEdit.addTextChangedListener(userMessageUpdater)
    mUserMessageDataEdit.setTextColor(Const.USER_MESSAGE_DEFAULT_COLOR)
    mCheckpointsDataEdit.addTextChangedListener(checkpointsUpdater)
    mPalette = mView.findViewById(R.id.messagesFragment_palette)
    mPalette.addOnColorSetListener(this)
    mView.findViewById<Button>(R.id.messagesFragment_setHMessage).setOnClickListener { sendHeartMessage() }
    mWear.getData("${DATA_PATH}/${DATA_KEY_CHECKPOINTS}", ::onWearDataUpdated)
  }

  fun onWearDataUpdated(path : String, dataMap : DataMap)
  {
    when (path)
    {
      MESSAGE_PATH                           -> onMessageUpdated(dataMap)
      "${DATA_PATH}/${DATA_KEY_CHECKPOINTS}" -> onCheckpointsUpdated(dataMap[DATA_KEY_CHECKPOINTS] ?: "")
    }
  }

  private fun onMessageUpdated(dataMap : DataMap)
  {
    mF.activity!!.runOnUiThread {
      val userMessage = dataMap.getString(DATA_KEY_USER_MESSAGE)
      val starts = getLineStartOffsets(userMessage)
      val text = SpannableString(userMessage)
      val colors = dataMap.getIntegerArrayList(Const.DATA_KEY_USER_MESSAGE_COLORS)
      if (null != colors && starts.size == colors.size)
        for (i in colors.indices) {
          val start = starts[i]
          val end = Math.max(start, if (i + 1 >= starts.size) userMessage.length else starts[i + 1])
          text.setSpan(ForegroundColorSpan(colors[i]), start, end, Spanned.SPAN_PARAGRAPH)
        }
      expectedMessageUpdatesCount.release()
      mUserMessageDataEdit.setTextKeepState(text)
    }
  }

  private fun onCheckpointsUpdated(checkpoints : String)
  {
    if (expectedCheckpointsUpdatesFromUI.tryAcquire()) return // Was updated from the UI of this very fragment
    mF.activity!!.runOnUiThread {
      expectedCheckpointsUpdatesFromWear.release()
      mCheckpointsDataEdit.setTextKeepState(checkpoints)
    }
  }

  override fun onResume()
  {
    SnackbarRegistry.setSnackbarParent(mView)
    mWearUpdateListener.resume()
    mWear.addListener(this)
  }

  override fun onPause()
  {
    SnackbarRegistry.unsetSnackbarParent(mView)
    mWearUpdateListener.pause()
    mWear.removeListener(this)
  }

  override fun onDestroy()
  {
    mPalette.removeOnColorSetListener(this)
  }

  private fun getLineStartOffsets(s : String) : IntArray
  {
    if (s.isEmpty()) return IntArray(0)
    val lines = s.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    val starts = IntArray(lines.size)
    var start = 0
    for (i in lines.indices)
    {
      starts[i] = start
      start += lines[i].length + 1
    }
    return starts
  }

  override fun onColorSet(color : Int)
  {
    try
    {
      val text = mUserMessageDataEdit.text
      val cursorStart = Selection.getSelectionStart(text)
      val cursorEnd = Selection.getSelectionEnd(text)

      var start = 0
      val lines = text.toString().split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
      for (line in lines)
      {
        val end = start + line.length + 1
        if (cursorStart < end && cursorEnd >= start)
        {
          for (span in text.getSpans(0, text.length - 1, ForegroundColorSpan::class.java))
            if (text.getSpanStart(span) == start)
              text.removeSpan(span)
          text.setSpan(ForegroundColorSpan(color), start, Math.min(end, text.length), Spannable.SPAN_PARAGRAPH)
        }
        start = end
      }
      userMessageUpdater.afterTextChanged(text)
    }
    catch (e : Exception)
    {
      removeAllSpans()
    }
  }

  private fun removeAllSpans()
  {
    val text = mUserMessageDataEdit.text
    for (span in text.getSpans(0, text.length - 1, ForegroundColorSpan::class.java)) text.removeSpan(span)
  }

  private fun sendHeartMessage()
  {
    val text = mView.findViewById<EditText>(R.id.messagesFragment_hMessage).text
    if (text.isEmpty()) return
    val dataMap = DataMap()
    dataMap.putString(DATA_KEY_HEART_MESSAGE, text.toString())
    mWear.putDataToCloud("${DATA_PATH}/${DATA_KEY_HEART_MESSAGE}", dataMap)
  }

  override fun onDataChanged(eb : DataEventBuffer)
  {
    eb.forEach {
      when (it.dataItem.uri.path)
      {
        "${DATA_PATH}/${DATA_KEY_CHECKPOINTS}" -> {
          if (DataEvent.TYPE_DELETED == it.type) onCheckpointsUpdated("")
          else onCheckpointsUpdated(DataMapItem.fromDataItem(it.dataItem).dataMap[DATA_KEY_CHECKPOINTS])
        }
      }
    }
  }
}
