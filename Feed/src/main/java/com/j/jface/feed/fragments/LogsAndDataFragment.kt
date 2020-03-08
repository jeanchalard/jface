package com.j.jface.feed.fragments

import android.app.Fragment
import android.view.View
import android.widget.TextView
import com.google.android.gms.wearable.DataMap
import com.j.jface.Const
import com.j.jface.Departure
import com.j.jface.R
import com.j.jface.feed.DataSource
import com.j.jface.feed.FeedLoader
import com.j.jface.lifecycle.WrappedFragment
import com.j.jface.wear.Wear
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class LogsAndDataFragment(a : WrappedFragment.Args, private val mWear : Wear) : WrappedFragment(a.inflater.inflate(R.layout.fragment_logs_and_data, a.container, false)), View.OnClickListener
{
  private val mF : Fragment = a.fragment
  private val mLog : TextView = mView.findViewById(R.id.log)

  init
  {
    mView.findViewById<View>(R.id.button_show).setOnClickListener(this)
    mView.findViewById<View>(R.id.button_load).setOnClickListener(this)
    mView.findViewById<View>(R.id.button_clear).setOnClickListener(this)
  }

  private fun showDataCallback(path : String, dataMap : DataMap)
  {
    mF.activity.runOnUiThread(Runnable {
      mLog.append(path)
      mLog.append("\n")
      if (path.endsWith(Const.DATA_PATH_SUFFIX_STATUS))
      {
        val lastUpdate = dataMap.getLong(Const.DATA_KEY_STATUS_UPDATE_DATE)
        if (0L == lastUpdate)
        {
          mLog.append(" Never updated\n")
          return@Runnable
        }
        val status = dataMap.getString(Const.DATA_KEY_LAST_STATUS)
        mLog.append(" Updated on " + OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        mLog.append("\n")
        mLog.append(" Status : " + status)
        mLog.append("\n")
        mLog.append(" Data last updated ")
        val updateDate = OffsetDateTime.ofInstant(Instant.ofEpochMilli(dataMap.getLong(Const.DATA_KEY_SUCCESSFUL_UPDATE_DATE)), ZoneId.systemDefault())
        mLog.append(updateDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
      } else
      {
        val departureList = dataMap.getDataMapArrayList(Const.DATA_KEY_DEPLIST) ?: return@Runnable
        for (map in departureList)
          mLog.append(Departure(map.getInt(Const.DATA_KEY_DEPTIME),
           map.getString(Const.DATA_KEY_EXTRA), "", null).toString() + "・")
        mLog.append("\n")
      }
    })
  }

  private fun retrieveStatus(wear : Wear)
  {
    for (ds in DataSource.ALL_SOURCES)
    {
      wear.getData(Const.DATA_PATH + "/" + ds.name + Const.DATA_PATH_SUFFIX_STATUS, ::showDataCallback)
      wear.getData(Const.DATA_PATH + "/" + ds.name, ::showDataCallback)
    }
  }

  override fun onClick(v : View)
  {
    when (v.id)
    {
      R.id.button_show  ->
      {
        mLog.text = ""
        retrieveStatus(mWear)
      }
      R.id.button_load  -> FeedLoader.startAllLoads(mWear)
      R.id.button_clear -> mWear.deleteAllData()
    }
  }
}
