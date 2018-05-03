package com.j.jface.wear

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.*
import com.j.jface.firebase.await
import java.util.concurrent.Executor
import java.util.concurrent.Executors

inline fun <T, U> Task<T>.addOnCompleteListener(e : Executor, crossinline l : (Task<T>) -> U) : Task<T> = addOnCompleteListener(e, OnCompleteListener<T> { l(it) })

class Wear(val context : Context)
{
  companion object { private val ex = Executors.newSingleThreadExecutor() }
  private val dataClient = Wearable.getDataClient(context)

  // Util
  private inline fun String.toWearUri() : Uri = PutDataMapRequest.create(this).uri
  private inline infix fun <T, R> Task<T>.then(crossinline d : (T) -> R) : Task<R> { return this.continueWith(ex, Continuation<T, R> { d(it.result) }) }
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
      callback(if (null == it || !it.isSuccessful) "Error getting node name" else it.result.run { (id ?: "") + (displayName ?: "") })
  }

  // Puts
  private inline fun put(path : String, f : (DataMap) -> Unit) = dataClient.putDataItem(PutDataMapRequest.create(path).apply{f(dataMap)}.asPutDataRequest())
  fun putData(path : String,               v : DataMap)        = put(path) { map -> map.putAll(v) }
  fun putData(path : String, key : String, v : String)         = put(path) { map -> map.putString(key, v) }
  fun putData(path : String, key : String, v : Boolean)        = put(path) { map -> map.putBoolean(key, v) }
  fun putData(path : String, key : String, v : Long)           = put(path) { map -> map.putLong(key, v) }
  fun putData(path : String, key : String, v : Asset)          = put(path) { map -> map.putAsset(key, v) }
  fun putData(path : String, key : String, v : ArrayList<Int>) = put(path) { map -> map.putIntegerArrayList(key, v) }

  // Deletes
  fun deleteData(uri : Uri) = dataClient.deleteDataItems(uri)
  fun deleteData(path : String) = deleteData(path.toWearUri())
  fun deleteAllData() = dataClient.dataItems.addOnCompleteListener(ex) { for (item in it.result) dataClient.deleteDataItems(item.uri) }
}
