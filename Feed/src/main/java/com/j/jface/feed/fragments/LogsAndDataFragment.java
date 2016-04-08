package com.j.jface.feed.fragments;

import android.app.Fragment;
import android.support.annotation.NonNull;
import android.text.format.Time;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.wearable.DataMap;
import com.j.jface.Const;
import com.j.jface.Departure;
import com.j.jface.R;
import com.j.jface.feed.Client;
import com.j.jface.feed.DataSource;
import com.j.jface.feed.FeedLoader;
import com.j.jface.feed.WrappedFragment;

import java.util.ArrayList;

public class LogsAndDataFragment extends WrappedFragment implements View.OnClickListener
{
  @NonNull private final Fragment mF;
  @NonNull private final Client mClient;
  @NonNull private final TextView mLog;

  public LogsAndDataFragment(@NonNull final WrappedFragment.Args a, @NonNull final Client b)
  {
    super(a.inflater.inflate(R.layout.fragment_logs_and_data, a.container, false));
    mF = a.fragment;
    mLog = (TextView)mView.findViewById(R.id.log);
    mClient = b;
    mView.findViewById(R.id.button_refresh).setOnClickListener(this);
    mView.findViewById(R.id.button_load).setOnClickListener(this);
    mView.findViewById(R.id.button_clear).setOnClickListener(this);
  }

  private Client.GetDataCallback showDataCallback()
  {
    return new Client.GetDataCallback() { public void run(@NonNull final String path, @NonNull final DataMap dataMap) {
      mF.getActivity().runOnUiThread(new Runnable() { @Override public void run()
      {
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

  @Override public void onClick(final View v)
  {
    switch (v.getId())
    {
      case R.id.button_refresh: mLog.setText(""); retrieveStatus(mClient); break;
      case R.id.button_load: FeedLoader.startAllLoads(mClient); break;
      case R.id.button_clear: mClient.clearAllData(); break;
    }
  }
}
