package com.j.jface.feed;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.wearable.DataMap;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;

public abstract class FeedParser
{
  @NonNull abstract DataMap parseStream(@NonNull final String dataName, @NonNull final BufferedInputStream src) throws IOException;

  @Nullable protected String find(@NonNull final BufferedReader src, @NonNull final String s) throws IOException {
    int index = 0;
    int expected = s.codePointAt(index);
    int read = src.read();
    final StringBuilder sb = new StringBuilder();
    while (read != -1) {
      if (read == expected) {
        index += Character.charCount(read);
        if (index >= s.length()) {
          return sb.toString();
        }
        expected = s.codePointAt(index);
      } else {
        if (0 != index) expected = s.codePointAt(0);
        index = 0;
      }
      sb.appendCodePoint(read);
      read = src.read();
    }
    return null;
  }
}
