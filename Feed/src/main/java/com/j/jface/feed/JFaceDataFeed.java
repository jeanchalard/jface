package com.j.jface.feed;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.text.format.Time;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.wearable.DataMap;
import com.j.jface.Const;
import com.j.jface.Departure;

import java.util.ArrayList;

public class JFaceDataFeed implements ActionBar.TabListener
{
  @NonNull private final Activity mA;
  @NonNull private final EditText mDataEdit;
  @NonNull private final TextView mLog;
  @NonNull private final Client mClient;

  public JFaceDataFeed(@NonNull final Activity a)
  {
    mA = a;
//    final ActionBar bar = mA.getActionBar();
//    bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
//    bar.addTab(bar.newTab().setText("Logs & Data").setTabListener(this));
//    bar.addTab(bar.newTab().setText("Debug tools").setTabListener(this));

    final ViewPager pager = new ViewPager(mA);
    final LayoutParams lp = new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    pager.setAdapter(new JFaceDataFeedFragment.PagerAdapter(mA.getFragmentManager()));
    pager.setId(10001);
    mA.setContentView(pager, lp);


//    mDataEdit = (EditText) mA.findViewById(R.id.dataItem);
//    mLog = (TextView) mA.findViewById(R.id.log);
mDataEdit = new EditText(mA);
mLog = new TextView(mA);
    mClient = new Client(a);
//    startGeofenceService(mA);
//    retrieveStatus(mClient);
  }

  private void startGeofenceService(final Activity activity)
  {
    final Intent i = new Intent(activity, GeofenceTransitionReceiverService.class);
    i.setAction(GeofenceTransitionReceiver.ACTION_MANUAL_START);
    activity.startService(i);
  }

  private Client.GetDataCallback showDataCallback()
  {
    return new Client.GetDataCallback() { public void run(@NonNull final String path, @NonNull final DataMap dataMap) {
      mA.runOnUiThread(new Runnable() { @Override public void run() {
        mLog.append(path);
        mLog.append("\n");
        if (path.endsWith(Const.DATA_PATH_SUFFIX_STATUS))
        {
          final long lastUpdate = dataMap.getLong(Const.DATA_KEY_STATUS_UPDATE_DATE);
          if (0 == lastUpdate)
          {
            mLog.append(" Never updated\n");
            return;
          }
          final Time t = new Time();
          t.set(lastUpdate);
          final String status = dataMap.getString(Const.DATA_KEY_LAST_STATUS);
          mLog.append(" Updated on " + t.format("%Y/%m/%d %H:%M:%S"));
          mLog.append("\n");
          mLog.append(" Status : " + status);
          mLog.append("\n");
          t.set(dataMap.getLong(Const.DATA_KEY_SUCCESSFUL_UPDATE_DATE));
          mLog.append(" Data last updated " + t.format("%Y/%m/%d %H:%M:%S\n"));
        }
        else
        {
          final ArrayList<DataMap> departureList = dataMap.getDataMapArrayList(Const.DATA_KEY_DEPLIST);
          if (null == departureList) return;
          for (final DataMap map : departureList)
            mLog.append(new Departure(map.getInt(Const.DATA_KEY_DEPTIME),
             map.getString(Const.DATA_KEY_EXTRA), "", null).toString() + "・");
          mLog.append("\n");
        }
      }});}};
  }

  private void retrieveStatus(final Client client)
  {
    for (final DataSource ds : DataSource.ALL_SOURCES)
    {
      client.getData(Const.DATA_PATH + "/" + ds.name + Const.DATA_PATH_SUFFIX_STATUS, showDataCallback());
      client.getData(Const.DATA_PATH + "/" + ds.name, showDataCallback());
    }
  }

  public void setAdhocData(@NonNull final View button)
  {
    mClient.putData(Const.DATA_PATH + "/" + Const.DATA_KEY_ADHOC, Const.DATA_KEY_ADHOC, mDataEdit.getText().toString());
  }

  public void refresh()
  {
    mLog.setText("");
    retrieveStatus(mClient);
  }

  public void load()
  {
    Logger.L("Start loading");
    FeedLoader.startAllLoads(mClient);
  }

  public void clearAllData()
  {
    mClient.clearAllData();
  }

  @Override
  public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft)
  {

  }

  @Override
  public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft)
  {

  }

  @Override
  public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft)
  {

  }
}
