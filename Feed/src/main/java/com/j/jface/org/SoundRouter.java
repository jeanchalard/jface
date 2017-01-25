package com.j.jface.org;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import java.util.ArrayList;

// Class in charge of routing the speech output of the SoundSource
// to the correct destination, typically a text field.
public class SoundRouter implements View.OnFocusChangeListener
{
  @Nullable private EditText mFocusedEditText;
  @NonNull private CharSequence mTextBeforeCursor;
  @NonNull private CharSequence mTextAfterCursor;

  public SoundRouter()
  {
    mFocusedEditText = null;
    mTextBeforeCursor = "";
    mTextAfterCursor = "";
  }

  @Override public void onFocusChange(@NonNull final View editText, final boolean b)
  {
    if (b)
      mFocusedEditText = (EditText) editText;
    else if (editText == mFocusedEditText)
    {
      mFocusedEditText = null;
      mTextBeforeCursor = "";
      mTextAfterCursor = "";
      return;
    }
    final int selStart = mFocusedEditText.getSelectionStart();
    final int selEnd = mFocusedEditText.getSelectionEnd();
    final CharSequence text = mFocusedEditText.getText();
    mTextBeforeCursor = text.subSequence(0, selStart);
    mTextAfterCursor = text.subSequence(selEnd, text.length() - selEnd);
  }

  public void onPartialResults(@NonNull final ArrayList<String> results)
  {
    mFocusedEditText.setText(TextUtils.concat(mTextBeforeCursor, results.get(0), mTextAfterCursor));
  }

  public void onResults(@NonNull final ArrayList<String> results)
  {
    mFocusedEditText.setText(TextUtils.concat(mTextBeforeCursor, results.get(0), mTextAfterCursor));
  }
}
