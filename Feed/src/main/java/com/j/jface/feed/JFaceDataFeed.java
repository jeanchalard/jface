package com.j.jface.feed;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.j.jface.R;

public class JFaceDataFeed
{
  @NonNull private final Activity mA;

  public JFaceDataFeed(@NonNull final Activity a)
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
      @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id)
      {
        final Fragment f = getFragmentForPosition(position, mClient);
        a.getFragmentManager().beginTransaction()
                              .replace(R.id.dataFeedContents, f)
                              .commit();
        list.setItemChecked(position, true);
        drawer.closeDrawer(list);
      }
    });

    startGeofenceService(a);
  }

  private static FragmentWrapper<?> getFragmentForPosition(final int position, final Client client)
  {
    switch (position)
    {
      case 0 : return new FragmentWrapper<TabLogsAndData>(client){}; // TODO : put the new content there
      case 1 : return new FragmentWrapper<TabLogsAndData>(client){};
      case 2 : return new FragmentWrapper<TabDebugTools>(client){};
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
