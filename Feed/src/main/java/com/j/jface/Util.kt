package com.j.jface

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
