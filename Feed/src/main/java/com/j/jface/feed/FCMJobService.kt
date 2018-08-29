package com.j.jface.feed

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import com.j.jface.Const
import com.j.jface.firebase.Firebase
import com.j.jface.lifecycle.CommonObjects
import com.j.jface.toDataMap
import com.j.jface.wear.addOnCompleteListener

class FCMJobService : JobService()
{
  override fun onStartJob(params : JobParameters?) : Boolean
  {
    Log.e("START JOB", "" + params)
    if (null == params) return true // true means work is finished
    CommonObjects.executor.execute {
      val success = try
      {
        val path = params.extras.getString(Const.EXTRA_PATH)
        val dataMap = params.extras.getPersistableBundle(Const.EXTRA_WEAR_DATA).toDataMap()
        val future = Firebase.updateWearData(path, dataMap)
        // If the message does not seem to arrive, do check the FCM server key is
        // correctly stored in /JOrg/jface/Conf/Conf/key. The value is in the
        // Firebase console, Gear icon > Settings > Cloud messaging > Server key
        future.addOnCompleteListener(CommonObjects.executor) { FCMHandler.sendFCMMessageForWearPathNow(path) }
        true // Success
      }
      catch (e : Exception)
      {
        Log.e("Couldn't sync after network is up, retrying later", "" + e)
        false
      }
      jobFinished(params, !success)
    }
    return false
  }

  override fun onStopJob(params : JobParameters?) : Boolean = false
}
