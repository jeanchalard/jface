package com.j.jface.feed;

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

public class TabDebugTools extends WrappedFragment implements View.OnClickListener, NumberPicker.OnValueChangeListener, TimePicker.OnTimeChangedListener
{
  private static final int MSG_UPDATE_TIME = 1;
  private static final int GRACE_FOR_UPDATE = 3000;

  @NonNull final Client mClient;
  @NonNull final EditText mDataEdit;
  long mOffset = 0;
  boolean mTicking = false;
  @NonNull final Time mTime1, mTime2;
  @NonNull final NumberPicker mDaysOffsetUI;
  @NonNull final TimePicker mTimeUI;
  @NonNull final NumberPicker mSecondsUI;
  @NonNull final TextView mOffsetLabel;
  @NonNull final CheckBox[] mFenceUIs;

  private static class TabDebugToolsHandler extends Handler
  {
    private final TabDebugTools p;
    public TabDebugToolsHandler(final TabDebugTools p) { this.p = p; }
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

  public TabDebugTools(@NonNull final Args a, @NonNull final Client b)
  {
    super(a.inflater.inflate(R.layout.debug_app_tab_debug_tools, a.container, false));
    mClient = b;
    mTime1 = new Time(); mTime2 = new Time();
    mDataEdit = (EditText)mView.findViewById(R.id.adHocDataEdit);
    mView.findViewById(R.id.button_set).setOnClickListener(this);
    mView.findViewById(R.id.button_now).setOnClickListener(this);
    mDaysOffsetUI = (NumberPicker)mView.findViewById(R.id.daysOffsetUI);
    mDaysOffsetUI.setMinValue(0); mDaysOffsetUI.setMaxValue(14);
    mTimeUI = (TimePicker)mView.findViewById(R.id.timeUI);
    mSecondsUI = (NumberPicker)mView.findViewById(R.id.secondsUI);
    mSecondsUI.setMinValue(0); mSecondsUI.setMaxValue(59);
    mOffsetLabel = (TextView)mView.findViewById(R.id.offsetLabel);
    mTimeUI.setIs24HourView(true); // sanity
    tick();
    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, 1000);

    mSecondsUI.setOnValueChangedListener(this);
    mTimeUI.setOnTimeChangedListener(this);
    mDaysOffsetUI.setOnValueChangedListener(this);

    mFenceUIs = new CheckBox[4];
    mFenceUIs[0] = (CheckBox)mView.findViewById(R.id.fence1);
    mFenceUIs[1] = (CheckBox)mView.findViewById(R.id.fence2);
    mFenceUIs[2] = (CheckBox)mView.findViewById(R.id.fence3);
    mFenceUIs[3] = (CheckBox)mView.findViewById(R.id.fence4);
    for (final CheckBox c : mFenceUIs) c.setOnClickListener(this);
  }

  private void tick()
  {
    mTicking = true;
    mTime1.set(System.currentTimeMillis() + mOffset);
    mTimeUI.setCurrentMinute(mTime1.hour);
    mTimeUI.setCurrentMinute(mTime1.minute);
    mSecondsUI.setValue(mTime1.second);

    mTime2.setToNow();
    mOffsetLabel.setText(Long.toString(mOffset));
    mTicking = false;
  }

  private void updateOffset()
  {
    if (mTicking) return;
    mHandler.removeMessages(MSG_UPDATE_TIME);
    mTime2.setToNow();
    final int second = mSecondsUI.getValue();
    final int minute = mTimeUI.getCurrentMinute();
    final int hour = mTimeUI.getCurrentHour();
    mTime1.set(second, minute, hour, mTime2.monthDay, mTime2.month, mTime2.year);
    final int dayOffset = mDaysOffsetUI.getValue();
    mTime1.set(mTime1.toMillis(true) + dayOffset * 86400000 - GRACE_FOR_UPDATE);
    mOffset = mTime1.toMillis(true) - mTime2.toMillis(true);
    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, GRACE_FOR_UPDATE);
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
        mClient.putData(Const.DATA_PATH + "/" + Const.DATA_KEY_ADHOC,
         Const.DATA_KEY_ADHOC, mDataEdit.getText().toString());
        break;
      case R.id.button_now:
        mOffset = 0;
        tick();
        updateOffset();
        mHandler.removeMessages(MSG_UPDATE_TIME);
        mHandler.sendEmptyMessage(MSG_UPDATE_TIME);
      default:
        updateFences();
    }
  }

  @Override public void onValueChange(final NumberPicker picker, final int oldVal, final int newVal)
  {
    updateOffset();
  }

  @Override public void onTimeChanged(final TimePicker view, final int hourOfDay, final int minute)
  {
    updateOffset();
  }
}
