package com.j.jface.feed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedReceiver extends BroadcastReceiver
{
  @Override public void onReceive(final Context context, final Intent intent)
  {
    final Intent i = new Intent(context, GeofenceTransitionReceiverService.class);
    i.setAction(Intent.ACTION_BOOT_COMPLETED);
    context.startService(i);
  }
}
