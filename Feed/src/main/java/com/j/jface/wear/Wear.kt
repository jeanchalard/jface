@file:Suppress("NOTHING_TO_INLINE")

package com.j.jface.wear

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.PersistableBundle
import android.support.annotation.WorkerThread
import android.util.Log
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.*
import com.j.jface.Const
import com.j.jface.feed.FCMJobService
import com.j.jface.firebase.Firebase
import com.j.jface.firebase.await
import com.j.jface.toPersistableBundle
import java.util.concurrent.Executor
import java.util.concurrent.Executors

inline fun <T, U> Task<T>.addOnCompleteListener(e : Executor, crossinline l : (Task<T>) -> U) : Task<T> = addOnCompleteListener(e, OnCompleteListener<T> { l(it) })

class Wear(val context : Context)
{
  companion object { private val ex = Executors.newSingleThreadExecutor() }
  private val dataClient = Wearable.getDataClient(context)
  private val hasConnectedNodes : Task<Boolean> = Wearable.getNodeClient(context).connectedNodes.continueWith { it.isSuccessful && null != it.result && !it.result.isEmpty() }

  // Util
  private inline fun String.toWearUri() : Uri = PutDataMapRequest.create(this).uri
  private inline fun <T> Task<T>.orNull() : T? = if (this.isSuccessful) this.result else null

  // Gets
  private inline fun getData(path : String) : Task<DataMap>
  {
    return dataClient.getDataItems(path.toWearUri()).continueWith(ex, Continuation
    {
      val result : DataMap
      if (it.isSuccessful)
      {
        val dataItems = it.result
        result = if (dataItems.count == 1) DataMapItem.fromDataItem(dataItems[0]).dataMap else DataMap()
        dataItems.release()
      }
      else
        result = DataMap()
      result
    })
  }
  private inline fun getBitmap(path : String, key : String) : Task<Bitmap>
  {
    return getData(path).continueWith(ex, Continuation {
      BitmapFactory.decodeStream(dataClient.getFdForAsset(it.result.getAsset(key)!!).await().inputStream) // If no asset or something, throw which makes the task fail
    })
  }
  fun getData(path : String, callback : (String, DataMap) -> Unit) = getData(path).addOnCompleteListener(ex) { callback(path, if (it.isSuccessful) it.result else DataMap()) }
  fun getDataSynchronously(path : String) : DataMap = getData(path).await()
  fun getBitmap(path : String, key :String, callback : (String, String, Bitmap?) -> Unit) = getBitmap(path, key).addOnCompleteListener(ex) { callback(path, key, it.orNull()) }
  fun getNodeName(context : Context, callback : (String) -> Unit) = Wearable.getNodeClient(context).localNode.addOnCompleteListener {
      callback(if (!it.isSuccessful) "Error getting node name" else it.result.run { (id ?: "") + (displayName ?: "") })
  }

  // Puts
  private inline fun put(path : String, toCloud : Boolean, f : (DataMap) -> Unit)
  {
    val data = PutDataMapRequest.create(path).apply { f(dataMap) }
    dataClient.putDataItem(data.asPutDataRequest())
    if (!toCloud or !Firebase.isLoggedIn) return
    // If this device is the master node, there is no point in sending an FCM message anyway.
    // hasConnectedNodes is a Task that almost certainly completed by now, but if not well
    // this will just send the message anyway which is harmless. The reason to test for isComplete
    // here is to guarantee result() will not block, not that it would be problematic but this
    // will crash if called on the main thread and would block.
    if (hasConnectedNodes.isComplete && hasConnectedNodes.result)
      Firebase.updateWearData(path, data.dataMap)
    else
      startUpdateWearDataJob(context, path, data.dataMap)
  }

  @WorkerThread
  private fun startUpdateWearDataJob(c : Context, path : String, dataMap : DataMap)
  {
    val job = JobInfo.Builder(path.hashCode(), ComponentName(c, FCMJobService::class.java))
     .setExtras(PersistableBundle().apply {
       putString(Const.EXTRA_PATH, path)
       putPersistableBundle(Const.EXTRA_WEAR_DATA, dataMap.toPersistableBundle())
     })
     .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
     .setBackoffCriteria(5000, JobInfo.BACKOFF_POLICY_EXPONENTIAL) // retry after 5s on first failure, then exponential backoff
     .setPersisted(true)
     .build()
    val jobScheduler = c.getSystemService(JobScheduler::class.java)
    Log.e("SCHEDULE", Integer.toString(jobScheduler.schedule(job)))
  }

  fun putDataToCloud(path : String,               v : DataMap)        = put(path, true) { map -> map.putAll(v) }
  fun putDataToCloud(path : String, key : String, v : String)         = put(path, true) { map -> map.putString(key, v) }
  fun putDataToCloud(path : String, key : String, v : Boolean)        = put(path, true) { map -> map.putBoolean(key, v) }
  fun putDataToCloud(path : String, key : String, v : Long)           = put(path, true) { map -> map.putLong(key, v) }
  fun putDataToCloud(path : String, key : String, v : Asset)          = put(path, true) { map -> map.putAsset(key, v) }
  fun putDataToCloud(path : String, key : String, v : ArrayList<Int>) = put(path, true) { map -> map.putIntegerArrayList(key, v) }
  fun putDataLocally(path : String,               v : DataMap)        = put(path, false) { map -> map.putAll(v) }
  fun putDataLocally(path : String, key : String, v : String)         = put(path, false) { map -> map.putString(key, v) }
  fun putDataLocally(path : String, key : String, v : Boolean)        = put(path, false) { map -> map.putBoolean(key, v) }
  fun putDataLocally(path : String, key : String, v : Long)           = put(path, false) { map -> map.putLong(key, v) }
  fun putDataLocally(path : String, key : String, v : Asset)          = put(path, false) { map -> map.putAsset(key, v) }
  fun putDataLocally(path : String, key : String, v : ArrayList<Int>) = put(path, false) { map -> map.putIntegerArrayList(key, v) }

  // Deletes
  fun deleteData(uri : Uri) = dataClient.deleteDataItems(uri)
  fun deleteData(path : String) = deleteData(path.toWearUri())
  fun deleteAllData() = dataClient.dataItems.addOnCompleteListener(ex) { for (item in it.result) dataClient.deleteDataItems(item.uri) }
}
