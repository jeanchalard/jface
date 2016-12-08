package com.j.jface.feed;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.j.jface.R;
import com.j.jface.feed.fragments.ActivityLogFragment;
import com.j.jface.feed.fragments.DebugToolsFragment;
import com.j.jface.feed.fragments.LogsAndDataFragment;

public class JFaceDataFeed
{
  @NonNull private final String LAST_OPEN_FRAGMENT_INDEX = "last_open_fragment_index";
  @NonNull private final Activity mA;
  @NonNull private final ActionBarDrawerToggle mDrawerToggle;

  // State
  int mCurrentlyDisplayedFragmentIndex = 0;

  public JFaceDataFeed(@NonNull final Activity a, final Bundle savedInstanceState)
  {
    mA = a;
    final LayoutParams lp = new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    a.setContentView(R.layout.data_feed_drawer);
    final DrawerLayout drawer = (DrawerLayout)a.findViewById(R.id.dataFeedDrawer);

    final ListView list = (ListView)a.findViewById(R.id.dataFeedDrawerContents);
    list.setAdapter(new ArrayAdapter<>(a, R.layout.data_feed_drawer_item, new String[] { "Activity log", "Logs & data", "Debug tools" }));
    list.setOnItemClickListener(new AdapterView.OnItemClickListener()
    {
      private final Client mClient = new Client(a);
      @Override public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id)
      {
        final Fragment f = getFragmentForPosition(position, mClient);
        a.getFragmentManager().beginTransaction()
                              .replace(R.id.dataFeedContents, f)
                              .commit();
        list.setItemChecked(position, true);
        drawer.closeDrawer(list);
        mCurrentlyDisplayedFragmentIndex = position;
      }
    });

    //noinspection ConstantConditions
    mCurrentlyDisplayedFragmentIndex = null == savedInstanceState ? 0 : savedInstanceState.getInt(LAST_OPEN_FRAGMENT_INDEX);
    list.getOnItemClickListener().onItemClick(null, null, mCurrentlyDisplayedFragmentIndex, 0); // Switch to the initial fragment

    final Toolbar toolbar = (Toolbar)a.findViewById(R.id.dataFeedToolbar);
    toolbar.setTitle(R.string.data_feed_title);
    mDrawerToggle = new ActionBarDrawerToggle(a, drawer, toolbar, R.string.drawer_open_desc, R.string.drawer_closed_desc);

    if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(mA, Manifest.permission.ACCESS_FINE_LOCATION))
      startGeofenceService(a);
  }

  public void onSaveInstanceState(@NonNull Bundle instanceState)
  {
    instanceState.putInt(LAST_OPEN_FRAGMENT_INDEX, mCurrentlyDisplayedFragmentIndex);
  }

  public void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] results)
  {
    if (0 != permissions.length) startGeofenceService(mA);
  }

  // Handling the drawer.
  public void onConfigurationChanged(final Configuration c)
  {
    mDrawerToggle.onConfigurationChanged(c);
  }

  public boolean onOptionsItemSelected(final MenuItem i)
  {
    return mDrawerToggle.onOptionsItemSelected(i);
  }

  public void onPostCreate(final Bundle b)
  {
    mDrawerToggle.syncState();
    ActivityCompat.requestPermissions(mA, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
  }

  private static FragmentWrapper<?> getFragmentForPosition(final int position, final Client client)
  {
    switch (position)
    {
      case 0 : return new FragmentWrapper<ActivityLogFragment>(client){}; // TODO : put the new content there
      case 1 : return new FragmentWrapper<LogsAndDataFragment>(client){};
      case 2 : return new FragmentWrapper<DebugToolsFragment>(client){};
    }
    return null;
  }

  private void startGeofenceService(final Activity activity)
  {
    final Intent i = new Intent(activity, GeofenceTransitionReceiverService.class);
    i.setAction(GeofenceTransitionReceiver.ACTION_MANUAL_START);
    activity.startService(i);
  }
}
