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
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.wearable.DataMap;
import com.j.jface.Const;
import com.j.jface.R;
import com.j.jface.client.Client;
import com.j.jface.feed.views.PaletteView;
import com.j.jface.lifecycle.WrappedFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    mTopicDataEdit.setTextColor(Const.TOPIC_DEFAULT_COLOR);
    mPalette = (PaletteView)mView.findViewById(R.id.messagesFragment_palette);
    mPalette.addOnColorSetListener(this);
    mDataFetched = false;
    final Activity activity = a.fragment.getActivity();
    b.getData(Const.DATA_PATH + "/" + Const.DATA_KEY_TOPIC, (path, dataMap) ->
     activity.runOnUiThread(() ->
    {
      mDataFetched = true;
      final String topic = dataMap.getString(Const.DATA_KEY_TOPIC);
      if (null == topic) return;
      final int[] starts = getLineStartOffsets(topic);
      final SpannableString text = new SpannableString(topic);
      final ArrayList<Integer> colors = dataMap.getIntegerArrayList(Const.DATA_KEY_TOPIC_COLORS);
      if (null != colors && starts.length == colors.size())
        for (int i = 0; i < colors.size(); ++i)
        {
          final int start = starts[i];
          final int end = Math.max(start, i + 1 >= starts.length ? topic.length() : starts[i + 1]);
          text.setSpan(new ForegroundColorSpan(colors.get(i)), start, end, Spanned.SPAN_PARAGRAPH);
        }
      mTopicDataEdit.setText(text);
    }));
  }

  @Override public void afterTextChanged(@NonNull final Editable s)
  {
    if (!mDataFetched) return;
    try
    {
      final int[] starts = getLineStartOffsets(s.toString());
      final ArrayList<Integer> colors = new ArrayList<>(starts.length);
      for (int i = 0; i < starts.length; ++i) colors.add(Const.TOPIC_DEFAULT_COLOR);
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
      dataMap.putString(Const.DATA_KEY_TOPIC, mTopicDataEdit.getText().toString());
      dataMap.putIntegerArrayList(Const.DATA_KEY_TOPIC_COLORS, colors);
      mClient.putData(Const.DATA_PATH + "/" + Const.DATA_KEY_TOPIC, dataMap);
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
      final Editable text = mTopicDataEdit.getText();
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
    final Editable text = mTopicDataEdit.getText();
    for (final ForegroundColorSpan span : text.getSpans(0, text.length() - 1, ForegroundColorSpan.class)) text.removeSpan(span);
  }
}
