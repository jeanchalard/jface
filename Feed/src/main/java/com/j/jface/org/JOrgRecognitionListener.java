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
  final SoundSource mSource;

  public JOrgRecognitionListener(final SoundSource source)
  {
    mSource = source;
  }

  @Override public void onReadyForSpeech(final Bundle params) {}
  @Override public void onBeginningOfSpeech() {}
  @Override public void onBufferReceived(final byte[] buffer) {}
  @Override public void onEndOfSpeech() {}

  @Override public void onRmsChanged(final float rmsdB)
  {
    mSource.setLastSoundLevel(rmsdB);
  }

  @Override public void onError(final int error)
  {
    Log.e("Recog", "error " + error);
    switch (error)
    {
      case SpeechRecognizer.ERROR_NO_MATCH:
      case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
      mSource.restartListening();
    }
  }

  @Override public void onEvent(final int eventType, final Bundle params)
  {
    Log.e("Recog", "event " + eventType + " " + params);
  }

  @Override public void onResults(final Bundle results)
  {
    final ArrayList<String> r = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
    if (r != null) mSource.onResults(r);
    mSource.restartListening();
  }

  @Override public void onPartialResults(final Bundle partialResults)
  {
    Log.e("Recog", "partial " + partialResults);
    final ArrayList<String> r = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
    if (r != null) mSource.onPartialResults(r);
  }
}
