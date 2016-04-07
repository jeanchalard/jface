package com.j.jface.feed;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentPagerAdapter;

public class JFaceDataFeedPager extends FragmentPagerAdapter
{
  @NonNull final Client mClient;

  public JFaceDataFeedPager(final FragmentManager fm, @NonNull final Client client)
  {
    super(fm);
    mClient = client;
  }

  @Override
  @SuppressLint("ValidFragment")
  public Fragment getItem(final int position)
  {
    switch (position)
    {
      case 0: return new FragmentWrapper<TabLogsAndData>(mClient){};
      case 1: return new FragmentWrapper<TabDebugTools>(mClient){};
      default: return null;
    }
  }

  @Override
  public int getCount()
  {
    return 2;
  }
}
