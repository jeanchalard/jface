package com.j.jface.feed.views

import android.support.design.widget.Snackbar
import android.view.View

/**
 * An instance that knows how to show a snackbar.
 * UI parts that can show a snackbar should implement this ; it will make for easier use of snackbar actions and let actions
 * opportunistically show snackbars instead of more instrusive UI elements like notifications.
 */
interface Snackbarable {
  val snackbarParent : View
  fun showSnackbar(message : String)
  {
    showSnackbar(message, null, null)
  }
  fun showSnackbar(message : String, actionTitle : String?, callback : View.OnClickListener?)
  {
    val sb = Snackbar.make(snackbarParent, message, Snackbar.LENGTH_SHORT)
    if (null != actionTitle && null != callback)
      sb.setAction(actionTitle, callback)
    sb.show()
  }
}
