package com.j.jface.feed;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.wearable.DataMap;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;

public abstract class FeedParser
{
  @NonNull abstract DataMap parseStream(@NonNull final String dataName, @NonNull final BufferedInputStream src, @Nullable Object arg) throws IOException, ParseException;

  @Nullable protected String find(@NonNull final BufferedReader src, @NonNull final String s) throws IOException {
    int index = 0;
    int expected = s.codePointAt(index);
    int read = src.read();
    final StringBuilder sb = new StringBuilder();
    while (read != -1)
    {
      if (read == expected)
      {
        index += Character.charCount(read);
        if (index >= s.length()) return sb.toString();
        expected = s.codePointAt(index);
      }
      else
      {
        if (0 != index) expected = s.codePointAt(0);
        index = 0;
      }
      sb.appendCodePoint(read);
      read = src.read();
    }
    return null;
  }
}
