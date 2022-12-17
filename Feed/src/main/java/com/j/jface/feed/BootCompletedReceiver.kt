package com.j.jface.feed

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompletedReceiver : BroadcastReceiver()
{
  override fun onReceive(context : Context, intent : Intent)
  {
    if (intent.action != Intent.ACTION_BOOT_COMPLETED) return // Security suggestion from linter : spoofing countermeasure

    val i = Intent(context, GeofenceTransitionReceiver::class.java)
    i.action = Intent.ACTION_BOOT_COMPLETED
    GeofenceTransitionReceiver().onReceive(context, i)
  }
}
