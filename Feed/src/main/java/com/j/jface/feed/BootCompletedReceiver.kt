package com.j.jface.feed

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

class BootCompletedReceiver : BroadcastReceiver()
{
  override fun onReceive(context : Context, intent : Intent)
  {
    if (intent.action != Intent.ACTION_BOOT_COMPLETED) return // Security suggestion from linter : spoofing countermeasure

//    val job = JobInfo.Builder(path.hashCode(), ComponentName(context, FCMJobService::class.java))
//     .setPersisted(true)
//     .build()
//    val jobScheduler = c.getSystemService(JobScheduler::class.java)
//    Log.e("SCHEDULE", "" + jobScheduler.schedule(job))

    val i = Intent(context, GeofenceTransitionReceiverService::class.java)
    i.action = Intent.ACTION_BOOT_COMPLETED
    context.startService(i)
  }
}
