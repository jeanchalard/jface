package com.j.jface.action

import com.j.jface.Util
import java.io.File
import java.io.InputStream

class FileCopyAction(src : InputStream, dst : File) : () -> File
{
  constructor(src : File, dst : File) : this(src.inputStream(), dst)
  private val mSrc : InputStream = src
  private val mDst : File = dst

  override fun invoke() : File
  {
    Util.copy(mSrc, mDst.outputStream())
    return mDst
  }
}
