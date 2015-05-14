package com.j.jface.feed;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.j.jface.R;

public class JFaceDataFeedFragment extends Fragment
{
  private static final String RESOURCE = "resource";
  private static final int[] RESOURCES = { R.layout.debug_app_tab_1,
                                           R.layout.debug_app_tab_2 };

  @Override public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle icicle)
  {
    final int resource = getArguments().getInt(RESOURCE);
    return inflater.inflate(resource, container, false);
  }

  public static class PagerAdapter extends FragmentPagerAdapter
  {
    public PagerAdapter(FragmentManager fm) { super(fm); }

    @Override public Fragment getItem(int position)
    {
      Fragment fragment = new JFaceDataFeedFragment();
      Bundle args = new Bundle();
      args.putInt(RESOURCE, RESOURCES[position]);
      fragment.setArguments(args);
      return fragment;
    }

    @Override
    public int getCount()
    {
      return RESOURCES.length;
    }
  }
}
