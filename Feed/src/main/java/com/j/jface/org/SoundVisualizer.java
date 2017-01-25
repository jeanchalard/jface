package com.j.jface.org;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

// A class that manages history and shows sound levels in a pretty manner.
// It needs to be passed the appropriate view to do it : a view with 5 subviews
// with a set width and a small minimum height that will be used to represent amplitude.
public class SoundVisualizer extends Handler
{
  private static final int UPDATE_SOUND = 1;

  private final float[] mSoundLevels = {0, 0, 0, 0, 0, 0};
  private final ViewGroup mView;

  public SoundVisualizer(final Looper l, final ViewGroup v)
  {
    super(l);
    mView = v;
  }

  @Override public void handleMessage(final Message msg)
  {
    switch (msg.what)
    {
      case UPDATE_SOUND:
        mSoundLevels[5] = mSoundLevels[4];
        mSoundLevels[4] = mSoundLevels[3];
        mSoundLevels[3] = mSoundLevels[2];
        mSoundLevels[2] = mSoundLevels[1];
        mSoundLevels[1] = mSoundLevels[0];
        if (mSoundLevels[5] != mSoundLevels[4]
         || mSoundLevels[4] != mSoundLevels[3]
         || mSoundLevels[3] != mSoundLevels[2]
         || mSoundLevels[2] != mSoundLevels[1])
          sendEmptyMessageDelayed(UPDATE_SOUND, 120);
        updateSoundLevelsVisu(mSoundLevels);
      default:
        // Que dalle
    }
  }

  public void resetToActive() { resetTo(0); }
  public void resetToNull() { resetTo(-3); }
  private void resetTo(final float f)
  {
    mSoundLevels[0] = f;
    mSoundLevels[1] = f;
    mSoundLevels[2] = f;
    mSoundLevels[3] = f;
    mSoundLevels[4] = f;
    mSoundLevels[5] = f;
    updateSoundLevelsVisu(mSoundLevels);
  }

  public void setLastSoundLevel(final float db)
  {
    if (db != mSoundLevels[0])
    {
      mSoundLevels[0] = db;
      if (!hasMessages(UPDATE_SOUND)) sendEmptyMessage(UPDATE_SOUND);
    }
  }

  private void updateSoundLevelsVisu(final float[] soundLevels)
  {
    mView.getChildAt(4).setMinimumHeight((int)(13 + soundLevels[1] * 4));
    mView.getChildAt(3).setMinimumHeight((int)(13 + soundLevels[2] * 4));
    mView.getChildAt(2).setMinimumHeight((int)(13 + soundLevels[3] * 4));
    mView.getChildAt(1).setMinimumHeight((int)(13 + soundLevels[4] * 4));
    mView.getChildAt(0).setMinimumHeight((int)(13 + soundLevels[5] * 4));
  }
}
