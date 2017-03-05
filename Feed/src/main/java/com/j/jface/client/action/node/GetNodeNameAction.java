package com.j.jface.client.action.node;

import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.j.jface.FutureValue;
import com.j.jface.client.action.Action;

// An Action that gets the name of the local node.
public class GetNodeNameAction implements Action
{
  @NonNull public FutureValue<String> mResult;

  public GetNodeNameAction()
  {
    mResult = new FutureValue<>();
  }

  @Override public void run(@NonNull GoogleApiClient client)
  {
    Wearable.NodeApi.getLocalNode(client).setResultCallback(
     new ResultCallback<NodeApi.GetLocalNodeResult>() {
       @Override public void onResult(@NonNull NodeApi.GetLocalNodeResult getLocalNodeResult)
       {
         mResult.set(getLocalNodeResult.getNode().getId());
       }
     }
    );
  }
}
