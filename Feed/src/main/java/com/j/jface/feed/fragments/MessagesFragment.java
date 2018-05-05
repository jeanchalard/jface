package com.j.jface.feed.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.widget.EditText;

import com.google.android.gms.wearable.DataMap;
import com.j.jface.Const;
import com.j.jface.R;
import com.j.jface.feed.views.PaletteView;
import com.j.jface.feed.views.SnackbarRegistry;
import com.j.jface.lifecycle.WrappedFragment;
import com.j.jface.wear.Wear;

import java.util.ArrayList;
import java.util.Arrays;

import kotlin.Unit;

/**
 * A fragment for showing and managing arbitrary messages.
 */
public class MessagesFragment extends WrappedFragment implements TextWatcher, PaletteView.OnColorSetListener
{
  @NonNull private final Fragment mF;
  @NonNull private final Wear mWear;
  @NonNull private final EditText mUserMessageDataEdit;
  @NonNull private final PaletteView mPalette;
  private boolean mDataFetched; // Replace this if this class or the use of this member ever become any more than elementary

  public MessagesFragment(@NonNull final WrappedFragment.Args a, @NonNull final Wear b)
  {
    super(a.inflater.inflate(R.layout.fragment_messages, a.container, false));
    mF = a.fragment;
    mWear = b;
    mUserMessageDataEdit = mView.findViewById(R.id.messagesFragment_userMessageDataEdit);
    mUserMessageDataEdit.addTextChangedListener(this);
    mUserMessageDataEdit.setTextColor(Const.USER_MESSAGE_DEFAULT_COLOR);
    mPalette = mView.findViewById(R.id.messagesFragment_palette);
    mPalette.addOnColorSetListener(this);
    mDataFetched = false;
    final Activity activity = a.fragment.getActivity();
    b.getData(Const.DATA_PATH + "/" + Const.DATA_KEY_USER_MESSAGE, (path, dataMap) ->
    {
      activity.runOnUiThread(() ->
      {
        mDataFetched = true;
        final String userMessage = dataMap.getString(Const.DATA_KEY_USER_MESSAGE);
        if (null == userMessage) return;
        final int[] starts = getLineStartOffsets(userMessage);
        final SpannableString text = new SpannableString(userMessage);
        final ArrayList<Integer> colors = dataMap.getIntegerArrayList(Const.DATA_KEY_USER_MESSAGE_COLORS);
        if (null != colors && starts.length == colors.size())
          for (int i = 0; i < colors.size(); ++i)
          {
            final int start = starts[i];
            final int end = Math.max(start, i + 1 >= starts.length ? userMessage.length() : starts[i + 1]);
            text.setSpan(new ForegroundColorSpan(colors.get(i)), start, end, Spanned.SPAN_PARAGRAPH);
          }
        mUserMessageDataEdit.setText(text);
      });
      return Unit.INSTANCE;
    });
  }

  @Override protected void onResume()
  {
    SnackbarRegistry.INSTANCE.setSnackbarParent(mView);
  }

  @Override protected void onPause()
  {
    SnackbarRegistry.INSTANCE.unsetSnackbarParent(mView);
  }

  @Override public void afterTextChanged(@NonNull final Editable s)
  {
    if (!mDataFetched) return;
    try
    {
      final int[] starts = getLineStartOffsets(s.toString());
      final ArrayList<Integer> colors = new ArrayList<>(starts.length);
      for (int i = 0; i < starts.length; ++i) colors.add(Const.USER_MESSAGE_DEFAULT_COLOR);
      for (final ForegroundColorSpan span : s.getSpans(0, s.length() - 1, ForegroundColorSpan.class))
      {
        final int spanStart = s.getSpanStart(span);
        final int spanEnd = s.getSpanEnd(span);
        final int index = Arrays.binarySearch(starts, spanStart);
        if (index >= 0 && spanStart != spanEnd)
        {
          colors.set(index, span.getForegroundColor());
          final int expectedEnd = index + 1 >= starts.length ? s.length() : starts[index + 1];
          if (spanEnd != expectedEnd)
          {
            s.removeSpan(span);
            s.setSpan(span, spanStart, expectedEnd, Spanned.SPAN_PARAGRAPH);
          }
        } else
          s.removeSpan(span); // Removed the new line on which this was anchored
      }
      final DataMap dataMap = new DataMap();
      dataMap.putString(Const.DATA_KEY_USER_MESSAGE, mUserMessageDataEdit.getText().toString());
      dataMap.putIntegerArrayList(Const.DATA_KEY_USER_MESSAGE_COLORS, colors);
      mWear.putDataToCloud(Const.DATA_PATH + "/" + Const.DATA_KEY_USER_MESSAGE, dataMap);
    }
    catch (Exception e) { removeAllSpans(); }
  }

  private int[] getLineStartOffsets(@NonNull final String s)
  {
    if (s.length() <= 0) return new int[0];
    final String[] lines = s.split("\n");
    final int[] starts = new int[lines.length];
    int start = 0;
    for (int i = 0; i < lines.length; ++i)
    {
      starts[i] = start;
      start += lines[i].length() + 1;
    }
    return starts;
  }

  @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
  @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

  @Override public void onColorSet(final int color)
  {
    try
    {
      final Editable text = mUserMessageDataEdit.getText();
      final int cursorStart = Selection.getSelectionStart(text);
      final int cursorEnd = Selection.getSelectionEnd(text);

      int start = 0, end = 0;
      final String[] lines = text.toString().split("\n");
      for (final String line : lines)
      {
        end = start + line.length() + 1;
        if (cursorStart < end && cursorEnd >= start)
        {
          for (final ForegroundColorSpan span : text.getSpans(0, text.length() - 1, ForegroundColorSpan.class))
            if (text.getSpanStart(span) == start)
              text.removeSpan(span);
          text.setSpan(new ForegroundColorSpan(color), start, Math.min(end, text.length()), Spannable.SPAN_PARAGRAPH);
        }
        start = end;
      }
      afterTextChanged(text);
    }
    catch (Exception e) { removeAllSpans(); }
  }

  private void removeAllSpans()
  {
    final Editable text = mUserMessageDataEdit.getText();
    for (final ForegroundColorSpan span : text.getSpans(0, text.length() - 1, ForegroundColorSpan.class)) text.removeSpan(span);
  }
}
