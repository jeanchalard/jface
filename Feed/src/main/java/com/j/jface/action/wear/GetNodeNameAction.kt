package com.j.jface.action.wear

import android.content.Context
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable

private fun getNodeName(result : Node?) : String
{
  if (null == result) return "Error getting node name"
  return (result.id ?: "") + ":" + (result.displayName ?: "")
}

/**
 * An action to get the name of a node.
 */
fun GetNodeNameAction(context : Context, callback : (String) -> Unit) = {
    Wearable.getNodeClient(context).localNode.addOnCompleteListener { callback(getNodeName(it.result)) } }
