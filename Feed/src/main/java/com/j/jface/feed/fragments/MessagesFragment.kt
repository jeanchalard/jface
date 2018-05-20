package com.j.jface.feed.fragments

import android.app.Activity
import android.app.Fragment
import android.text.Editable
import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.widget.EditText

import com.google.android.gms.wearable.DataMap
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.feed.views.PaletteView
import com.j.jface.feed.views.SnackbarRegistry
import com.j.jface.lifecycle.WrappedFragment
import com.j.jface.wear.Wear

import java.util.ArrayList
import java.util.Arrays

/**
 * A fragment for showing and managing arbitrary messages.
 */
class MessagesFragment(a : WrappedFragment.Args, private val mWear : Wear) : WrappedFragment(a.inflater.inflate(R.layout.fragment_messages, a.container, false)), TextWatcher, PaletteView.OnColorSetListener
{
  private val mF : Fragment
  private var mWearData : DataMap
  private val mUserMessageDataEdit : EditText
  private val mPalette : PaletteView

  init
  {
    mF = a.fragment
    mUserMessageDataEdit = mView.findViewById(R.id.messagesFragment_userMessageDataEdit)
    mUserMessageDataEdit.addTextChangedListener(this)
    mUserMessageDataEdit.setTextColor(Const.USER_MESSAGE_DEFAULT_COLOR)
    mPalette = mView.findViewById(R.id.messagesFragment_palette)
    mPalette.addOnColorSetListener(this)
    mWearData = DataMap()
    val activity = a.fragment.activity
    mWear.getData(Const.DATA_PATH + "/" + Const.DATA_KEY_USER_MESSAGE) { path, dataMap ->
      activity.runOnUiThread {
        mWearData = dataMap
        val userMessage = dataMap.getString(Const.DATA_KEY_USER_MESSAGE)
        if (null == userMessage) return@activity.runOnUiThread
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
        mUserMessageDataEdit.setText(text)
      }
      Unit
    }
  }

  override fun onResume()
  {
    SnackbarRegistry.setSnackbarParent(mView)
  }

  override fun onPause()
  {
    SnackbarRegistry.unsetSnackbarParent(mView)
  }

  override fun afterTextChanged(s : Editable)
  {
    if (!mDataFetched) return
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
    if (s.length <= 0) return IntArray(0)
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
