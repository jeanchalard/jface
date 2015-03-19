package com.j.jface.face;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class Sensors implements SensorEventListener
{
  final SensorManager mSensorManager;
  final Sensor mBarometer;
  final Sensor mVector;

  public float mPressure = 0;
  public float mNormal = 0;

  public Sensors(final Context ctx) {
    mSensorManager = (SensorManager)ctx.getSystemService(Context.SENSOR_SERVICE);
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
