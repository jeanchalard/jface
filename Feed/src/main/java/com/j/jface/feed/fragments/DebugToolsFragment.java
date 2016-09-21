package com.j.jface.feed.fragments;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.format.Time;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;

import com.j.jface.Const;
import com.j.jface.R;
import com.j.jface.feed.Client;
import com.j.jface.feed.WrappedFragment;

public class DebugToolsFragment extends WrappedFragment implements View.OnClickListener, NumberPicker.OnValueChangeListener, TimePicker.OnTimeChangedListener
{
  private static final int MSG_UPDATE_TIME = 1;
  private static final int GRACE_FOR_UPDATE = 3000;

  @NonNull final Client mClient;
  @NonNull final EditText mTopicDataEdit;
  long mOffset = 0;
  boolean mTicking = false;
  @NonNull final Time mTime1, mTime2;
  @NonNull final NumberPicker mDaysOffsetUI;
  @NonNull final NumberPicker mHoursUI;
  @NonNull final NumberPicker mMinutesUI;
  @NonNull final NumberPicker mSecondsUI;
  @NonNull final TextView mOffsetLabel;
  @NonNull final CheckBox[] mFenceUIs;

  private static class TabDebugToolsHandler extends Handler
  {
    private final DebugToolsFragment p;
    public TabDebugToolsHandler(final DebugToolsFragment p) { this.p = p; }
    @Override public void handleMessage(@NonNull Message message)
    {
      switch (message.what)
      {
        case MSG_UPDATE_TIME:
          p.tick();
          sendEmptyMessageDelayed(MSG_UPDATE_TIME, 1000);
      }
    }
  };
  private Handler mHandler = new TabDebugToolsHandler(this);

  public DebugToolsFragment(@NonNull final Args a, @NonNull final Client b)
  {
    super(a.inflater.inflate(R.layout.fragment_debug_tools, a.container, false));
    mClient = b;
    mTime1 = new Time(); mTime2 = new Time();
    mTopicDataEdit = (EditText)mView.findViewById(R.id.topicDataEdit);
    mView.findViewById(R.id.button_set).setOnClickListener(this);
    mView.findViewById(R.id.button_now).setOnClickListener(this);
    mDaysOffsetUI = (NumberPicker)mView.findViewById(R.id.daysOffsetUI);
    mDaysOffsetUI.setMinValue(0); mDaysOffsetUI.setMaxValue(14);
    mHoursUI = (NumberPicker)mView.findViewById(R.id.hoursUI);
    mHoursUI.setMinValue(0); mHoursUI.setMaxValue(23);
    mMinutesUI = (NumberPicker)mView.findViewById(R.id.minutesUI);
    mMinutesUI.setMinValue(0); mMinutesUI.setMaxValue(59);
    mSecondsUI = (NumberPicker)mView.findViewById(R.id.secondsUI);
    mSecondsUI.setMinValue(0); mSecondsUI.setMaxValue(59);
    mOffsetLabel = (TextView)mView.findViewById(R.id.offsetLabel);
    tick();
    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, 1000);

    mHoursUI.setOnValueChangedListener(this);
    mMinutesUI.setOnValueChangedListener(this);
    mSecondsUI.setOnValueChangedListener(this);
    mDaysOffsetUI.setOnValueChangedListener(this);

    mFenceUIs = new CheckBox[4];
    mFenceUIs[0] = (CheckBox)mView.findViewById(R.id.fence1);
    mFenceUIs[1] = (CheckBox)mView.findViewById(R.id.fence2);
    mFenceUIs[2] = (CheckBox)mView.findViewById(R.id.fence3);
    mFenceUIs[3] = (CheckBox)mView.findViewById(R.id.fence4);
    for (final CheckBox c : mFenceUIs) c.setOnClickListener(this);
    if (Const.RIO_MODE) mFenceUIs[2].setText("六本木");
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
    mClient.putData(Const.DATA_PATH + "/" + Const.DATA_KEY_DEBUG_TIME_OFFSET, Const.DATA_KEY_DEBUG_TIME_OFFSET, mOffset);
  }

  private void updateFences()
  {
    int fences = 0;
    for (int i = 0; i < mFenceUIs.length; ++i)
      fences |= mFenceUIs[i].isChecked() ? 1 << i : 0;
    mClient.putData(Const.DATA_PATH + "/" + Const.DATA_KEY_DEBUG_FENCES, Const.DATA_KEY_DEBUG_FENCES, fences);
  }

  @Override public void onClick(final View v)
  {
    switch (v.getId())
    {
      case R.id.button_set:
        mClient.putData(Const.DATA_PATH + "/" + Const.DATA_KEY_TOPIC, Const.DATA_KEY_TOPIC, mTopicDataEdit.getText().toString());
        break;
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
