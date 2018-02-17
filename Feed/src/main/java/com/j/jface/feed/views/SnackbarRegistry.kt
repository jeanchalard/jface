package com.j.jface.feed.views

import android.view.View
import java.lang.ref.WeakReference

/**
 * An object that knows where to show the snackbar.
 */
object SnackbarRegistry
{
  private var snackbarParent : WeakReference<View>? = null

  fun getSnackbarParent() : View?
  {
    return snackbarParent?.get()
  }

  fun setSnackbarParent(v : View)
  {
    synchronized(this) { snackbarParent = WeakReference(v) }
  }
  fun unsetSnackbarParent(v : View)
  {
    synchronized(this)
    {
      val parent = snackbarParent ?: return
      if (parent.get() == v) snackbarParent = null
    }
  }
}
