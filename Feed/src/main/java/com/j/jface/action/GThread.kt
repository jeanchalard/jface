package com.j.jface.action

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.*
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.Wearable
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.j.jface.action.firebase.auth
import com.j.jface.action.wear.*
import com.j.jface.feed.views.SnackbarRegistry
import com.j.jface.org.todo.TodoCore
import java.util.concurrent.Executor
import java.util.concurrent.Executors

inline fun <T, U> Task<T>.addOnCompleteListener(e : Executor, crossinline l : (Task<T>) -> U) : Task<T> = addOnCompleteListener(e, OnCompleteListener<T> { l(it) })
inline fun <T, U> Task<T>.continueWith(e : Executor, crossinline l : (T) -> U) : Task<U> = continueWith(e, Continuation<T, U> { l(it.result) })
inline fun <T, U> Task<T>.continueWith(e : Executor, crossinline l : () -> U) : Task<U> = continueWith(e, Continuation<T, U> { l() })
inline fun <T, U> Task<T>.continueWithTask(e : Executor, crossinline l : (Task<T>) -> Task<U>) : Task<U> = continueWithTask(e, Continuation<T, Task<U>> { l(it) })


fun <T> (() -> T).enqueue(t : GThread) = t.enqueue(this)
fun Runnable.enqueue(t : GThread) = t.enqueue(this)

/**
 * A thread tuned for using Google APIs.
 *
 * Specifically Wear, Firebase, Drive. This thread is also used to throw
 * random asynchronous stuff that should be separate from UI, especially
 * when they require total ordering with some of the actions that relate
 * to Google APIs or that simply look similar.
 */
class GThread(val context : Context)
{
  companion object
  {
    private val ex = Executors.newSingleThreadExecutor()
  }
  private val dataClient = Wearable.getDataClient(context, Wearable.WearableOptions.Builder().setLooper(context.mainLooper).build())!!
  private var firebaseUser : FirebaseUser? = null
  private lateinit var rootCollection : CollectionReference

  class ETask<T>(val gThread : GThread, val task : Task<T>)
  {
    fun <R> then(next : () -> R) : ETask<R> = ETask(gThread, task.continueWith(ex, Continuation<T, R> { next() }))
    fun <R> then(next : (T) -> R) : ETask<R> = ETask(gThread, task.continueWith(ex, Continuation<T, R> { next(it.result) }))
    fun <R> then(next : (GThread, T) -> R) : ETask<R> = ETask(gThread, task.continueWith(ex, Continuation<T, R> { next(gThread, it.result) }))
    fun await() : T { return Tasks.await(task) }
  }

  fun enqueue(a : Runnable) : ETask<Unit>
  {
    val x = { a.run() }
    return enqueue(x)
  }
  fun <T> enqueue(a : Task<T>) : ETask<T>
  {
    a.addOnCompleteListener {}
    return ETask(this, a)
  }
  fun <T> enqueue(a : () -> T) : ETask<T>
  {
    val source = TaskCompletionSource<T>()
    ex.execute { val r = a.invoke(); source.setResult(r) }
    return ETask<T>(this, source.task)
  }
  private fun <T> enqueueU(a : (Unit) -> T) : ETask<T>
  {
    val source = TaskCompletionSource<T>()
    ex.execute { val r = a.invoke(Unit); source.setResult(r) }
    return ETask<T>(this, source.task)
  }
  fun <T> enqueueD(a : (DataClient) -> T) : ETask<T>
  {
    val source = TaskCompletionSource<T>()
    ex.execute { val r = a(dataClient); source.setResult(r) }
    return ETask<T>(this, source.task)
  }
  fun <T> enqueueF(a : (CollectionReference) -> T) : ETask<T>
  {
    if (null == firebaseUser)
    {
      val user = FirebaseAuth.getInstance().currentUser
      if (null == user)
        return enqueue(
         // Use the currently registered snackbar parent if it's there, otherwise the context passed to the GThread.
         // This sucks, but I don't think there is a good way of doing it.
         auth(SnackbarRegistry.getSnackbarParent()?.context ?: context).then { it : AuthResult? ->
           if (null == it)
             throw FirebaseAuthException("Can't log in", "J needs to fix his code to figure out why")
           else
           {
             if (null != it.user)
               Log.e("Logged in to firebase as", it.user.displayName)
             firebaseUser = it.user
             rootCollection = FirebaseFirestore.getInstance().collection(it.user.uid)
             a(rootCollection)
           }
         })
      else
      {
        firebaseUser = user
        rootCollection = FirebaseFirestore.getInstance().collection(user.uid)
      }
    }
    val source = TaskCompletionSource<T>()
    ex.execute { val r = a.invoke(rootCollection); source.setResult(r) }
    return ETask<T>(this, source.task)
  }
  fun <T, R> enqueue(a : ActionChain<Unit, T, R>) : ETask<R>
  {
    // A bit ugly, but all types have been checked at construction time of the ActionChain. They can't
    // be retrieved now, but it's fine, they have all been statically checked for sure.
    if (null == a.car) return enqueueU(a.cdr as (Unit) -> R)
    return enqueue(a.car).then(a.cdr)
  }

  // For calling from Java
  fun <T, R> executeInOrder(a : () -> T, b : (T) -> R) = enqueue(a).then(b)
  fun <T, R> executeInOrder(a : () -> T, b : () -> R) = enqueue(a).then(b)

  fun getDataSynchronously(path : String) : DataMap = Tasks.await(GetDataAction(dataClient, ex, path))
  fun getData(path : String, callback : (String, DataMap) -> Unit) = enqueueD(GetDataAction(ex, path, callback))
  fun getBitmap(path : String, key : String, callback : (String, String, Bitmap?) -> Unit) = enqueueD(GetBitmapAction(ex, path, key, callback))
  fun deleteData(path : String) = enqueueD(DeleteDataAction(path))
  fun deleteAllData() = enqueue(DeleteAllDataAction(dataClient))
  fun putData(path : String,               v : DataMap)        = enqueueD(PutDataAction(path, v))
  fun putData(path : String, key : String, v : String)         = enqueueD(PutDataAction(path, key, v))
  fun putData(path : String, key : String, v : Boolean)        = enqueueD(PutDataAction(path, key, v))
  fun putData(path : String, key : String, v : Long)           = enqueueD(PutDataAction(path, key, v))
  fun putData(path : String, key : String, v : Asset)          = enqueueD(PutDataAction(path, key, v))
  fun putData(path : String, key : String, v : ArrayList<Int>) = enqueueD(PutDataAction(path, key, v))

  fun updateTodo(t : TodoCore) = enqueueF(com.j.jface.action.firebase.updateTodo(t))
  fun todoList() = enqueueF(com.j.jface.action.firebase.todoList())
}
