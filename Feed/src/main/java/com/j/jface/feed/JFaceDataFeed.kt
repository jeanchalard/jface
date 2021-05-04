package com.j.jface.feed

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.j.jface.R
import com.j.jface.feed.fragments.DebugToolsFragment
import com.j.jface.feed.fragments.ImageSelectorFragment
import com.j.jface.feed.fragments.LogsAndDataFragment
import com.j.jface.feed.fragments.MessagesFragment
import com.j.jface.lifecycle.ActivityWrapper
import com.j.jface.lifecycle.AuthTrampoline
import com.j.jface.lifecycle.FragmentWrapper
import com.j.jface.lifecycle.WrappedActivity
import com.j.jface.wear.Wear

@Suppress("UNUSED_ANONYMOUS_PARAMETER")

const val LAST_OPEN_FRAGMENT_INDEX = "last_open_fragment_index"

class AuthTrampolineJFaceDataFeedBoot : ActivityWrapper<AuthTrampolineJFaceDataFeed>()
class AuthTrampolineJFaceDataFeed(args : WrappedActivity.Args) : AuthTrampoline(args) { override val trampolineDestination get() = JFaceDataFeedBoot::class.java }
class JFaceDataFeedBoot : ActivityWrapper<JFaceDataFeed>()
class JFaceDataFeed(args : WrappedActivity.Args) : WrappedActivity(args)
{
  private val mDrawerToggle : ActionBarDrawerToggle
  private val mWear : Wear

  // State
  private var mCurrentlyDisplayedFragmentIndex = 0

  init
  {
    mA.setContentView(R.layout.data_feed_drawer)
    mWear = Wear(mA)
    val drawer = mA.findViewById<DrawerLayout>(R.id.dataFeedDrawer)

    val list = mA.findViewById<ListView>(R.id.dataFeedDrawerContents)
    list.adapter = ArrayAdapter(mA, R.layout.data_feed_drawer_item, arrayOf("Messages", "Settings", "Logs & data", "Debug tools"))
    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    val listener = AdapterView.OnItemClickListener { parent, view, position, id ->
      val f = getFragmentForPosition(position, mWear)!!
      mA.supportFragmentManager.beginTransaction()
       .replace(R.id.dataFeedContents, f)
       .commit()
      list.setItemChecked(position, true)
      drawer.closeDrawer(list)
      mCurrentlyDisplayedFragmentIndex = position
    }
    list.onItemClickListener = listener

    val icicle = args.icicle
    mCurrentlyDisplayedFragmentIndex = icicle?.getInt(LAST_OPEN_FRAGMENT_INDEX) ?: 3
    mA.lifecycleScope.launchWhenResumed {
      listener.onItemClick(null, null, mCurrentlyDisplayedFragmentIndex, 0) // Switch to the initial fragment
    }

    val toolbar = mA.findViewById<Toolbar>(R.id.dataFeedToolbar)
    toolbar.setTitle(R.string.data_feed_title)
    mDrawerToggle = ActionBarDrawerToggle(mA, drawer, toolbar, R.string.drawer_open_desc, R.string.drawer_closed_desc)
  }

  override fun onSaveInstanceState(instanceState : Bundle)
  {
    instanceState.putInt(LAST_OPEN_FRAGMENT_INDEX, mCurrentlyDisplayedFragmentIndex)
  }

  override fun onRequestPermissionsResult(requestCode : Int, permissions : Array<String>, results : IntArray)
  {
    if (permissions.isNotEmpty()) startGeofenceService(mA)
  }

  // Handling the drawer.
  override fun onConfigurationChanged(c : Configuration)
  {
    mDrawerToggle.onConfigurationChanged(c)
  }

  override fun onOptionsItemSelected(i : MenuItem) : Boolean
  {
    return mDrawerToggle.onOptionsItemSelected(i)
  }

  override fun onPostCreate(b : Bundle?)
  {
    mDrawerToggle.syncState()
    if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(mA, Manifest.permission.ACCESS_FINE_LOCATION))
      startGeofenceService(mA)
    else
      ActivityCompat.requestPermissions(mA, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
  }

  private fun getFragmentForPosition(position : Int, wear : Wear) : FragmentWrapper<*>?
  {
    when (position)
    {
      0 -> return FragmentWrapper(MessagesFragment::class.java, wear)
      1 -> return FragmentWrapper(ImageSelectorFragment::class.java, wear)
      2 -> return FragmentWrapper(LogsAndDataFragment::class.java, wear)
      3 -> return FragmentWrapper(DebugToolsFragment::class.java, wear)
    }
    return null
  }

  private fun startGeofenceService(activity : Activity)
  {
    val i = Intent(activity, GeofenceTransitionReceiverService::class.java)
    i.action = GeofenceTransitionReceiver.ACTION_MANUAL_START
    activity.startService(i)
  }
}
