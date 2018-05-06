package com.j.jface.firebase

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataMap
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.*
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.org.AuthTrampoline
import com.j.jface.org.todo.TodoCore
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

fun <T> Task<T>.await() : T = Tasks.await(this) // Guaranteed to return non-null
fun DataMap.toMap() : Map<String, Object>
{
  val m = HashMap<String, Object>()
  this.keySet().forEach { m[it] = this[it] }
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

  fun isLoggedIn() : Boolean = ::firebaseUser.isInitialized

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
    db.document(Const.DB_APP_TOP_PATH).collection(Const.DB_ORG_TOP).orderBy("ord").addSnapshotListener(executor, TodoUpdateListener)
    db.document(Const.DB_APP_TOP_PATH).collection(Const.DB_WEAR_TOP).addSnapshotListener(executor, TodoUpdateListener)
  }

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

  fun updateTodo(todo : TodoCore) = db.document(Const.DB_APP_TOP_PATH).collection(Const.DB_ORG_TOP).document(todo.id).set(todo)
  fun updateWearData(path : String, d : DataMap) = db.document(Const.DB_APP_TOP_PATH).collection(Const.DB_WEAR_TOP).document(path).set(d.toMap())

  object WearUpdateListener : EventListener<QuerySnapshot>
  {
    interface Listener
    {
      fun onDataUpdated()
    }
    private val listeners = ArrayList<Listener>()
    fun addListener(l : Listener) = listeners.add(l)
    fun removeListener(l : Listener) = listeners.remove(l)
    override fun onEvent(snapshot : QuerySnapshot?, exception : FirebaseFirestoreException?)
    {
      if (null == snapshot) return // TODO : handle failures
      // Note that these events all happen on the same single-thread executor, so they are serialized.
    }
  }

  object TodoUpdateListener : EventListener<QuerySnapshot>
  {
    interface Listener
    {
      fun onTodoUpdated(type : DocumentChange.Type, todo : TodoCore)
    }
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
            if (index > 0)
              tl[index] = updatedTodo
            else // It's an insertion of a new todo.
              tl.add(-index - 1, updatedTodo)
          }
        }
        todoList = tl
      }
    }
  }
}

fun DocumentSnapshot.toTodoCore() = TodoCore(this.getString("id"),
   this.getString("ord"),
   this.getLong("creationTime"),
   this.getLong("completionTime"),
   this.get("text") as String,
   this.getLong("depth").toInt(),
   this.getLong("lifeline"),
   this.getLong("deadline"),
   this.getLong("hardness").toInt(),
   this.getLong("constraint").toInt(),
   this.getLong("estimatedTime").toInt())
