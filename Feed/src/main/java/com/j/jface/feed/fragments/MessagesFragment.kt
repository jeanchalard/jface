package com.j.jface.feed.fragments

import android.text.*
import android.text.style.ForegroundColorSpan
import android.widget.EditText
import com.google.android.gms.wearable.DataMap
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.feed.views.PaletteView
import com.j.jface.feed.views.SnackbarRegistry
import com.j.jface.firebase.Firebase
import com.j.jface.lifecycle.WrappedFragment
import com.j.jface.wear.Wear
import java.util.*
import java.util.concurrent.Semaphore

/**
 * A fragment for showing and managing arbitrary messages.
 */
const val MESSAGE_PATH = "${Const.DATA_PATH}/${Const.DATA_KEY_USER_MESSAGE}"

class MessagesFragment(a : WrappedFragment.Args, private val mWear : Wear) : WrappedFragment(a.inflater.inflate(R.layout.fragment_messages, a.container, false)), TextWatcher, PaletteView.OnColorSetListener
{
  private val mF = a.fragment
  private val mUserMessageDataEdit : EditText = mView.findViewById(R.id.messagesFragment_userMessageDataEdit)
  // Very ugly but I'm not sure how else to deal with user vs auto updates. Not even sure it's right to
  // count this versus a simple volatile boolean, because I'm not positive updates can't actually be
  // batched :/
  private val expectedUpdatesCount = Semaphore(0)
  private val mPalette : PaletteView
  private val mWearUpdateListener = object : Firebase.WearDataUpdateListener() {
    override fun onWearDataUpdated(path: String, data: DataMap) = this@MessagesFragment.onWearDataUpdated(path, data)
  }

  init
  {
    mUserMessageDataEdit.addTextChangedListener(this)
    mUserMessageDataEdit.setTextColor(Const.USER_MESSAGE_DEFAULT_COLOR)
    mPalette = mView.findViewById(R.id.messagesFragment_palette)
    mPalette.addOnColorSetListener(this)
  }

  fun onWearDataUpdated(path : String, dataMap : DataMap)
  {
    if (path != MESSAGE_PATH) return
    val activity = mF.activity
    activity.runOnUiThread {
      val oldDistToEnd = (mUserMessageDataEdit.text?.length ?: 0) - mUserMessageDataEdit.selectionStart
      val userMessage = dataMap.getString(Const.DATA_KEY_USER_MESSAGE)
      if (null == userMessage) return@runOnUiThread
      val starts = getLineStartOffsets(userMessage!!)
      val text = SpannableString(userMessage)
      val colors = dataMap.getIntegerArrayList(Const.DATA_KEY_USER_MESSAGE_COLORS)
      if (null != colors && starts.size == colors.size)
        for (i in colors.indices)
        {
          val start = starts[i]
          val end = Math.max(start, if (i + 1 >= starts.size) userMessage.length else starts[i + 1])
          text.setSpan(ForegroundColorSpan(colors[i]), start, end, Spanned.SPAN_PARAGRAPH)
        }
      expectedUpdatesCount.release()
      mUserMessageDataEdit.setText(text)
      mUserMessageDataEdit.setSelection(text.length - oldDistToEnd)
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

  override fun afterTextChanged(s : Editable)
  {
    if (expectedUpdatesCount.tryAcquire()) return // We were expecting an update.
    val starts = getLineStartOffsets(s.toString())
    val colors = ArrayList<Int>(starts.size)
    for (i in starts.indices) colors.add(Const.USER_MESSAGE_DEFAULT_COLOR)
    for (span in s.getSpans(0, s.length - 1, ForegroundColorSpan::class.java))
    {
      val spanStart = s.getSpanStart(span)
      val spanEnd = s.getSpanEnd(span)
      val index = Arrays.binarySearch(starts, spanStart)
      if (index >= 0 && spanStart != spanEnd)
      {
        colors[index] = span.foregroundColor
        val expectedEnd = if (index + 1 >= starts.size) s.length else starts[index + 1]
        if (spanEnd != expectedEnd)
        {
          s.removeSpan(span)
          s.setSpan(span, spanStart, expectedEnd, Spanned.SPAN_PARAGRAPH)
        }
      }
      else
        s.removeSpan(span) // Removed the new line on which this was anchored
    }
    val dataMap = DataMap()
    dataMap.putString(Const.DATA_KEY_USER_MESSAGE, mUserMessageDataEdit.text.toString())
    dataMap.putIntegerArrayList(Const.DATA_KEY_USER_MESSAGE_COLORS, colors)
    mWear.putDataToCloud(Const.DATA_PATH + "/" + Const.DATA_KEY_USER_MESSAGE, dataMap)
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

  override fun beforeTextChanged(s : CharSequence, start : Int, count : Int, after : Int)
  {
  }

  override fun onTextChanged(s : CharSequence, start : Int, before : Int, count : Int)
  {
  }

  override fun onColorSet(color : Int)
  {
    try
    {
      val text = mUserMessageDataEdit.text
      val cursorStart = Selection.getSelectionStart(text)
      val cursorEnd = Selection.getSelectionEnd(text)

      var start = 0
      var end = 0
      val lines = text.toString().split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
      for (line in lines)
      {
        end = start + line.length + 1
        if (cursorStart < end && cursorEnd >= start)
        {
          for (span in text.getSpans(0, text.length - 1, ForegroundColorSpan::class.java))
            if (text.getSpanStart(span) == start)
              text.removeSpan(span)
          text.setSpan(ForegroundColorSpan(color), start, Math.min(end, text.length), Spannable.SPAN_PARAGRAPH)
        }
        start = end
      }
      afterTextChanged(text)
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
}
