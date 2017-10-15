package com.j.jface.feed;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.j.jface.R;
import com.j.jface.client.Client;
import com.j.jface.feed.fragments.ActivityLogFragment;
import com.j.jface.feed.fragments.DebugToolsFragment;
import com.j.jface.feed.fragments.ImageEditorFragment;
import com.j.jface.feed.fragments.ImageSelectorFragment;
import com.j.jface.feed.fragments.LogsAndDataFragment;
import com.j.jface.feed.fragments.MessagesFragment;
import com.j.jface.lifecycle.FragmentWrapper;
import com.j.jface.lifecycle.WrappedActivity;

public class JFaceDataFeed extends WrappedActivity
{
  @NonNull private final String LAST_OPEN_FRAGMENT_INDEX = "last_open_fragment_index";
  @NonNull private final ActionBarDrawerToggle mDrawerToggle;
  @NonNull private final Client mClient;

  // State
  private int mCurrentlyDisplayedFragmentIndex = 0;

  public JFaceDataFeed(@NonNull final Args args)
  {
    super(args);
    mA.setContentView(R.layout.data_feed_drawer);
    mClient = new Client(mA);
    final DrawerLayout drawer = (DrawerLayout)mA.findViewById(R.id.dataFeedDrawer);

    final ListView list = (ListView)mA.findViewById(R.id.dataFeedDrawerContents);
    list.setAdapter(new ArrayAdapter<>(mA, R.layout.data_feed_drawer_item, new String[] { "Messages", "Settings", "Activity log", "Logs & data", "Debug tools" }));
    final AdapterView.OnItemClickListener listener = (parent, view, position, id) ->
    {
      final Fragment f = getFragmentForPosition(position, mClient);
      mA.getFragmentManager().beginTransaction()
                            .replace(R.id.dataFeedContents, f)
                            .commit();
      list.setItemChecked(position, true);
      drawer.closeDrawer(list);
      mCurrentlyDisplayedFragmentIndex = position;
    };
    list.setOnItemClickListener(listener);

    final Bundle icicle = args.icicle;
    mCurrentlyDisplayedFragmentIndex = null == icicle ? 0 : icicle.getInt(LAST_OPEN_FRAGMENT_INDEX);
    listener.onItemClick(null, null, mCurrentlyDisplayedFragmentIndex, 0); // Switch to the initial fragment

    final Toolbar toolbar = (Toolbar)mA.findViewById(R.id.dataFeedToolbar);
    toolbar.setTitle(R.string.data_feed_title);
    mDrawerToggle = new ActionBarDrawerToggle(mA, drawer, toolbar, R.string.drawer_open_desc, R.string.drawer_closed_desc);

    if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(mA, Manifest.permission.ACCESS_FINE_LOCATION))
      startGeofenceService(mA);
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

  @Nullable private static FragmentWrapper<?> getFragmentForPosition(final int position, final Client client)
  {
    switch (position)
    {
      case 0 : return new FragmentWrapper<MessagesFragment>(client){};
      case 1 : return new FragmentWrapper<ImageSelectorFragment>(client){};
      case 2 : return new FragmentWrapper<ActivityLogFragment>(client){};
      case 3 : return new FragmentWrapper<LogsAndDataFragment>(client){};
      case 4 : return new FragmentWrapper<DebugToolsFragment>(client){};
    }
    return null;
  }

  private void startGeofenceService(final Activity activity)
  {
    final Intent i = new Intent(activity, GeofenceTransitionReceiverService.class);
    i.setAction(GeofenceTransitionReceiver.ACTION_MANUAL_START);
    activity.startService(i);
  }

  @Override protected void onActivityResult(final int requestCode, final int resultCode, final Intent data)
  {
    switch (requestCode)
    {
      case ImageSelectorFragment.CHOOSE_IMAGE_INTENT :
        mA.getFragmentManager().beginTransaction()
         .replace(R.id.dataFeedContents, new FragmentWrapper<ImageEditorFragment>(data){})
         .commit();
    }
  }
}
