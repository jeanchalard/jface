package com.j.jface.firebase

import android.util.ArrayMap
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataMap
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Transaction
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.Util
import com.j.jface.lifecycle.AuthTrampoline
import com.j.jface.org.todo.TodoCore
import com.j.jface.org.todo.TodoProviderContract
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

fun <T> Task<T>.await() : T = Tasks.await(this) // Guaranteed to return non-null
private fun firestoreStoreableObject(it : Any) : Any = when (it)
{
  is Asset -> "image" // TODO : send the image data, like base64 encoded or something
  else -> it
}
private fun DataMap.toMap() : Map<String, Any>
{
  val m = HashMap<String, Any>()
  this.keySet().forEach { m[it] = firestoreStoreableObject(this[it]) }
  return m
}

class JOrgAuthException(s : String) : RuntimeException(s)

object Firebase
{
  private val executor = Executors.newSingleThreadExecutor()
  private lateinit var firebaseUser : FirebaseUser
  private lateinit var db : CollectionReference

  init
  {
    val user = FirebaseAuth.getInstance().currentUser
    if (null != user) start(user)
  }

  /**************
   * Login
   **************/
  val isLoggedIn : Boolean
    get() = ::firebaseUser.isInitialized

  fun signIn(host : AuthTrampoline)
  {
    executor.execute { signInSynchronously(host) }
  }

