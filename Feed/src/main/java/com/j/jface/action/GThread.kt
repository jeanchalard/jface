package com.j.jface.action

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.tasks.*
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.Wearable
import com.j.jface.action.wear.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

infix fun <A, B, C> ((A) -> B).then(g : (B) -> C) : (A) -> C { return { a -> g(this(a)) } }
inline fun <T, U> Task<T>.addOnCompleteListener(e : Executor, crossinline l : (Task<T>) -> U) : Task<T> = addOnCompleteListener(e, OnCompleteListener<T> { l(it) })
inline fun <T, U> Task<T>.continueWith(e : Executor, crossinline l : (T) -> U) : Task<U> = continueWith(e, Continuation<T, U> { l(it.result) })
inline fun <T, U> Task<T>.continueWith(e : Executor, crossinline l : () -> U) : Task<U> = continueWith(e, Continuation<T, U> { l() })
inline fun <T, U> Task<T>.continueWithTask(e : Executor, crossinline l : (Task<T>) -> Task<U>) : Task<U> = continueWithTask(e, Continuation<T, Task<U>> { l(it) })

fun <T> (() -> T).enqueue(t : GThread) = t.enqueue(this)
fun Runnable.enqueue(t : GThread) = t.enqueue(this)

/**
 * A handler thread tuned for using Google APIs.
 *
 * Specifically Wear, Firebase, Drive. This thread is also used to throw
 * random asynchronous stuff that should be separate from UI, especially
 * when they require total ordering with some of the actions that relate
 * to Google APIs or that simply look similar.
 */
class GThread(context : Context)
{
  companion object
  {
    private val ex = Executors.newSingleThreadExecutor()
  }
  private val dataClient = Wearable.getDataClient(context, Wearable.WearableOptions.Builder().setLooper(context.mainLooper).build())!!

  fun enqueue(a : Runnable) = ex.execute(a)
  fun <T> enqueue(a : () -> T) : Task<T>
  {
    val source = TaskCompletionSource<T>()
    ex.execute { val r = a.invoke(); source.setResult(r) }
    return source.task
  }
  fun <T> enqueue(a : (DataClient) -> T) : Task<T>
  {
    val source = TaskCompletionSource<T>()
    ex.execute { val r = a.invoke(dataClient); source.setResult(r) }
    return source.task
  }
  // For calling from Java
  fun <T, R> executeInOrder(a : () -> T, b : (T) -> R) = enqueue(a).continueWith(ex, b)
  fun <T, R> executeInOrder(a : () -> T, b : () -> R) = enqueue(a).continueWith(ex, b)

  fun getDataSynchronously(path : String) : DataMap = Tasks.await(GetDataAction(dataClient, ex, path))
  fun getData(path : String, callback : (String, DataMap) -> Unit) = enqueue(GetDataAction(ex, path, callback))
  fun getBitmap(path : String, key : String, callback : (String, String, Bitmap?) -> Unit) = enqueue(GetBitmapAction(ex, path, key, callback))
  fun deleteData(path : String) = enqueue(DeleteDataAction(path))
  fun deleteAllData() = enqueue(DeleteAllDataAction(ex))
  fun putData(path : String,               v : DataMap)        = enqueue(PutDataAction(path, v))
  fun putData(path : String, key : String, v : String)         = enqueue(PutDataAction(path, key, v))
  fun putData(path : String, key : String, v : Boolean)        = enqueue(PutDataAction(path, key, v))
  fun putData(path : String, key : String, v : Long)           = enqueue(PutDataAction(path, key, v))
  fun putData(path : String, key : String, v : Asset)          = enqueue(PutDataAction(path, key, v))
  fun putData(path : String, key : String, v : ArrayList<Int>) = enqueue(PutDataAction(path, key, v))
}
