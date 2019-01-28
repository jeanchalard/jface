package com.j.jface.feed

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.j.jface.Const
import com.j.jface.firebase.Firebase
import com.j.jface.wear.Wear

class FCMReceiverService : FirebaseMessagingService()
{
  val wear : Wear by lazy { Wear(this) }

  override fun onMessageReceived(msg : RemoteMessage?)
  {
    if (null == msg) return
    Log.e("Firebase message", "" + msg.data)
    if (!Firebase.isLoggedIn) { Log.e("...but", "firebase not logged in ?!"); return }
    val path = msg.data[Const.FIREBASE_MESSAGE_WEAR_PATH]
    if (null == path) { Log.e("...but", "Message is ill-formed, no ${Const.FIREBASE_MESSAGE_WEAR_PATH} field : " + msg); return }
    Firebase.getWearData(path).addOnCompleteListener {
      wear.putDataLocally(path, it.result)
    }
  }
}
