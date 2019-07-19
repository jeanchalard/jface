package com.j.jface.face;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import androidx.annotation.NonNull;

public class Sensors implements SensorEventListener
{
  @NonNull private final SensorManager mSensorManager;
  @NonNull private final Sensor mBarometer;
  @NonNull private final Sensor mVector;

  public float mPressure = 0;
  public float mNormal = 0;

  public Sensors(@NonNull final Context ctx) {
    final SensorManager sm = (SensorManager)ctx.getSystemService(Context.SENSOR_SERVICE);
    if (null == sm) throw new RuntimeException("Null sensor manager from the framework... argh.");
    mSensorManager = sm;
    mBarometer = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
    mVector = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
  }

  public void start() {
    mSensorManager.registerListener(this, mBarometer, SensorManager.SENSOR_DELAY_NORMAL);
    mSensorManager.registerListener(this, mVector, SensorManager.SENSOR_DELAY_GAME);
  }

  public void stop() {
    mSensorManager.unregisterListener(this);
  }

  @Override
  public void onSensorChanged(final SensorEvent event)
  {
    if (event.sensor == mBarometer)
      mPressure = event.values[0];
//    else if (event.sensor == mVector)
//    {
//      double sin = Math.sqrt(1 - event.values[3] * event.values[3]);
//      Log.e("ROTATE", String.format("%.3f %.3f %.3f %.3f", event.values[0] / sin, event.values[1] / sin, event.values[2] / sin, event.values[3]));
//    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
