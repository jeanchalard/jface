package com.j.jface.feed;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

public class JFaceDataFeed implements ActionBar.TabListener, ViewPager.OnPageChangeListener
{
  @IdRes private static final int PAGER_ID = 10001;
  @NonNull private final Activity mA;
  @NonNull private final ViewPager mPager;

  public JFaceDataFeed(@NonNull final Activity a)
  {
    mA = a;
    mPager = new ViewPager(a);
    final LayoutParams lp = new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    mPager.setAdapter(new JFaceDataFeedPager(a.getFragmentManager(), new Client(a)));
    mPager.setOnPageChangeListener(this);
    mPager.setId(PAGER_ID);
    a.setContentView(mPager, lp);

    final ActionBar bar = a.getActionBar();
    bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    bar.addTab(bar.newTab().setText("Logs & Data").setTabListener(this));
    bar.addTab(bar.newTab().setText("Debug tools").setTabListener(this));

    startGeofenceService(mA);
  }

  private void startGeofenceService(final Activity activity)
  {
    final Intent i = new Intent(activity, GeofenceTransitionReceiverService.class);
    i.setAction(GeofenceTransitionReceiver.ACTION_MANUAL_START);
    activity.startService(i);
  }

  // Tab management
  @Override public void onTabSelected(final ActionBar.Tab tab, final FragmentTransaction ft)
  {
    mPager.setCurrentItem(tab.getPosition());
  }
  @Override public void onPageSelected(final int position)
  {
    mA.getActionBar().setSelectedNavigationItem(position);
  }

  @Override public void onTabUnselected(final ActionBar.Tab tab, final FragmentTransaction ft) {}
  @Override public void onTabReselected(final ActionBar.Tab tab, final FragmentTransaction ft) {}
  @Override public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {}
  @Override public void onPageScrollStateChanged(final int state) {}
}
