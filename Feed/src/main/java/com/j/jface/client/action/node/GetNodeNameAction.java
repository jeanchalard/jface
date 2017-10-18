package com.j.jface.client.action.node;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.j.jface.FutureValue;
import com.j.jface.client.Client;
import com.j.jface.client.action.Action;
import com.j.jface.client.action.ResultAction;

// An Action that gets the name of the local node.
public class GetNodeNameAction extends ResultAction<String> implements ResultCallback<NodeApi.GetLocalNodeResult>
{
  public interface GetNodeNameCallback { void run(@NonNull final String name); }
  @Nullable private final GetNodeNameCallback mCallback;

  public GetNodeNameAction(@NonNull final Client client, @Nullable final Action then) { this(client, then, null); }
  public GetNodeNameAction(@NonNull final Client client, @Nullable final Action then, @Nullable GetNodeNameCallback callback)
  {
    super(client, then);
    mCallback = callback;
  }

  @Override public void run(@NonNull final GoogleApiClient client)
  {
    Wearable.NodeApi.getLocalNode(client).setResultCallback(this);
  }

  @Override public void onResult(@NonNull final NodeApi.GetLocalNodeResult getLocalNodeResult)
  {
    final String nodeId = getLocalNodeResult.getNode().getId();
    if (null != mCallback) mCallback.run(nodeId);
    finish(nodeId);
  }
}
