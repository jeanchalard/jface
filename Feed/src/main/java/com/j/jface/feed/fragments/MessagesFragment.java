package com.j.jface.feed.fragments;

import android.app.Fragment;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.wearable.DataMap;
import com.j.jface.Const;
import com.j.jface.R;
import com.j.jface.client.Client;
import com.j.jface.feed.views.PaletteView;
import com.j.jface.lifecycle.WrappedFragment;

/**
 * A fragment for showing and managing arbitrary messages.
 */
public class MessagesFragment extends WrappedFragment implements TextWatcher, PaletteView.OnColorSetListener
{
  @NonNull private final Fragment mF;
  @NonNull private final Client mClient;
  @NonNull private final EditText mTopicDataEdit;
  @NonNull private final PaletteView mPalette;
  private boolean mDataFetched; // Replace this if this class or the use of this member ever become any more than elementary

  public MessagesFragment(@NonNull final WrappedFragment.Args a, @NonNull final Client b)
  {
    super(a.inflater.inflate(R.layout.fragment_messages, a.container, false));
    mF = a.fragment;
    mClient = b;
    mTopicDataEdit = (EditText)mView.findViewById(R.id.messagesFragment_topicDataEdit);
    mTopicDataEdit.addTextChangedListener(this);
    mPalette = (PaletteView)mView.findViewById(R.id.messagesFragment_palette);
    mPalette.addOnColorSetListener(this);
    mDataFetched = false;
    b.getData(Const.DATA_PATH + "/" + Const.DATA_KEY_TOPIC, new Client.GetDataCallback() {
      public void run(@NonNull final String path, @NonNull final DataMap dataMap) {
        a.fragment.getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            mTopicDataEdit.setText(dataMap.getString(Const.DATA_KEY_TOPIC));
            mDataFetched = true;
          }
        });
      }
    });
  }

  @Override public void afterTextChanged(@NonNull final Editable s)
  {
    if (!mDataFetched) return;
    mClient.putData(Const.DATA_PATH + "/" + Const.DATA_KEY_TOPIC, Const.DATA_KEY_TOPIC, mTopicDataEdit.getText().toString());
  }

  @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
  @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

  @Override public void onColorSet(final int color)
  {
    mTopicDataEdit.setTextColor(color);
  }
}
