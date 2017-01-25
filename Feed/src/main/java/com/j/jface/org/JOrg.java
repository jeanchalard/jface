package com.j.jface.org;

import android.Manifest;
import android.animation.LayoutTransition;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.j.jface.R;
import com.j.jface.lifecycle.WrappedActivity;
import com.j.jface.org.todo.Todo;
import com.j.jface.org.todo.TodoUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Main activity class for JOrg.
 */
public class JOrg extends WrappedActivity
{
  private static final int LAYOUT_ANIMATION_DURATION = 100;
  private final SpeechRecognizer mSpeechRecognizer;
  private final RecognitionListener mRecognitionListener;
  private final SoundVisualizer mSoundVisualizer;
  private final Intent mListeningIntent;

  public JOrg(@NonNull Args args)
  {
    super(args);
    mA.setContentView(R.layout.org_top);
    final LinearLayout top = (LinearLayout)mA.findViewById(R.id.todoList);

    mSoundVisualizer = new SoundVisualizer(mA.getMainLooper(), (ViewGroup)mA.findViewById(R.id.sound_visualizer));
    mListeningIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    mListeningIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.FRANCE);
    mListeningIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
    mListeningIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    mListeningIntent.putExtra("android.speech.extra.DICTATION_MODE", true);
    mListeningIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.j.jface");
    if (ContextCompat.checkSelfPermission(mA, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
    {
      ActivityCompat.requestPermissions(mA, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
      mSpeechRecognizer = null;
      mRecognitionListener = null;
    }
    else
    {
      mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(mA);
      mRecognitionListener = new JOrgRecognitionListener(this);
    }

    Todo tt[] = {
     new Todo("foo1", null, null,
      Arrays.asList(new Todo("subfoo1"), new Todo("subfoo2", null, null, Arrays.asList(new Todo("subsubfoo1")), null, 0)),
      null, 0), new Todo("foo2")//, new Todo("foo3"), new Todo("foo4")
    };

    top.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
    top.getLayoutTransition().setDuration(LAYOUT_ANIMATION_DURATION);
    addTodos(Arrays.asList(tt), top, 0);
  }

  public void stopListening()
  {
    mSpeechRecognizer.destroy();
  }

  public void startListening()
  {
    mSpeechRecognizer.setRecognitionListener(mRecognitionListener);
    mSpeechRecognizer.startListening(mListeningIntent);
  }

  // More efficient than stop start :(
  public void restartListening()
  {
    mSpeechRecognizer.cancel();
    mSpeechRecognizer.startListening(mListeningIntent);
  }

  public void setLastSoundLevel(final float db)
  {
    mSoundVisualizer.setLastSoundLevel(db >= 0 ? db : 0);
  }

  public void onPause() { stopListening(); }
  public void onResume() { startListening(); }

  private void addTodos(final List<Todo> l, final LinearLayout topView, final int shift)
  {
    for (final Todo todo : l)
    {
      topView.addView(inflateTodo(todo, shift));
      final View separator = new View(mA);
      separator.setBackgroundColor(0xFFA0A0A0);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1);
      lp.setMargins(20 + shift, 0, 20, 0);
      separator.setLayoutParams(lp);
      topView.addView(separator);
      if (null != todo.mChildren) addTodos(todo.mChildren, topView, shift + 25);
    }
  }

  private View inflateTodo(final Todo t, final int shift)
  {
    final LinearLayout v = (LinearLayout)mA.getLayoutInflater().inflate(R.layout.todo, null);
    final EditText et = ((EditText)v.findViewById(R.id.todoText));
    final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lp.setMargins(shift, 0, 0, 0);
    v.setLayoutParams(lp);
    final LayoutTransition lt = v.getLayoutTransition();
    lt.setDuration(LayoutTransition.CHANGE_APPEARING, LAYOUT_ANIMATION_DURATION);
    lt.setDuration(LayoutTransition.CHANGE_DISAPPEARING, LAYOUT_ANIMATION_DURATION);
    lt.setStartDelay(LayoutTransition.APPEARING, 0);
    et.setText(t.mText);
    final View expansion = v.findViewById(R.id.todoExpanded);
    final ImageButton b = (ImageButton)v.findViewById(R.id.todoExpandButton);
    b.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(final View clicked)
      {
        if (expansion.getVisibility() == View.VISIBLE)
          expansion.setVisibility(View.GONE);
        else
          expansion.setVisibility(View.VISIBLE);
      }
    });
    expansion.setVisibility(View.GONE);
    populateExpandedTodo(v, t);
    return v;
  }

  private void populateExpandedTodo(final View v, final Todo t)
  {
    final Spinner s = (Spinner)v.findViewById(R.id.deadLineType);
    final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mA,
     R.array.deadline_type, android.R.layout.simple_spinner_dropdown_item);
    s.setAdapter(adapter);
    final Button tc = (Button)v.findViewById(R.id.todoTimeConstraint);
    tc.setText(TodoUtil.timeConstraintString(t.mPlanning.mTimeConstraint));
    final Button w = (Button)v.findViewById(R.id.todoWhere);
    w.setText(TodoUtil.whereString(t.mPlanning.mWhere));
  }
}
