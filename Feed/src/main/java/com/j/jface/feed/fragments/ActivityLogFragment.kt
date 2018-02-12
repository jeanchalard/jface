package com.j.jface.feed.fragments

import android.app.Fragment
import android.widget.TextView
import com.google.android.gms.wearable.DataMap
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.action.GThread
import com.j.jface.lifecycle.WrappedFragment
import java.util.*

/**
 * A fragment for showing and managing the activity log.
 */
class ActivityLogFragment(a : WrappedFragment.Args, gThread : GThread) : WrappedFragment(a.inflater.inflate(R.layout.fragment_activity_log, a.container, false))
{
  private val mF : Fragment = a.fragment
  private val mLastActivityMnemonic : TextView = mView.findViewById(R.id.last_activity_mnemonic)
  private val mLastActivityStartTime : TextView = mView.findViewById(R.id.last_activity_start_time)

  init
  {
    gThread.getData(Const.ACTIVITY_PATH, ::showData)
  }

  private fun showData(path : String, dataMap : DataMap)
  {
    mF.activity.runOnUiThread {
      val mnemonic = dataMap.getString(Const.DATA_KEY_LAST_ACTIVITY_MNEMONIC)
      val startTime = dataMap.getLong(Const.DATA_KEY_LAST_ACTIVITY_START_TIME)
      mLastActivityMnemonic.text = mnemonic
      val cal = GregorianCalendar()
      cal.timeInMillis = startTime
      mLastActivityStartTime.text = ("" + cal.get(Calendar.YEAR) + "/" + cal.get(Calendar.MONTH) + "/" + cal.get(Calendar.DAY_OF_MONTH) + "  "
       + cal.get(Calendar.HOUR) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND))
    }
  }
}
