package com.j.jface.feed

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.j.jface.firebase.Firebase

class FCMReceiverService : FirebaseMessagingService()
{
  override fun onMessageReceived(msg : RemoteMessage?)
  {
    if (null == msg) return
    Log.e("Firebase message", "" + msg)
    if (!Firebase.isLoggedIn()) { Log.e("...but", "firebase not logged in ?!"); return }
  }
}
