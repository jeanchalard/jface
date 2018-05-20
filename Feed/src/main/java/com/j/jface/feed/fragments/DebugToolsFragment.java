package com.j.jface.feed.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.firebase.iid.FirebaseInstanceId;
import com.j.jface.Const;
import com.j.jface.R;
import com.j.jface.action.InformUserAction;
import com.j.jface.feed.views.SnackbarRegistry;
import com.j.jface.lifecycle.WrappedFragment;
import com.j.jface.org.todo.TodoProvider;
import com.j.jface.wear.Wear;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import kotlin.Unit;

public class DebugToolsFragment extends WrappedFragment implements View.OnClickListener, NumberPicker.OnValueChangeListener, TimePicker.OnTimeChangedListener
{
  private static final int MSG_UPDATE_TIME = 1;
  private static final int GRACE_FOR_UPDATE = 3000;

  @NonNull private final Wear mWear;
  @NonNull private final Fragment mFragment;
  private long mOffset = 0;
  private boolean mTicking = false;
  @NonNull private final Time mTime1, mTime2;
  @NonNull private final NumberPicker mDaysOffsetUI;
  @NonNull private final NumberPicker mHoursUI;
  @NonNull private final NumberPicker mMinutesUI;
  @NonNull private final NumberPicker mSecondsUI;
  @NonNull private final TextView mOffsetLabel;
  @NonNull private final CheckBox[] mFenceUIs;

  private static class TabDebugToolsHandler extends Handler
  {
    private final DebugToolsFragment p;
    private TabDebugToolsHandler(final DebugToolsFragment p) { this.p = p; }
    @Override public void handleMessage(@NonNull Message message)
    {
      switch (message.what)
      {
        case MSG_UPDATE_TIME:
          p.tick();
          sendEmptyMessageDelayed(MSG_UPDATE_TIME, 1000);
      }
    }
  }
  private final Handler mHandler = new TabDebugToolsHandler(this);
  public DebugToolsFragment(@NonNull final Args a, @NonNull final Wear b)
  {
    super(a.inflater.inflate(R.layout.fragment_debug_tools, a.container, false));
    mWear = b;
    mFragment = a.fragment;
    mTime1 = new Time(); mTime2 = new Time();
    mView.findViewById(R.id.button_now).setOnClickListener(this);
    mDaysOffsetUI = mView.findViewById(R.id.daysOffsetUI);
    mDaysOffsetUI.setMinValue(0); mDaysOffsetUI.setMaxValue(14);
    mHoursUI = mView.findViewById(R.id.hoursUI);
    mHoursUI.setMinValue(0); mHoursUI.setMaxValue(23);
    mMinutesUI = mView.findViewById(R.id.minutesUI);
    mMinutesUI.setMinValue(0); mMinutesUI.setMaxValue(59);
    mSecondsUI = mView.findViewById(R.id.secondsUI);
    mSecondsUI.setMinValue(0); mSecondsUI.setMaxValue(59);
    mOffsetLabel = mView.findViewById(R.id.offsetLabel);
    tick();
    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, 1000);

    mHoursUI.setOnValueChangedListener(this);
    mMinutesUI.setOnValueChangedListener(this);
    mSecondsUI.setOnValueChangedListener(this);
    mDaysOffsetUI.setOnValueChangedListener(this);

    mFenceUIs = new CheckBox[4];
    mFenceUIs[0] = mView.findViewById(R.id.fence1);
    mFenceUIs[1] = mView.findViewById(R.id.fence2);
    mFenceUIs[2] = mView.findViewById(R.id.fence3);
    mFenceUIs[3] = mView.findViewById(R.id.fence4);
    for (final CheckBox c : mFenceUIs) c.setOnClickListener(this);
    if (Const.RIO_MODE) mFenceUIs[2].setText("六本木");

    final TextView nodeNameTextView = mView.findViewById(R.id.nodeId_textView);
      mWear.getNodeName(mFragment.getContext(), nodeName -> {
        a.fragment.getActivity().runOnUiThread(() -> nodeNameTextView.setText("Node id : " + nodeName));
        return Unit.INSTANCE;
     });

