package com.j.jface.feed.fragments;

import android.app.Fragment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.wearable.DataMap;
import com.j.jface.Const;
import com.j.jface.R;
import com.j.jface.feed.Client;
import com.j.jface.feed.WrappedFragment;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * A fragment for showing and managing the activity log.
 */
public class ActivityLogFragment extends WrappedFragment
{
  @NonNull private final Fragment mF;
  @NonNull private final Client mClient;
  @NonNull private final TextView mLastActivityMnemonic;
  @NonNull private final TextView mLastActivityStartTime;

  public ActivityLogFragment(@NonNull final WrappedFragment.Args a, @NonNull final Client b)
  {
    super(a.inflater.inflate(R.layout.activity_log_layout, a.container, false));
    Log.e("HMMM", "hMM");
    mF = a.fragment;
    mClient = b;
    mLastActivityMnemonic = (TextView)mView.findViewById(R.id.last_activity_mnemonic);
    mLastActivityStartTime = (TextView)mView.findViewById(R.id.last_activity_start_time);
    mClient.getData(Const.ACTIVITY_PATH, showDataCallback());
  }

  private Client.GetDataCallback showDataCallback()
  {
    return new Client.GetDataCallback() { public void run(@NonNull final String path, @NonNull final DataMap dataMap) {
      mF.getActivity().runOnUiThread(new Runnable() { @Override public void run()
      {
        final String mnemonic = dataMap.getString(Const.DATA_KEY_LAST_ACTIVITY_MNEMONIC);
        final long startTime = dataMap.getLong(Const.DATA_KEY_LAST_ACTIVITY_START_TIME);
        mLastActivityMnemonic.setText(mnemonic);
        final GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(startTime);
        mLastActivityStartTime.setText("" + cal.get(Calendar.YEAR) + "/" + cal.get(Calendar.MONTH) + "/" + cal.get(Calendar.DAY_OF_MONTH) + "  "
         + cal.get(Calendar.HOUR) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND));
      }
      });
    }
    };
  }

}
