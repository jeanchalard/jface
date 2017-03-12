package com.j.jface.org;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.v13.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.j.jface.R;

import java.util.ArrayList;
import java.util.Locale;

// An object that acts as the source for sound / speech and implements visualisation and
// all the technical details of the lifecycle of the speech recognition objects.
public class SoundSource implements View.OnClickListener, RecognitionListener
{
  @NonNull private final SpeechRecognizer mSpeechRecognizer;
  @NonNull private final SoundVisualizer mSoundVisualizer;
  @NonNull private final View mNoSound;
  @NonNull private final Intent mListeningIntent;
  @NonNull private SoundRouter mRouter;
  private boolean mActive;

  // The SoundSource view group needs to be compliant with the spec :
  // - Contain a "no_sound" view that will be toggled visible when the source is off and invisible when it's on
  // - Contain a "sound_visualizer" view with 5 children, the minHeight of which will be animated to reflect sound levels
  public SoundSource(@NonNull final Activity activity, @NonNull final ViewGroup soundSource)
  {
    mRouter = SoundRouter.Sink;
    mSoundVisualizer = new SoundVisualizer(activity.getMainLooper(), (ViewGroup)soundSource.findViewById(R.id.sound_visualizer));
    mListeningIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    mListeningIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr_FR");
    mListeningIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
    mListeningIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    mListeningIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
    mListeningIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000);
    mListeningIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000);
    mListeningIntent.putExtra("android.speech.extra.DICTATION_MODE", true);
    mListeningIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.j.jface");

    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
      ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, 1);

    mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
    mSpeechRecognizer.setRecognitionListener(this);
    mNoSound = soundSource.findViewById(R.id.no_sound);
    mNoSound.setVisibility(View.INVISIBLE);
    mActive = mNoSound.getVisibility() == View.INVISIBLE;
    soundSource.setOnClickListener(this);
  }

  public void setRouter(final EditTextSoundRouter router)
  {
    mRouter = router;
  }

  // Core methods

  public void stopListening()
  {
    mSoundVisualizer.resetToNull();
    mSpeechRecognizer.stopListening();
    mSpeechRecognizer.destroy();
  }

  public void startListening()
  {
    if (!mActive || !mRouter.isRouting()) return;
    mSoundVisualizer.resetToActive();
    mSpeechRecognizer.destroy();
    mSpeechRecognizer.setRecognitionListener(this);
    mSpeechRecognizer.startListening(mListeningIntent);
  }


  // Click listener, to turn sound on or off

  @Override public void onClick(final View v)
  {
    mActive = !mActive;
    mNoSound.setVisibility(mActive ? View.INVISIBLE : View.VISIBLE);
    if (mActive)
      startListening();
    else
      stopListening();
  }


  // Recognition listener API, starting with stubs

  @Override public void onReadyForSpeech(final Bundle params) {}
  @Override public void onBeginningOfSpeech() {}
  @Override public void onBufferReceived(final byte[] buffer) {}
  @Override public void onEndOfSpeech() {}
  @Override public void onEvent(final int eventType, final Bundle params)
  {
    Log.e("Recog", "event " + eventType + " " + params);
  }

  @Override public void onRmsChanged(final float rmsdB)
  {
    mSoundVisualizer.setLastSoundLevel(rmsdB >= 0 ? rmsdB : 0);
  }

  @Override public void onError(final int error)
  {
    Log.e("Recog", "error " + error);
    switch (error)
    {
      case SpeechRecognizer.ERROR_NO_MATCH:
      case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
        startListening();
    }
  }

  @Override public void onResults(final Bundle results)
  {
    final ArrayList<String> r = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
    if (r != null) mRouter.onResults(r);
    startListening();
  }

  @Override public void onPartialResults(final Bundle partialResults)
  {
    final ArrayList<String> r = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
    if (r != null) mRouter.onPartialResults(r);
  }


  // Lifecycle

  public void onPause() { stopListening(); }
  public void onResume() { startListening(); }
}
