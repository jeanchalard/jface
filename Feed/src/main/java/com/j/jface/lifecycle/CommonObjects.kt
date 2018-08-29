package com.j.jface.lifecycle

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object CommonObjects
{
  val executor : ExecutorService by lazy { Executors.newSingleThreadExecutor() }
}
