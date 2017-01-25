package com.j.jface.org;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.v13.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;

import com.j.jface.R;

import java.util.ArrayList;
import java.util.Locale;

// An object that acts as the source for sound / speech and implements visualisation and
// all the technical details of the lifecycle of the speech recognition objects.
public class SoundSource implements View.OnClickListener
{
  @NonNull private final SpeechRecognizer mSpeechRecognizer;
  @NonNull private final RecognitionListener mRecognitionListener;
  @NonNull private final SoundVisualizer mSoundVisualizer;
  @NonNull private final View mNoSound;
  @NonNull private final Intent mListeningIntent;
  @NonNull private final SoundRouter mRouter;
  private boolean mActive;

  // The SoundSource view group needs to be compliant with the spec :
  // - Contain a "no_sound" view that will be toggled visible when the source is off and invisible when it's on
  // - Contain a "sound_visualizer" view with 5 children, the minHeight of which will be animated to reflect sound levels
  public SoundSource(@NonNull final Activity activity, @NonNull final SoundRouter router, @NonNull final ViewGroup soundSource)
  {
    mRouter = router;
    mSoundVisualizer = new SoundVisualizer(activity.getMainLooper(), (ViewGroup)soundSource.findViewById(R.id.sound_visualizer));
    mListeningIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    mListeningIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.FRANCE);
    mListeningIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
    mListeningIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    mListeningIntent.putExtra("android.speech.extra.DICTATION_MODE", true);
    mListeningIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.j.jface");

    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
    {
      ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
      mSpeechRecognizer = null;
      mRecognitionListener = null;
    }
    else
    {
      mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
      mRecognitionListener = new JOrgRecognitionListener(this);
    }

    mNoSound = soundSource.findViewById(R.id.no_sound);
    mNoSound.setVisibility(View.INVISIBLE);
    mActive = mNoSound.getVisibility() == View.INVISIBLE;
    soundSource.setOnClickListener(this);
  }

  public void stopListening()
  {
    mSpeechRecognizer.destroy();
  }

  public void startListening()
  {
    if (!mActive) return;
    mSpeechRecognizer.setRecognitionListener(mRecognitionListener);
    mSpeechRecognizer.startListening(mListeningIntent);
  }

  // More efficient than stop start :(
  public void restartListening()
  {
    if (!mActive) return;
    mSpeechRecognizer.cancel();
    mSpeechRecognizer.startListening(mListeningIntent);
  }

  public void setLastSoundLevel(final float db)
  {
    mSoundVisualizer.setLastSoundLevel(db >= 0 ? db : 0);
  }

  public void onPause() { stopListening(); }
  public void onResume() { startListening(); }

  @Override public void onClick(final View v)
  {
    mActive = !mActive;
    mNoSound.setVisibility(mActive ? View.INVISIBLE : View.VISIBLE);
    if (mActive)
    {
      mSoundVisualizer.resetToActive();
      startListening();
    }
    else
    {
      mSoundVisualizer.resetToNull();
      stopListening();
    }
  }

  public void onPartialResults(@NonNull ArrayList<String> results)
  {
    mRouter.onPartialResults(results);
  }

  public void onResults(@NonNull ArrayList<String> results)
  {
    mRouter.onResults(results);
  }
}
