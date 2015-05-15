package com.j.jface.feed;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.EditText;

import com.j.jface.Const;
import com.j.jface.R;

public class TabDebugTools extends WrappedFragment implements View.OnClickListener
{
  @NonNull final Client mClient;
  @NonNull final EditText mDataEdit;

  public TabDebugTools(@NonNull final Args a, @NonNull final Client b)
  {
    super(a.inflater.inflate(R.layout.debug_app_tab_debug_tools, a.container, false));
    mClient = b;
    mDataEdit = (EditText)mView.findViewById(R.id.adHocDataEdit);
    mView.findViewById(R.id.button_set).setOnClickListener(this);
  }

  @Override public void onClick(final View v)
  {
    switch (v.getId())
    {
      case R.id.button_set:
        mClient.putData(Const.DATA_PATH + "/" + Const.DATA_KEY_ADHOC,
         Const.DATA_KEY_ADHOC, mDataEdit.getText().toString());
        break;
    }
  }
}
