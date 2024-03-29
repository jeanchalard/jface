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
import android.provider.MediaStore.Images.Media.getBitmap
import androidx.annotation.WorkerThread
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

fun DataItemBuffer?.singleToDataMap() = if (null != this && this.count == 1) DataMapItem.fromDataItem(this[0]).dataMap else DataMap()

class Wear(val context : Context)
{
  companion object { private val ex = Executors.newSingleThreadExecutor() }
  private val dataClient = Wearable.getDataClient(context)
  private val hasConnectedNodes : Task<Boolean> = Wearable.getNodeClient(context).connectedNodes.continueWith { it.isSuccessful && !it.result.isNullOrEmpty() }

  // Util
  private inline fun String.toWearUri() : Uri = PutDataMapRequest.create(this).uri
  private inline fun <T> Task<T>.orNull() : T? = if (this.isSuccessful) this.result else null

  // Live updates
  fun addListener(f : DataClient.OnDataChangedListener) = dataClient.addListener(f)
  fun removeListener(f : DataClient.OnDataChangedListener) = dataClient.removeListener(f)

  // Gets
  private inline fun getData(path : String) : Task<DataMap>
  {
    return dataClient.getDataItems(path.toWearUri()).continueWith(ex, Continuation
    {
      if (it.isSuccessful)
        it.result.let { dataItems -> dataItems.singleToDataMap().also { dataItems?.release() } }
      else
        DataMap()
    })
  }
  private inline fun getBitmap(path : String, key : String) : Task<Bitmap>
  {
    return getData(path).continueWith(ex, Continuation {
      BitmapFactory.decodeStream(dataClient.getFdForAsset(it.result!!.getAsset(key)!!).await().inputStream) // If no asset or something, throw which makes the task fail
    })
  }
  fun getData(path : String, callback : (String, DataMap) -> Unit) = getData(path).addOnCompleteListener(ex) {
    callback(path, (if (it.isSuccessful) it.result else null) ?: DataMap())
  }
  fun getDataSynchronously(path : String) : DataMap = getData(path).await()
  fun getBitmap(path : String, key :String, callback : (String, String, Bitmap?) -> Unit) = getBitmap(path, key).addOnCompleteListener(ex) { callback(path, key, it.orNull()) }
  fun getNodeName(context : Context, callback : (String) -> Unit) = Wearable.getNodeClient(context).localNode.addOnCompleteListener {
    val result = if (it.isSuccessful) it.result else null
    callback(if (null == result) "Error getting node name" else (result.id ?: "") + (result.displayName ?: ""))
  }

  // Puts
  private inline fun put(path : String, locally : Boolean = true, toCloud : Boolean = true, f : (DataMap) -> Unit)
  {
    val data = PutDataMapRequest.create(path).apply { f(dataMap); setUrgent() }
    if (locally) dataClient.putDataItem(data.asPutDataRequest())
    if (!toCloud or !Firebase.isLoggedIn) return
    // If this device is the master node, there is no point in sending an FCM message anyway.
    // hasConnectedNodes is a Task that almost certainly completed by now, but if not well
    // this will just send the message anyway which is harmless. The reason to test for isComplete
    // here is to guarantee result() will not block, not that it would be problematic but this
    // will crash if called on the main thread and would block.
    if (hasConnectedNodes.isComplete && hasConnectedNodes.result == true)
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
    Log.e("SCHEDULE", jobScheduler.schedule(job).toString())
  }

  fun putDataToCloudOnly(path : String, key : String, v : String)     = put(path, locally = false) { map -> map.putString(key, v) }
  fun putDataToCloud(path : String,               v : DataMap)        = put(path) { map -> map.putAll(v) }
  fun putDataToCloud(path : String, key : String, v : String)         = put(path) { map -> map.putString(key, v) }
  fun putDataToCloud(path : String, key : String, v : Boolean)        = put(path) { map -> map.putBoolean(key, v) }
  fun putDataToCloud(path : String, key : String, v : Long)           = put(path) { map -> map.putLong(key, v) }
  fun putDataToCloud(path : String, key : String, v : Asset)          = put(path) { map -> map.putAsset(key, v) }
  fun putDataToCloud(path : String, key : String, v : ArrayList<Int>) = put(path) { map -> map.putIntegerArrayList(key, v) }
  fun putDataLocally(path : String,               v : DataMap)        = put(path, toCloud = false) { map -> map.putAll(v) }
  fun putDataLocally(path : String, key : String, v : String)         = put(path, toCloud = false) { map -> map.putString(key, v) }
  fun putDataLocally(path : String, key : String, v : Boolean)        = put(path, toCloud = false) { map -> map.putBoolean(key, v) }
  fun putDataLocally(path : String, key : String, v : Long)           = put(path, toCloud = false) { map -> map.putLong(key, v) }
  fun putDataLocally(path : String, key : String, v : Asset)          = put(path, toCloud = false) { map -> map.putAsset(key, v) }
  fun putDataLocally(path : String, key : String, v : ArrayList<Int>) = put(path, toCloud = false) { map -> map.putIntegerArrayList(key, v) }

  // Deletes
  fun deleteData(uri : Uri) = dataClient.deleteDataItems(uri)
  fun deleteData(path : String) = deleteData(path.toWearUri())
  fun deleteAllData() = dataClient.dataItems.addOnCompleteListener(ex) { it.result?.forEach { dataClient.deleteDataItems(it.uri) } }
}