  private fun signInSynchronously(host : AuthTrampoline)
  {
    val client : GoogleSignInClient = GoogleSignIn.getClient(host.context, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
     .requestIdToken(host.context.getString(R.string.firebase_client_id))
     .build())
    try
    {
      val googleAccount = client.silentSignIn().await() ?: throw JOrgAuthException("Can't log in to Google")
      val firebaseAccount : AuthResult = FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(googleAccount.idToken, null)).await()
      val user = firebaseAccount.user ?: throw JOrgAuthException("Logged in to Google, but can't log in to Firebase")
      start(user)
      host.onSignInSuccess()
    }
    catch (e : ExecutionException)
    {
      host.context.runOnUiThread { host.context.startActivityForResult(client.signInIntent, Const.GOOGLE_SIGN_IN_RESULT_CODE) }
    }
    catch (e : JOrgAuthException)
    {
      host.onSignInFailure(e.toString())
    }
  }

  private fun start(user : FirebaseUser)
  {
    firebaseUser = user
    db = FirebaseFirestore.getInstance().collection(user.uid)
    db.document(Const.DB_APP_TOP_PATH).collection(Const.DB_ORG_TOP)
     .whereEqualTo(TodoProviderContract.COLUMN_completionTime, 0)
     .orderBy("ord")
     .addSnapshotListener(executor, TodoUpdateListener)
  }

  /**************
   * Util
   **************/
  // Very raw and unencapsulated and kind of unsafe but for the time being I don't care
  fun <R> transaction(what : (CollectionReference, Transaction) -> R) = FirebaseFirestore.getInstance().runTransaction { t -> what(db, t) }

  /**************
   * TODOs
   **************/
  private val condVar = Object()
  @Volatile @JvmField var todoList : MutableList<TodoCore>? = null

  fun getTodoList() : List<TodoCore>
  {
    synchronized(condVar) {
      var t = todoList
      while (null == t)
      {
        try { condVar.wait() } catch (e : InterruptedException) {}
        t = todoList
      }
      return t
    }
  }

  // Use to update a single field of a known todo ID without reading the entire database.
  // Use is generally discouraged, but it's useful to avoid getting the entire database
  // (please don't do this if you have a reference to it, just call updateTodo)
  // and to update a Todo that is not normally found, like a Todo that's already complete
  // (that's useful to undo completion typically)
  fun outOfBandUpdateTodoField(id : String, field : String, value : Any) : Task<Void>
  {
    if (!isLoggedIn) throw RuntimeException("You must be logged in to do this")
    val vals = ArrayMap<String, Any>()
    vals[field] = value
    return FirebaseFirestore.getInstance().collection(firebaseUser.uid).document(Const.DB_APP_TOP_PATH).collection(Const.DB_ORG_TOP)
     .document(id).set(vals, SetOptions.merge())
  }

  fun updateTodo(todo : TodoCore)
  {
    Log.e("UPDATED TODO ${todo.text}", Util.getStackTrace(30))
    db.document(Const.DB_APP_TOP_PATH).collection(Const.DB_ORG_TOP).document(todo.id).set(todo)
  }
  object TodoUpdateListener : EventListener<QuerySnapshot?>
  {
    interface Listener { fun onTodoUpdated(type : DocumentChange.Type, todo : TodoCore) }
    private val listeners = ArrayList<Listener>()
    fun addListener(l : Listener) = listeners.add(l)
    fun removeListener(l : Listener) = listeners.remove(l)
    override fun onEvent(snapshot : QuerySnapshot?, exception : FirebaseFirestoreException?)
    {
      if (null == snapshot) return // TODO : handle failures
      // Note that these events all happen on the same single-thread executor, so they are serialized.
      // This is absolutely essential for synchronization when getting the first instance of the todo list.
      val tl = todoList
      if (null == tl)
      {
        val l = ArrayList<TodoCore>()
        todoList = snapshot.documents.mapTo(l, DocumentSnapshot::toTodoCore)
        synchronized(condVar) { condVar.notifyAll() }
      }
      else
      {
        if (snapshot.metadata.hasPendingWrites()) return // This was a local update.
        synchronized(condVar)
        {
          for (doc in snapshot.documentChanges)
          {
            val updatedTodo = doc.document.toTodoCore()
            for (l in listeners)
              l.onTodoUpdated(doc.type, updatedTodo)
            val index = tl.binarySearch(updatedTodo, Comparator.comparing(TodoCore::ord))
            if (index >= 0)
            {
              if (doc.type == DocumentChange.Type.REMOVED)
                tl.removeAt(index) // Removal of an existing Todo
              else
              {
                assert(doc.type == DocumentChange.Type.MODIFIED)
                tl[index] = updatedTodo
              }
            }
            else // It's an insertion of a new todo.
            {
              assert(doc.type == DocumentChange.Type.ADDED)
              tl.add(-index - 1, updatedTodo)
            }
          }
        }
        todoList = tl
      }
    }

  }
  /**************
   * Wear
   **************/
  enum class PathType { DOCUMENT, COLLECTION }

  private fun wearPathToFirebasePath(wearPath : String, pathType : PathType = PathType.DOCUMENT) : String
  {
    val wearComponents = wearPath.split('/')
    val firebaseComponents = ArrayList<String>()
    if (PathType.DOCUMENT == pathType) firebaseComponents.add(Const.DB_APP_TOP_PATH)
    var collectionComponent = false
    wearComponents.forEach {
      if (it.isEmpty()) return@forEach
      firebaseComponents.add(it)
      if (collectionComponent) firebaseComponents.add(it)
      collectionComponent = !collectionComponent
    }
    return firebaseComponents.joinToString("/")
  }
  private fun firebasePathToWearPath(firebasePath : String) : String
  {
    val firebaseComponents = firebasePath.split('/')
    if (firebaseUser.uid != firebaseComponents[0] || Const.DB_APP_TOP_PATH != firebaseComponents[1])
      throw RuntimeException("Not a valid firebase path representing a wear path")
    val wearComponents = ArrayList<String>()
    wearComponents.add("") // Simplest way to add a / in front
    for (i in 2..firebaseComponents.size - 1)
    {
      val it = firebaseComponents[i]
      if (0 != (i + 2) % 3)
        wearComponents.add(it)
      else
      // Duplicated component
        if (it != wearComponents.last()) throw RuntimeException("Not a valid firebase path representing a wear path")
    }
    return wearComponents.joinToString("/")
  }

  fun updateWearData(path : String, d : DataMap) = FirebaseFirestore.getInstance().batch().set(db.document(wearPathToFirebasePath(path)), d.toMap()).commit()
  fun getWearData(path : String) : Task<DataMap>
  {
    val p = wearPathToFirebasePath(path)
    return db.document(p).get().continueWith { it: Task<DocumentSnapshot> -> it.result.toDataMap() }
  }
  fun getAccessKeyAndWearListenersSynchronously() : Pair<String, List<String>>
  {
    val config = db.document(Const.DB_APP_TOP_PATH).collection(wearPathToFirebasePath(Const.CONFIG_PATH, PathType.COLLECTION)).get()
    Tasks.await(config)
    var key = ""
    val listeners = ArrayList<String>()
    config.result.documents.forEach {
      if (null != it[Const.CONFIG_KEY_SERVER_KEY])
        key = it.getString(Const.CONFIG_KEY_SERVER_KEY)
      else
        listeners.add(it.getString(Const.CONFIG_KEY_WEAR_LISTENER_ID))
    }
    return Pair(key, listeners)
  }

  abstract class WearDataUpdateListener : EventListener<QuerySnapshot>
  {
    private val registrationTokens : ArrayList<ListenerRegistration> = ArrayList()
    fun resume()
    {
      registrationTokens.add(db.document(Const.DB_APP_TOP_PATH).collection(wearPathToFirebasePath(Const.CONFIG_PATH, PathType.COLLECTION)).addSnapshotListener(executor, this))
      registrationTokens.add(db.document(Const.DB_APP_TOP_PATH).collection(wearPathToFirebasePath(Const.DATA_PATH, PathType.COLLECTION)).addSnapshotListener(executor, this))
    }
    fun pause()
    {
      while (!registrationTokens.isEmpty())
        registrationTokens.removeAt(0).remove()
    }
    override fun onEvent(snapshot : QuerySnapshot?, exception : FirebaseFirestoreException?)
    {
      if (null == snapshot) return // TODO : handle failures
      if (snapshot.metadata.hasPendingWrites()) return // This was a local update.
      snapshot.documents.forEach { doc ->
        val wearPath = firebasePathToWearPath(doc.reference.path)
        val data = doc.toDataMap()
        onWearDataUpdated(wearPath, data)
      }
    }
    abstract fun onWearDataUpdated(path : String, data : DataMap)
  }
}

