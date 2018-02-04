package com.j.jface.client.action.wear

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.gms.common.api.GoogleApiClient

import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.wearable.DataApi
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.Wearable
import com.j.jface.Future
import com.j.jface.client.Client
import com.j.jface.client.action.Action
import com.j.jface.client.action.ResultAction

class GetBitmapAction(client : Client, path : String, key : String, dependency : Action?, callback : Client.GetBitmapCallback?) : ResultAction<Bitmap>(client, dependency), ResultCallback<DataApi.GetFdForAssetResult>
{
  private val mPath = path
  private val mKey = key
  private val mCallback = callback
  private val mGetAssetAction = GetDataAction(client, this, path)
  override fun run(client : GoogleApiClient)
  {
    when (mGetAssetAction.status())
    {
      Future.NOT_DONE -> { mGetAssetAction.enqueue(); return }
      Future.FAILURE -> { mCallback?.run(mPath, mKey, null) ; fail(mGetAssetAction.error) ; return }
    }
    val dataMap : DataMap? = mGetAssetAction.get()
    if (null == dataMap) { mCallback?.run(mPath, mKey, null) ; fail("No data at path ${mPath}") ; return }
    val asset = dataMap.getAsset(mKey)
    if (null == asset) { mCallback?.run(mPath, mKey, null) ; fail("Data at path ${mPath} found, but no asset at key ${mKey}") ; return }
    Wearable.DataApi.getFdForAsset(client, asset).setResultCallback(this)
  }

  override fun onResult(fdResult : DataApi.GetFdForAssetResult)
  {
    if (!fdResult.status.isSuccess)
    {
      mCallback?.run(mPath, mKey, null)
      fail("Error fetching asset fd " + fdResult.status.statusMessage ?: "Unknown error")
      return
    }
    val bitmap = BitmapFactory.decodeStream(fdResult.inputStream)
    mCallback?.run(mPath, mKey, bitmap)
    finish(bitmap)
  }
}
