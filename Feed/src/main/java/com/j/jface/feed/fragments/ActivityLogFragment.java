package com.j.jface.feed.fragments;

import android.app.Fragment;
import android.support.annotation.NonNull;
import android.widget.TextView;

import com.google.android.gms.wearable.DataMap;
import com.j.jface.Const;
import com.j.jface.R;
import com.j.jface.client.Client;
import com.j.jface.lifecycle.WrappedFragment;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * A fragment for showing and managing the activity log.
 */
public class ActivityLogFragment extends WrappedFragment
{
  @NonNull private final Fragment mF;
  @NonNull private final TextView mLastActivityMnemonic;
  @NonNull private final TextView mLastActivityStartTime;

  public ActivityLogFragment(@NonNull final WrappedFragment.Args a, @NonNull final Client client)
  {
    super(a.inflater.inflate(R.layout.fragment_activity_log, a.container, false));
    mF = a.fragment;
    mLastActivityMnemonic = (TextView)mView.findViewById(R.id.last_activity_mnemonic);
    mLastActivityStartTime = (TextView)mView.findViewById(R.id.last_activity_start_time);
    client.getData(Const.ACTIVITY_PATH, showDataCallback());
  }

  private Client.GetDataCallback showDataCallback()
  {
    return (path, dataMap) -> mF.getActivity().runOnUiThread(() ->
    {
      final String mnemonic = dataMap.getString(Const.DATA_KEY_LAST_ACTIVITY_MNEMONIC);
      final long startTime = dataMap.getLong(Const.DATA_KEY_LAST_ACTIVITY_START_TIME);
      mLastActivityMnemonic.setText(mnemonic);
      final GregorianCalendar cal = new GregorianCalendar();
      cal.setTimeInMillis(startTime);
      mLastActivityStartTime.setText("" + cal.get(Calendar.YEAR) + "/" + cal.get(Calendar.MONTH) + "/" + cal.get(Calendar.DAY_OF_MONTH) + "  "
       + cal.get(Calendar.HOUR) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND));
    });
  }
}
