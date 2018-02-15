package com.j.jface.action.wear

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.*
import com.j.jface.action.addOnCompleteListener
import com.j.jface.action.continueWith
import com.j.jface.action.continueWithTask
import java.util.concurrent.Executor

private fun String.toWearUri() : Uri = PutDataMapRequest.create(this).uri

fun DeleteDataAction(uri : Uri) = { client : DataClient -> client.deleteDataItems(uri) }
fun DeleteDataAction(path : String) = DeleteDataAction(path.toWearUri())

fun DeleteAllDataAction(e : Executor) =
{ client : DataClient ->
  client.dataItems.addOnSuccessListener(e, OnSuccessListener { for (item in it) client.deleteDataItems(item.uri) })
}

fun GetDataAction(client : DataClient, e : Executor, path : String) : Task<DataMap>
{
  return client.getDataItems(path.toWearUri()).continueWith(e, Continuation
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

fun GetDataAction(e : Executor, path : String, callback : (String, DataMap) -> Unit) =
{ client : DataClient ->
  GetDataAction(client, e, path).addOnCompleteListener { callback(path, if (it.isSuccessful) it.result else DataMap()) }
}

fun GetBitmapAction(client : DataClient, e : Executor, path : String, key : String) : Task<Bitmap>
{
  return GetDataAction(client, e, path).continueWithTask(e)
  {
    val asset = it.result.getAsset(key)!! // If no asset or anything, throw which makes the task fail
    client.getFdForAsset(asset)
  }
  .continueWith(e)
  { it : DataClient.GetFdForAssetResponse ->
    BitmapFactory.decodeStream(it.inputStream)
  }
}

fun GetBitmapAction(e : Executor, path : String, key : String, callback : (String, String, Bitmap?) -> Unit) = { client : DataClient
->
  GetBitmapAction(client, e, path, key).addOnCompleteListener(e) { callback(path, key, if (it.isSuccessful) it.result else null) }
}

private fun PDA(client : DataClient, path : String, f : (DataMap) -> Unit) : Task<DataItem> = client.putDataItem(PutDataMapRequest.create(path).apply{f(dataMap)}.asPutDataRequest())

fun PutDataAction(client : DataClient, path : String,               v : DataMap)        = PDA(client, path) { map -> map.putAll(v) }
fun PutDataAction(client : DataClient, path : String, key : String, v : String)         = PDA(client, path) { map -> map.putString(key, v) }
fun PutDataAction(client : DataClient, path : String, key : String, v : Boolean)        = PDA(client, path) { map -> map.putBoolean(key, v) }
fun PutDataAction(client : DataClient, path : String, key : String, v : Long)           = PDA(client, path) { map -> map.putLong(key, v) }
fun PutDataAction(client : DataClient, path : String, key : String, v : Asset)          = PDA(client, path) { map -> map.putAsset(key, v) }
fun PutDataAction(client : DataClient, path : String, key : String, v : ArrayList<Int>) = PDA(client, path) { map -> map.putIntegerArrayList(key, v) }

fun PutDataAction(path : String,               v : DataMap)        = { client : DataClient -> PutDataAction(client, path, v) }
fun PutDataAction(path : String, key : String, v : String)         = { client : DataClient -> PutDataAction(client, path, key, v) }
fun PutDataAction(path : String, key : String, v : Boolean)        = { client : DataClient -> PutDataAction(client, path, key, v) }
fun PutDataAction(path : String, key : String, v : Long)           = { client : DataClient -> PutDataAction(client, path, key, v) }
fun PutDataAction(path : String, key : String, v : Asset)          = { client : DataClient -> PutDataAction(client, path, key, v) }
fun PutDataAction(path : String, key : String, v : ArrayList<Int>) = { client : DataClient -> PutDataAction(client, path, key, v) }
