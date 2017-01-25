package com.j.jface.org;

import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

// The recognition listener for JOrg. This streams voice, possibly cleans up the stream,
// and sends results back to the main activity.
public class JOrgRecognitionListener implements RecognitionListener
{
  final JOrg mParent;

  public JOrgRecognitionListener(final JOrg parent)
  {
    mParent = parent;
  }

  @Override public void onReadyForSpeech(final Bundle params)
  {
    Log.e("Recog", "ready");
  }

  @Override public void onBeginningOfSpeech()
  {
    Log.e("Recog", "Begin");
  }

  @Override public void onRmsChanged(final float rmsdB)
  {
    Log.e("Recog", "RMS " + rmsdB);
    mParent.setLastSoundLevel(rmsdB);
  }

  @Override public void onBufferReceived(final byte[] buffer)
  {
    Log.e("Recog", "received");
  }

  @Override public void onEndOfSpeech()
  {
    Log.e("Recog", "End");
  }

  @Override public void onError(final int error)
  {
    Log.e("Recog", "error " + error);
    switch (error)
    {
      case SpeechRecognizer.ERROR_NO_MATCH:
      case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
      mParent.restartListening();
    }
  }

  @Override public void onResults(final Bundle results)
  {
    Log.e("Recog", "results " + results);
    final ArrayList<String> x = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
    if (x != null) Log.e(">>>", "" + x);
    mParent.restartListening();
  }

  @Override public void onPartialResults(final Bundle partialResults)
  {
    Log.e("Recog", "partial " + partialResults);
    final ArrayList<String> x = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
    if (x != null) Log.e(">>>", "" + x);
  }

  @Override public void onEvent(final int eventType, final Bundle params)
  {
    Log.e("Recog", "event " + eventType + " " + params);
  }
}