    mView.findViewById(R.id.button_copy_todo_from_storage).setOnClickListener(v ->
    {
      final Intent intent = new Intent();
      intent.setType("*/*");
      intent.setAction(Intent.ACTION_GET_CONTENT);
      a.fragment.startActivityForResult(intent, Const.DESTROY_DATABASE_AND_REPLACE_WITH_FILE_CONTENTS_RESULT_CODE);
    });

    mView.findViewById(R.id.button_delete_FCM_token).setOnClickListener(v ->
    {
      new Thread(() ->
      {
        try { FirebaseInstanceId.getInstance().deleteInstanceId(); } catch (IOException e) { Log.e("Can't delete token", "" + e); }
        FirebaseInstanceId.getInstance().getToken(); // Force token generation
      }).start();
    });
  }

  @Override protected void onResume()
  {
    SnackbarRegistry.INSTANCE.setSnackbarParent(mView);
  }

  @Override protected void onPause()
  {
    SnackbarRegistry.INSTANCE.unsetSnackbarParent(mView);
  }

  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data)
  {
    if (null == data || Const.DESTROY_DATABASE_AND_REPLACE_WITH_FILE_CONTENTS_RESULT_CODE != requestCode) return;
    final Uri fileUri = data.getData();
    FileNotFoundException exception = null;
    if (null != fileUri)
    {
      InputStream is = null;
      try { is = mFragment.getContext().getContentResolver().openInputStream(fileUri); }
      catch (final FileNotFoundException e) { exception = e; }
      if (null == exception && null != is)
        // Do this on the UI thread because YOLO. This is only for debug so we don't care.
        if (TodoProvider.destroyDatabaseAndReplaceWithFileContents(mFragment.getContext(), is))
        {
          new InformUserAction(mFragment.getActivity(), "Database dumped, restart JOrg", "Kill", v -> System.exit(0), null).invoke();
          return;
        }
    }
    new InformUserAction(mFragment.getActivity(), "File can't be opened." + (null == exception ? "" : " " + exception.toString()), null, null, null).invoke();
  }

  private void tick()
  {
    mTicking = true;
    mTime1.set(System.currentTimeMillis() + mOffset);
    mHoursUI.setValue(mTime1.hour);
    mMinutesUI.setValue(mTime1.minute);
    mSecondsUI.setValue(mTime1.second);

    mTime2.setToNow();
    mOffsetLabel.setText(Long.toString(mOffset));
    mTicking = false;
  }

  private void updateOffset(final int grace)
  {
    if (mTicking) return;
    mHandler.removeMessages(MSG_UPDATE_TIME);
    mTime2.setToNow();
    final int second = mSecondsUI.getValue();
    final int minute = mMinutesUI.getValue();
    final int hour = mHoursUI.getValue();
    mTime1.set(second, minute, hour, mTime2.monthDay, mTime2.month, mTime2.year);
    final int dayOffset = mDaysOffsetUI.getValue();
    mTime1.set(mTime1.toMillis(true) + dayOffset * 86400000 - grace);
    mOffset = mTime1.toMillis(true) - mTime2.toMillis(true);
    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, grace);
    mWear.putDataToCloud(Const.DATA_PATH + "/" + Const.DATA_KEY_DEBUG_TIME_OFFSET, Const.DATA_KEY_DEBUG_TIME_OFFSET, mOffset);
  }

  private void updateFences()
  {
    int fences = 0;
    for (int i = 0; i < mFenceUIs.length; ++i)
      fences |= mFenceUIs[i].isChecked() ? 1 << i : 0;
    mWear.putDataToCloud(Const.DATA_PATH + "/" + Const.DATA_KEY_DEBUG_FENCES, Const.DATA_KEY_DEBUG_FENCES, fences);
  }

  @Override public void onClick(final View v)
  {
    switch (v.getId())
    {
      case R.id.button_now:
        mOffset = 0;
        tick();
        updateOffset(0);
        mHandler.removeMessages(MSG_UPDATE_TIME);
        mHandler.sendEmptyMessage(MSG_UPDATE_TIME);
      default:
        updateFences();
    }
  }

  @Override public void onValueChange(final NumberPicker picker, final int oldVal, final int newVal)
  {
    updateOffset(GRACE_FOR_UPDATE);
  }

  @Override public void onTimeChanged(final TimePicker view, final int hourOfDay, final int minute)
  {
    updateOffset(GRACE_FOR_UPDATE);
  }
}
