package com.j.jface.org;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
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
}