fun DocumentSnapshot.toTodoCore() : TodoCore
{
  try {
    return directToTodoCore()
  } catch (e : Exception) {
    Log.e("Offending todo : ", "" + this.getString(TodoProviderContract.COLUMN_id))
    throw e
  }
}
fun DocumentSnapshot.directToTodoCore() = TodoCore(
   this.getString(TodoProviderContract.COLUMN_id),
   this.getString(TodoProviderContract.COLUMN_ord),
   this.getLong  (TodoProviderContract.COLUMN_creationTime),
   this.getLong  (TodoProviderContract.COLUMN_completionTime),
   this.get      (TodoProviderContract.COLUMN_text) as String,
   this.getLong  (TodoProviderContract.COLUMN_depth).toInt(),
   this.getLong  (TodoProviderContract.COLUMN_lifeline),
   this.getLong  (TodoProviderContract.COLUMN_deadline),
   this.getLong  (TodoProviderContract.COLUMN_hardness).toInt(),
   this.getLong  (TodoProviderContract.COLUMN_constraint).toInt(),
   this.getLong  (TodoProviderContract.COLUMN_estimatedMinutes).toInt(),
   this.getLong  (TodoProviderContract.COLUMN_updateTime).toLong())
fun DocumentSnapshot.toTodoCoreDebug() : TodoCore {
  val id = this.getString(TodoProviderContract.COLUMN_id)
  if (null == this.getString(TodoProviderContract.COLUMN_id))               Log.e("Todo ${id}", "id is null")
  if (null == this.getString(TodoProviderContract.COLUMN_ord))              Log.e("Todo ${id}", "ord is null")
  if (null == this.getLong  (TodoProviderContract.COLUMN_creationTime))     Log.e("Todo ${id}", "creationTime is null")
  if (null == this.getLong  (TodoProviderContract.COLUMN_completionTime))   Log.e("Todo ${id}", "completionTime is null")
  if (null == this.get      (TodoProviderContract.COLUMN_text))             Log.e("Todo ${id}", "text is null")
  if (null == this.getLong  (TodoProviderContract.COLUMN_depth))            Log.e("Todo ${id}", "depth is null")
  if (null == this.getLong  (TodoProviderContract.COLUMN_lifeline))         Log.e("Todo ${id}", "lifeline is null")
  if (null == this.getLong  (TodoProviderContract.COLUMN_deadline))         Log.e("Todo ${id}", "deadline is null")
  if (null == this.getLong  (TodoProviderContract.COLUMN_hardness))         Log.e("Todo ${id}", "hardness is null")
  if (null == this.getLong  (TodoProviderContract.COLUMN_constraint))       Log.e("Todo ${id}", "constraint is null")
  if (null == this.getLong  (TodoProviderContract.COLUMN_estimatedMinutes)) Log.e("Todo ${id}", "estimatedTime is null")
  if (null == this.getLong  (TodoProviderContract.COLUMN_updateTime))       Log.e("Todo ${id}", "updateTime is null")
  return TodoCore(
   this.getString(TodoProviderContract.COLUMN_id) ?: "",
   this.getString(TodoProviderContract.COLUMN_ord) ?: "",
   this.getLong  (TodoProviderContract.COLUMN_creationTime) ?: -1,
   this.getLong  (TodoProviderContract.COLUMN_completionTime) ?: -1,
   this.get      (TodoProviderContract.COLUMN_text) as String? ?: "",
   this.getLong  (TodoProviderContract.COLUMN_depth)?.toInt() ?: -1,
   this.getLong  (TodoProviderContract.COLUMN_lifeline) ?: -1,
   this.getLong  (TodoProviderContract.COLUMN_deadline) ?: -1,
   this.getLong  (TodoProviderContract.COLUMN_hardness)?.toInt() ?: -1,
   this.getLong  (TodoProviderContract.COLUMN_constraint)?.toInt() ?: -1,
   this.getLong  (TodoProviderContract.COLUMN_estimatedMinutes)?.toInt() ?: -1,
   this.getLong  (TodoProviderContract.COLUMN_updateTime)?.toLong() ?: -1)
}

fun DocumentSnapshot.toDataMap() = DataMap().apply {
  this@toDataMap.data.forEach {
    key, value -> when(value) {
      is String -> this.putString(key, value)
      is Boolean -> this.putBoolean(key, value)
      is Long -> this.putLong(key, value)
      // Note our own Wear API only tolerates arrays of ints, and FireStore converts that to Longs
      is ArrayList<*> -> this.putIntegerArrayList(key, value.map { (it as Long).toInt() }.toCollection(ArrayList<Int>()))
      is Array<*> -> this.putIntegerArrayList(key, value.map { (it as Long).toInt() }.toCollection(ArrayList<Int>()))
      else -> throw RuntimeException("Unknown type in document for wear path : " + value.javaClass)
      // TODO : assets ; for the time being they are represented by a string "image". See at the top of this file.
    }
  }
}
