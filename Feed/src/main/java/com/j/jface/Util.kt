package com.j.jface

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.PersistableBundle
import com.google.android.gms.wearable.DataMap
import com.j.jface.Util.arrayListFromIntArray

private operator fun PersistableBundle.set(key : String, value : Any)
{
  when (value)
  {
    // Careful : this converts ArrayList<Int> to Int[]. The method below does the opposite.
    // There are types not handled here but that could be, like list of strings or so.
    is String -> putString(key, value)
    is Int -> putInt(key, value)
    is Long -> putLong(key, value)
    is Double -> putDouble(key, value)
    is Boolean -> putBoolean(key, value)
    is LongArray -> putLongArray(key, value)
    is ArrayList<*> -> putIntArray(key, (value as java.util.ArrayList<Int>).toIntArray())
    else -> throw UnsupportedOperationException("Can't put a ${value.javaClass} (${value}) into a PersistableBundle")
  }
}
private operator fun DataMap.set(key : String, value : Any)
{
  when (value)
  {
    // Careful : this converts Int[] to ArrayList<Int>. The method above does the opposite.
    // There are types not handled here but that could be, like list of strings or so.
    is String -> putString(key, value)
    is Int -> putInt(key, value)
    is Long -> putLong(key, value)
    is Double -> putDouble(key, value)
    is Boolean -> putBoolean(key, value)
    is IntArray -> putIntegerArrayList(key, arrayListFromIntArray(value))
    is LongArray -> putLongArray(key, value)
    else -> throw UnsupportedOperationException("Can't put a ${value.javaClass} (${value}) into a DataMap")
  }
}

fun DataMap.toPersistableBundle() : PersistableBundle
{
  val target = PersistableBundle()
  for (key in this.keySet()) target[key] = this[key]
  return target
}
fun PersistableBundle.toDataMap() : DataMap
{
  val target = DataMap()
  for (key in this.keySet()) target[key] = this[key]
  return target
}

fun <T : Comparable<T>> clamp(min : T, value : T, max : T) : T = if (value < min) min else if (value > max) max else value

fun Context.nextNotifId(channel : String) : Int
{
  val persist : SharedPreferences = getSharedPreferences(Const.INTERNAL_PERSISTED_VALUES_FILES, Context.MODE_PRIVATE)
  val notifId = persist.getInt(channel, 1)
  persist.edit().putInt(channel, notifId + 1).apply()
  return notifId
}

val Context.notifManager : NotificationManager
  get() = getSystemService(NotificationManager::class.java)
