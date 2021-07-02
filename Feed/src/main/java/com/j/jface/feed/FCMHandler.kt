package com.j.jface.feed

import android.content.Context
import androidx.annotation.WorkerThread
import android.util.Log
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.Wearable
import com.google.firebase.messaging.FirebaseMessaging
import com.j.jface.Const
import com.j.jface.firebase.Firebase
import com.j.jface.firebase.await
import org.json.JSONObject
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

const val FCMURL = "https://fcm.googleapis.com/fcm/send"

object FCMHandler
{
  private fun getConnectionDataForPath(path : String) : Pair<HttpURLConnection, String>?
  {
    val (key, listeners) = Firebase.getAccessKeyAndWearListenersSynchronously()
    Log.e("Sending to listeners", "${key} : " + listeners.joinToString(","))
    if (key.isEmpty() || listeners.isEmpty()) return null
    val payload = JSONObject().apply {
      put("to", listeners[0])
      put("priority", "high")
      put("data", JSONObject().apply { put(Const.FIREBASE_MESSAGE_WEAR_PATH, path) })
    }.toString()

    val url = URL(FCMURL)
    val cx = url.openConnection() as HttpURLConnection
    cx.doOutput = true
    cx.requestMethod = "POST"
    cx.setRequestProperty("Authorization", "key=${key}")
    cx.setRequestProperty("Content-Type", "application/json")
    cx.setFixedLengthStreamingMode(payload.length)
    return cx to payload
  }

  @WorkerThread fun sendFCMMessageForWearPathNow(path : String) : Boolean
  {
    Log.e("sendFCMMessageNow", "path = ${path}")
    try
    {
      val (cx, payload) = getConnectionDataForPath(path) ?: return false
      OutputStreamWriter(cx.outputStream).use { it.write(payload) }
      cx.connect()
      Log.e("Firebase messaging → code : ", "" + cx.responseCode)
      InputStreamReader(cx.inputStream).use { Log.i("FCM server response", it.readText()) }
      return true
    }
    catch (e : IOException)
    {
      return false
    }
  }

  fun registerTokenForWearData(context : Context)
  {
    val token = FirebaseMessaging.getInstance().token
    Log.e("JOrg/Firebase", "Firebase decided to update its token : ${token}")
    Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener {
      if (null == it || it.isEmpty()) return@addOnSuccessListener
      val d = DataMap()
      token.addOnCompleteListener { tokenId ->
        d.putString(Const.CONFIG_KEY_WEAR_LISTENER_ID, tokenId.result)
        if (Firebase.isLoggedIn) Firebase.updateWearData("${Const.CONFIG_PATH}/${Const.CONFIG_KEY_WEAR_LISTENER_PREFIX}${token}", d)
      }
    }
  }
}
