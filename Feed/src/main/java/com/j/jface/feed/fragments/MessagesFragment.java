package com.j.jface.feed.fragments;

import android.app.Fragment;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.wearable.DataMap;
import com.j.jface.Const;
import com.j.jface.R;
import com.j.jface.feed.Client;
import com.j.jface.lifecycle.WrappedFragment;

/**
 * A fragment for showing and managing arbitrary messages.
 */
public class MessagesFragment extends WrappedFragment implements View.OnClickListener
{
  @NonNull private final Fragment mF;
  @NonNull private final Client mClient;
  @NonNull private final EditText mTopicDataEdit;

  public MessagesFragment(@NonNull final WrappedFragment.Args a, @NonNull final Client b)
  {
    super(a.inflater.inflate(R.layout.fragment_messages, a.container, false));
    mF = a.fragment;
    mClient = b;
    mTopicDataEdit = (EditText)mView.findViewById(R.id.topicDataEdit);
    mView.findViewById(R.id.button_set).setOnClickListener(this);
    b.getData(Const.DATA_PATH + "/" + Const.DATA_KEY_TOPIC, new Client.GetDataCallback() {
      public void run(@NonNull final String path, @NonNull final DataMap dataMap) {
        a.fragment.getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            mTopicDataEdit.setText(dataMap.getString(Const.DATA_KEY_TOPIC));
          }
        });
      }
    });
  }

  @Override public void onClick(final View v)
  {
    switch (v.getId())
    {
      case R.id.button_set:
        mClient.putData(Const.DATA_PATH + "/" + Const.DATA_KEY_TOPIC, Const.DATA_KEY_TOPIC, mTopicDataEdit.getText().toString());
        break;
      default:
        // Nothing, how did you come here ?
    }
  }
}
