package com.j.jface.feed.fragments;

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
    b.getData(Const.DATA_PATH + "/" + Const.DATA_KEY_TOPIC, new Client.GetDataCallback() {
      public void run(@NonNull final String path, @NonNull final DataMap dataMap) {
        a.fragment.getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            mDataFetched = true;
            final String topic = dataMap.getString(Const.DATA_KEY_TOPIC);
            if (null == topic) return;
            final int[] starts = getLineStartOffsets(topic);
            final SpannableString text = new SpannableString(topic);
            final ArrayList<Integer> colors = dataMap.getIntegerArrayList(Const.DATA_KEY_TOPIC_COLORS);
            if (null != colors) // Just in case
              for (int i = 0; i < colors.size(); ++i)
              {
                final int end = i + 1 >= starts.length ? topic.length() - 1 : starts[i + 1];
                text.setSpan(new ForegroundColorSpan(colors.get(i)), starts[i], end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
              }
            mTopicDataEdit.setText(text);
          }
        });
      }
    });
  }

  @Override public void afterTextChanged(@NonNull final Editable s)
  {
    if (!mDataFetched) return;
    final int[] starts = getLineStartOffsets(s.toString());
    final ArrayList<Integer> colors = new ArrayList<>(starts.length);
    for (int i = 0; i < starts.length; ++i) colors.add(Const.TOPIC_DEFAULT_COLOR);
    for (final ForegroundColorSpan span : s.getSpans(0, s.length() - 1, ForegroundColorSpan.class))
    {
      final int spanStart = s.getSpanStart(span);
      final int index = Arrays.binarySearch(starts, spanStart);
      if (index >= 0) // Should always be, but just in case
        colors.set(index, span.getForegroundColor());
    }
    final DataMap dataMap = new DataMap();
    dataMap.putString(Const.DATA_KEY_TOPIC, mTopicDataEdit.getText().toString());
    dataMap.putIntegerArrayList(Const.DATA_KEY_TOPIC_COLORS, colors);
    mClient.putData(Const.DATA_PATH + "/" + Const.DATA_KEY_TOPIC, dataMap);
  }

  private int[] getLineStartOffsets(@NonNull final String s)
  {
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
    final Editable text = mTopicDataEdit.getText();
    final int cursorStart = Selection.getSelectionStart(text);
    final int cursorEnd = Selection.getSelectionEnd(text);

    int start = 0, end = 0;
    final String[] lines = text.toString().split("\n");
    for (final String line : lines)
    {
      end = start + line.length();
      if (cursorStart <= end && cursorEnd >= start) text.setSpan(new ForegroundColorSpan(color), start, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
      start = end + 1;
    }
    afterTextChanged(text);
  }
}
