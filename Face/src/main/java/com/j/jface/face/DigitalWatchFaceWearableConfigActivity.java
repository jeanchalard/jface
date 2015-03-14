/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.j.jface.face;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Wearable;
import com.j.jface.Const;
import com.j.jface.R;

/**
 * The watch-side config activity for {@link DigitalWatchFaceService}, which allows for setting the
 * background.
 */
public class DigitalWatchFaceWearableConfigActivity extends Activity implements
 WearableListView.ClickListener, WearableListView.OnScrollListener
{
  private static final String TAG = "DigitalWatchFaceConfig";

  private GoogleApiClient mGoogleApiClient;
  private TextView mHeader;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_digital_config);

    mHeader = (TextView) findViewById(R.id.header);
    WearableListView listView = (WearableListView) findViewById(R.id.option_picker);
    BoxInsetLayout content = (BoxInsetLayout) findViewById(R.id.content);
    // BoxInsetLayout adds padding by default on round devices. Add some on square devices.
    content.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener()
    {
      @Override
      public WindowInsets onApplyWindowInsets(@NonNull View v, @NonNull WindowInsets insets)
      {
        if (!insets.isRound())
        {
          v.setPaddingRelative(
           (int) getResources().getDimensionPixelSize(R.dimen.content_padding_start),
           v.getPaddingTop(),
           v.getPaddingEnd(),
           v.getPaddingBottom());
        }
        return v.onApplyWindowInsets(insets);
      }
    });

    listView.setHasFixedSize(true);
    listView.setClickListener(this);
    listView.addOnScrollListener(this);

    String[] options = getResources().getStringArray(R.array.yes_no_array);
    listView.setAdapter(new OptionsListAdapter(options));

    mGoogleApiClient = new GoogleApiClient.Builder(this)
     .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks()
     {
       @Override
       public void onConnected(Bundle connectionHint)
       {
         if (Log.isLoggable(TAG, Log.DEBUG))
         {
           Log.d(TAG, "onConnected: " + connectionHint);
         }
       }

       @Override
       public void onConnectionSuspended(int cause)
       {
         if (Log.isLoggable(TAG, Log.DEBUG))
         {
           Log.d(TAG, "onConnectionSuspended: " + cause);
         }
       }
     })
     .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener()
     {
       @Override
       public void onConnectionFailed(ConnectionResult result)
       {
         if (Log.isLoggable(TAG, Log.DEBUG))
         {
           Log.d(TAG, "onConnectionFailed: " + result);
         }
       }
     })
     .addApi(Wearable.API)
     .build();
  }

  @Override
  protected void onStart()
  {
    super.onStart();
    mGoogleApiClient.connect();
  }

  @Override
  protected void onStop()
  {
    if (mGoogleApiClient != null && mGoogleApiClient.isConnected())
    {
      mGoogleApiClient.disconnect();
    }
    super.onStop();
  }

  @Override // WearableListView.ClickListener
  public void onClick(WearableListView.ViewHolder viewHolder)
  {
    OptionItemViewHolder optionItemViewHolder = (OptionItemViewHolder) viewHolder;
    updateConfigDataItem(optionItemViewHolder.getValue());
    finish();
  }

  @Override // WearableListView.ClickListener
  public void onTopEmptyRegionClick()
  {
  }

  @Override // WearableListView.OnScrollListener
  public void onScroll(int scroll)
  {
  }

  @Override // WearableListView.OnScrollListener
  public void onAbsoluteScrollChange(int scroll)
  {
    float newTranslation = Math.min(-scroll, 0);
    mHeader.setTranslationY(newTranslation);
  }

  @Override // WearableListView.OnScrollListener
  public void onScrollStateChanged(int scrollState)
  {
  }

  @Override // WearableListView.OnScrollListener
  public void onCentralPositionChanged(int centralPosition)
  {
  }

  private void updateConfigDataItem(final boolean background)
  {
    DataMap configKeysToOverwrite = new DataMap();
    configKeysToOverwrite.putBoolean(Const.CONFIG_KEY_BACKGROUND, background);
    DigitalWatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);
  }

  private class OptionsListAdapter extends WearableListView.Adapter
  {
    private final String[] mOptions;

    public OptionsListAdapter(String[] options)
    {
      mOptions = options;
    }

    @NonNull
    @Override
    public OptionItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
      return new OptionItemViewHolder(new OptionItem(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(WearableListView.ViewHolder holder, int position)
    {
      OptionItemViewHolder optionItemViewHolder = (OptionItemViewHolder) holder;
      String optionName = mOptions[position];
      optionItemViewHolder.mOptionItem.setText(optionName);

      RecyclerView.LayoutParams layoutParams =
       new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
      int optionPickerItemMargin = (int) getResources()
       .getDimension(R.dimen.digital_config_option_picker_item_margin);
      // Add margins to first and last item to make it possible for user to tap on them.
      if (position == 0)
      {
        layoutParams.setMargins(0, optionPickerItemMargin, 0, 0);
      }
      else if (position == mOptions.length - 1)
      {
        layoutParams.setMargins(0, 0, 0, optionPickerItemMargin);
      }
      else
      {
        layoutParams.setMargins(0, 0, 0, 0);
      }
      optionItemViewHolder.itemView.setLayoutParams(layoutParams);
    }

    @Override
    public int getItemCount()
    {
      return mOptions.length;
    }
  }

  /**
   * The layout of an option with its label.
   */
  private static class OptionItem extends LinearLayout implements
   WearableListView.OnCenterProximityListener
  {
    /**
     * The duration of the expand/shrink animation.
     */
    private static final int ANIMATION_DURATION_MS = 150;

    private static final float SHRINK_LABEL_ALPHA = .5f;
    private static final float EXPAND_LABEL_ALPHA = 1f;

    @NonNull
    private final TextView mLabel;

    @NonNull
    private final ObjectAnimator mExpandLabelAnimator;
    @NonNull
    private final AnimatorSet mExpandAnimator;

    @NonNull
    private final ObjectAnimator mShrinkLabelAnimator;
    @NonNull
    private final AnimatorSet mShrinkAnimator;

    public OptionItem(Context context)
    {
      super(context);
      View.inflate(context, R.layout.color_picker_item, this);

      mLabel = (TextView) findViewById(R.id.label);

      mShrinkLabelAnimator = ObjectAnimator.ofFloat(mLabel, "alpha",
       EXPAND_LABEL_ALPHA, SHRINK_LABEL_ALPHA);
      mShrinkAnimator = new AnimatorSet().setDuration(ANIMATION_DURATION_MS);
      mShrinkAnimator.play(mShrinkLabelAnimator);

      mExpandLabelAnimator = ObjectAnimator.ofFloat(mLabel, "alpha",
       SHRINK_LABEL_ALPHA, EXPAND_LABEL_ALPHA);
      mExpandAnimator = new AnimatorSet().setDuration(ANIMATION_DURATION_MS);
      mExpandAnimator.play(mExpandLabelAnimator);
    }

    @Override
    public void onCenterPosition(boolean animate)
    {
      if (animate)
      {
        mShrinkAnimator.cancel();
        if (!mExpandAnimator.isRunning())
        {
          mExpandLabelAnimator.setFloatValues(mLabel.getAlpha(), EXPAND_LABEL_ALPHA);
          mExpandAnimator.start();
        }
      }
      else
      {
        mExpandAnimator.cancel();
        mLabel.setAlpha(EXPAND_LABEL_ALPHA);
      }
    }

    @Override
    public void onNonCenterPosition(boolean animate)
    {
      if (animate)
      {
        mExpandAnimator.cancel();
        if (!mShrinkAnimator.isRunning())
        {
          mShrinkLabelAnimator.setFloatValues(mLabel.getAlpha(), SHRINK_LABEL_ALPHA);
          mShrinkAnimator.start();
        }
      }
      else
      {
        mShrinkAnimator.cancel();
        mLabel.setAlpha(SHRINK_LABEL_ALPHA);
      }
    }

    private void setText(final String value)
    {
      mLabel.setText(value);
    }
  }

  private static class OptionItemViewHolder extends WearableListView.ViewHolder
  {
    private final OptionItem mOptionItem;

    public OptionItemViewHolder(OptionItem optionItem)
    {
      super(optionItem);
      mOptionItem = optionItem;
    }

    public boolean getValue()
    {
      Log.e("Option", mOptionItem.mLabel.getText().toString());
      return mOptionItem.mLabel.getText().toString().equals("Yes");
    }
  }
}
