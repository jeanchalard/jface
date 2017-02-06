package com.j.jface.org;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

// An EditTextVersion that reports cursor moves to a listener.
public class SelReportEditText extends EditText
{
  public interface CursorListener
  {
    void onCursorMoved(@NonNull final SelReportEditText editText, final int selStart, final int selEnd);
  }
  @Nullable public CursorListener mListener;

  public SelReportEditText(final Context context) { super(context); }
  public SelReportEditText(final Context context, final AttributeSet attrs) { super(context, attrs); }
  public SelReportEditText(final Context context, final AttributeSet attrs, final int defStyleAttr) { super(context, attrs, defStyleAttr); }
  public SelReportEditText(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) { super(context, attrs, defStyleAttr, defStyleRes); }

  @Override protected void onSelectionChanged(int selStart, int selEnd)
  {
    super.onSelectionChanged(selStart, selEnd);
    if (null != mListener) mListener.onCursorMoved(this, selStart, selEnd);
  }

  @Override public void onEditorAction(final int actionCode)
  {
    if (EditorInfo.IME_ACTION_DONE == actionCode)
    {
      clearFocus();
      final InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(getWindowToken(), 0);
    }
  }

  /*
  @Override public void onFocusChanged(final boolean gainFocus, final int direction, final Rect previouslyFocusedRect)
  {
    if (gainFocus)
    {
      final InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.showSoftInput(this, 0);
    }
  }*/
}
