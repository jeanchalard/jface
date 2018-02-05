package com.j.jface.feed.views

import android.view.View

/**
 * An instance that knows how to show a snackbar.
 * UI parts that can show a snackbar should implement this ; it will make for easier use of snackbar actions and let actions
 * opportunistically show snackbars instead of more instrusive UI elements like notifications.
 */
interface Snackbarable
{
  val snackbarParent : View?
}
