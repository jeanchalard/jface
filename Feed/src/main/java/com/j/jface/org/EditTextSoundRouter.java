package com.j.jface.org;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import java.util.ArrayList;

// Class in charge of routing the speech output of the SoundSource
// to the correct destination, typically a text field.
public class EditTextSoundRouter implements View.OnFocusChangeListener, SelReportEditText.CursorListener, SoundRouter
{
  @NonNull private final SoundSource mSoundSource;
  @Nullable private EditText mFocusedEditText;
  @NonNull private CharSequence mTextBeforeCursor;
  @NonNull private CharSequence mTextAfterCursor;
  private boolean mEditing;

  public EditTextSoundRouter(@NonNull final SoundSource soundSource)
  {
    mSoundSource = soundSource;
    soundSource.setRouter(this);
    mFocusedEditText = null;
    mTextBeforeCursor = "";
    mTextAfterCursor = "";
    mEditing = false;
  }

  @Override public void onFocusChange(@NonNull final View editText, final boolean b)
  {
    if (b)
    {
      mFocusedEditText = (EditText)editText;
      mSoundSource.startListening();
    }
    else if (editText == mFocusedEditText)
    {
      mFocusedEditText = null;
      mSoundSource.stopListening();
    }
    reloadBuffers();
  }

  private void reloadBuffers()
  {
    if (null == mFocusedEditText)
    {
      mTextBeforeCursor = "";
      mTextAfterCursor = "";
      return;
    }
    final int selStart = mFocusedEditText.getSelectionStart();
    final int selEnd = mFocusedEditText.getSelectionEnd();
    final CharSequence text = mFocusedEditText.getText();
    mTextBeforeCursor = text.subSequence(0, selStart);
    mTextAfterCursor = text.subSequence(selEnd, text.length());
    if (mTextBeforeCursor.length() > 0)
    {
      if (mTextAfterCursor.length() > 0)
      {
        if (mTextAfterCursor.charAt(0) == 0x20 && mTextBeforeCursor.charAt(mTextBeforeCursor.length() - 1) != 0x20)
          mTextBeforeCursor = TextUtils.concat(mTextBeforeCursor, " ");
        else if (mTextBeforeCursor.charAt(mTextBeforeCursor.length() - 1) == 0x20 && mTextAfterCursor.charAt(0) != 0x20)
          mTextAfterCursor = TextUtils.concat(" ", mTextAfterCursor);
      } else if (mTextBeforeCursor.charAt(mTextBeforeCursor.length() - 1) != 0x20) // Before > 0, After <= 0
        mTextBeforeCursor = TextUtils.concat(mTextBeforeCursor, " ");
    }
    else if (mTextAfterCursor.length() > 0) // && Before <= 0
      mTextAfterCursor = TextUtils.concat(" ", mTextAfterCursor);
  }

  @Override public void onCursorMoved(@NonNull final SelReportEditText editText, final int selStart, final int selEnd)
  {
    if (mFocusedEditText != editText || mEditing) return;
    reloadBuffers();
    mSoundSource.startListening();
  }

  @Override
  public void onPartialResults(@NonNull final ArrayList<String> results)
  {
    if (null == mFocusedEditText) return;
    mEditing = true;
    mFocusedEditText.beginBatchEdit();
    final String result;
    if (mTextBeforeCursor.length() > 0) result = results.get(0);
    else
    {
      final String r = results.get(0);
      if (r.length() > 1)
        result = r.substring(0, 1).toUpperCase() + r.substring(1);
      else result = r.toUpperCase();
    }
    mFocusedEditText.setText(TextUtils.concat(mTextBeforeCursor, result, mTextAfterCursor));
    mFocusedEditText.setSelection(mTextBeforeCursor.length() + result.length());
    mFocusedEditText.endBatchEdit();
    mEditing = false;
  }

  @Override
  public void onResults(@NonNull final ArrayList<String> results)
  {
    onPartialResults(results);
    reloadBuffers();
  }

  @Override
  public boolean isRouting()
  {
    return null != mFocusedEditText;
  }
}
