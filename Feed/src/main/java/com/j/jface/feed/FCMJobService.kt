package com.j.jface.feed

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.j.jface.Const
import com.j.jface.firebase.Firebase
import kotlin.concurrent.thread

const val SENTINEL_PATH = "JOrg/jface/Conf/Conf/Sentinel"
const val SENTINEL_KEY = "sentinelTimestamp"

class FCMJobService : JobService()
{
  override fun onStartJob(params : JobParameters?) : Boolean
  {
    Log.e("START JOB", "" + params)
    if (null == params) return true // true means work is finished
    thread(name = "FCM job thread") {
      val path = params.extras.getString(Const.EXTRA_PATH)
      val success = try
      {
        Tasks.await(tryForceSyncDB())
        FCMHandler.sendFCMMessageForWearPathNow(path)
      }
      catch(e : Exception)
      {
        Log.e("Couldn't sync after network is up, retrying later", "" + e)
        false
      }
      jobFinished(params, !success)
    }
    return false
  }

  override fun onStopJob(params : JobParameters?) : Boolean = false

  private fun tryForceSyncDB() : Task<Unit>
  {
    return Firebase.transaction { db, transaction ->
      val sentinel = db.document(SENTINEL_PATH)
      val value = transaction.get(sentinel).getLong(SENTINEL_KEY) ?: 0L
      transaction.update(db.document(), SENTINEL_KEY, value + 1)
      Unit
    }
  }
}
