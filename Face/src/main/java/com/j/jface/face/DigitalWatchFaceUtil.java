/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.j.jface.face;

import android.graphics.Color;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import org.jetbrains.annotations.NotNull;

public final class DigitalWatchFaceUtil
{
  /**
   * The path for the {@link DataItem} containing {@link DigitalWatchFaceService} configuration.
   */
  public static final String CONFIG_PATH = "/jwatch/Conf";
  public static final String DATA_PATH = "/jwatch/Data";

  public static final String CONFIG_KEY_BACKGROUND = "background";

  /**
   * Name of the default interactive mode digits color and the ambient mode digits color.
   */
  public static final int COLOR_VALUE_DEFAULT_AND_AMBIENT_DIGITS = parseColor("White");
  public static final int COLOR_VALUE_DEFAULT_AND_AMBIENT_SECONDS = parseColor("Grey");

  /**
   * Callback interface to perform an action with the current config {@link DataMap} for
   * {@link DigitalWatchFaceService}.
   */
  public interface FetchConfigDataMapCallback
  {
    /**
     * Callback invoked with the current config {@link DataMap} for
     * {@link DigitalWatchFaceService}.
     */
    void onConfigDataMapFetched(DataMap config);
  }

  private static int parseColor(@NotNull String colorName)
  {
    return Color.parseColor(colorName.toLowerCase());
  }

  /**
   * Asynchronously fetches the current config {@link DataMap} for {@link DigitalWatchFaceService}
   * and passes it to the given callback.
   * <p>
   * If the current config {@link DataItem} doesn't exist, it isn't created and the callback
   * receives an empty DataMap.
   */
  public static void fetchConfigDataMap(final GoogleApiClient client,
                                        final FetchConfigDataMapCallback callback)
  {
    Wearable.NodeApi.getLocalNode(client).setResultCallback(
     new ResultCallback<NodeApi.GetLocalNodeResult>()
     {
       @Override
       public void onResult(@NotNull NodeApi.GetLocalNodeResult getLocalNodeResult)
       {
         String localNode = getLocalNodeResult.getNode().getId();
         Uri uri = new Uri.Builder()
          .scheme("wear")
          .path(DigitalWatchFaceUtil.CONFIG_PATH)
          .authority(localNode)
          .build();
         Wearable.DataApi.getDataItem(client, uri)
          .setResultCallback(new DataItemResultCallback(callback));
       }
     }
    );
  }

  /**
   * Overwrites (or sets, if not present) the keys in the current config {@link DataItem} with
   * the ones appearing in the given {@link DataMap}. If the config DataItem doesn't exist,
   * it's created.
   * <p>
   * It is allowed that only some of the keys used in the config DataItem appear in
   * {@code configKeysToOverwrite}. The rest of the keys remains unmodified in this case.
   */
  public static void overwriteKeysInConfigDataMap(final GoogleApiClient googleApiClient,
                                                  final DataMap configKeysToOverwrite)
  {

    DigitalWatchFaceUtil.fetchConfigDataMap(googleApiClient,
     new FetchConfigDataMapCallback()
     {
       @Override
       public void onConfigDataMapFetched(DataMap currentConfig)
       {
         DataMap overwrittenConfig = new DataMap();
         overwrittenConfig.putAll(currentConfig);
         overwrittenConfig.putAll(configKeysToOverwrite);
         DigitalWatchFaceUtil.putConfigDataItem(googleApiClient, overwrittenConfig);
       }
     }
    );
  }

  /**
   * Overwrites the current config {@link DataItem}'s {@link DataMap} with {@code newConfig}.
   * If the config DataItem doesn't exist, it's created.
   */
  public static void putConfigDataItem(GoogleApiClient googleApiClient, DataMap newConfig)
  {
    PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(CONFIG_PATH);
    DataMap configToPut = putDataMapRequest.getDataMap();
    configToPut.putAll(newConfig);
    Wearable.DataApi.putDataItem(googleApiClient, putDataMapRequest.asPutDataRequest())
     .setResultCallback(new ResultCallback<DataApi.DataItemResult>()
     {
       @Override
       public void onResult(@NotNull DataApi.DataItemResult dataItemResult)
       {
         if (Log.isLoggable("J", Log.DEBUG))
         {
           Log.d("J", "putDataItem result status: " + dataItemResult.getStatus());
         }
       }
     });
  }

  private static class DataItemResultCallback implements ResultCallback<DataApi.DataItemResult>
  {

    private final FetchConfigDataMapCallback mCallback;

    public DataItemResultCallback(FetchConfigDataMapCallback callback)
    {
      mCallback = callback;
    }

    @Override
    public void onResult(@NotNull DataApi.DataItemResult dataItemResult)
    {
      if (dataItemResult.getStatus().isSuccess())
      {
        if (dataItemResult.getDataItem() != null)
        {
          DataItem configDataItem = dataItemResult.getDataItem();
          DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
          DataMap config = dataMapItem.getDataMap();
          mCallback.onConfigDataMapFetched(config);
        }
        else
        {
          mCallback.onConfigDataMapFetched(new DataMap());
        }
      }
    }
  }

  private DigitalWatchFaceUtil()
  {
  }
}
