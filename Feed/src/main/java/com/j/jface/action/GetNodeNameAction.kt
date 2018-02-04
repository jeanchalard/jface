package com.j.jface.action

import android.content.Context
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable

/**
 * An action to get the name of a node.
 */
class GetNodeNameAction(val context : Context, val callback : Function1<String, Unit>) : Action()
{
  private fun getNodeName(result : Node?) : String
  {
    if (null == result) return "Error getting node name"
    return (result.id ?: "") + ":" + (result.displayName ?: "")
  }

  override fun run()
  {
    Wearable.getNodeClient(context).localNode.addOnCompleteListener { callback(getNodeName(it.result)) }
  }
}
