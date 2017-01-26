package com.j.jface.org;

import android.support.annotation.NonNull;

import java.util.ArrayList;

// Interface for a class that routes sound out of a SoundSource.
public interface SoundRouter
{
  void onPartialResults(@NonNull ArrayList<String> results);
  void onResults(@NonNull ArrayList<String> results);
  boolean isRouting();

  class SinkRouter implements SoundRouter
  {
    public void onPartialResults(@NonNull final ArrayList<String> results) {}
    public void onResults(@NonNull final ArrayList<String> results) {}
    public boolean isRouting() { return false; }
  }
  @NonNull SinkRouter Sink = new SinkRouter();
}
