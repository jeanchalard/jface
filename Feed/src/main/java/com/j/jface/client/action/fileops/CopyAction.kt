package com.j.jface.client.action.fileops

import com.google.android.gms.common.api.GoogleApiClient
import com.j.jface.Util
import com.j.jface.client.Client
import com.j.jface.client.action.Action
import java.io.File
import java.io.InputStream

class CopyAction(client : Client, dependency : Action?, src : InputStream, dst : File) : Action(client, dependency)
{
  private val mSrc : InputStream = src
  private val mDst : File = dst
  constructor(client : Client, dependency : Action?, src : File, dst : File) : this(client, dependency, src.inputStream(), dst)

  override fun run(client : GoogleApiClient)
  {
    Util.copy(mSrc, mDst.outputStream())
    finish()
  }
}
