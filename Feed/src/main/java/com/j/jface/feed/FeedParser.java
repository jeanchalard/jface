package com.j.jface.feed;

import android.support.annotation.NonNull;

import com.google.android.gms.wearable.PutDataMapRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

public abstract class FeedParser
{
  @NonNull abstract PutDataMapRequest parseStream(@NonNull final InputStream src) throws IOException;

  String find(@NonNull final BufferedReader src, @NonNull final String s) throws IOException {
    int index = 0;
    int expected = s.codePointAt(index);
    int offset = 0;
    int read = src.read();
    final StringBuffer sb = new StringBuffer();
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
      offset += 1;
      read = src.read();
    }
    return null;
  }
}
