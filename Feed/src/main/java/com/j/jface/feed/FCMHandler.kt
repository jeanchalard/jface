package com.j.jface.feed

import android.content.Context
import android.support.annotation.WorkerThread
import android.util.Log
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.Wearable
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import com.j.jface.Const
import com.j.jface.firebase.Firebase
import org.json.JSONObject
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

const val FCMURL = "https://fcm.googleapis.com/fcm/send"

class FCMHandler : FirebaseInstanceIdService()
{
  companion object
  {
    private fun getConnectionDataForPath(path : String) : Pair<HttpURLConnection, String>?
    {
      val (key, listeners) = Firebase.getAccessKeyAndWearListenersSynchronously()
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
      try {
        val (cx, payload) = getConnectionDataForPath(path) ?: return false
        OutputStreamWriter(cx.outputStream).use { it.write(payload) }
        cx.connect()
        Log.i("Firebase messaging → code : ", "" + cx.responseCode)
        InputStreamReader(cx.inputStream).use { Log.i("FCM server response", it.readText()) }
        return true
      } catch (e : IOException) {
        return false
      }
    }

    fun registerTokenForWearData(context : Context)
    {
      val token = FirebaseInstanceId.getInstance().token
      Log.e("JOrg/Firebase", "Firebase decided to update its token : " + token)
      val nodeClient = Wearable.getNodeClient(context)
      nodeClient.connectedNodes.addOnSuccessListener {
        if (null == it || it.isEmpty()) return@addOnSuccessListener
        val d = DataMap().apply {
          putString(Const.CONFIG_KEY_WEAR_LISTENER_ID, token)
        }
        if (Firebase.isLoggedIn()) Firebase.updateWearData("${Const.CONFIG_PATH}/${Const.CONFIG_KEY_WEAR_LISTENER_PREFIX}${token}", d)
      }
    }
  }

  override fun onTokenRefresh() = registerTokenForWearData(this)
}
