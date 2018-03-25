package com.j.jface.action.firebase

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.j.jface.org.todo.Todo

class TodoSourceTest
{
  val mDb = FirebaseFirestore.getInstance()
  fun writeLastUse() : Task<Void>
  {
    val todo = Todo("test todo", "123")
    return mDb.collection("metadata").document("lastUse").set(todo).addOnCompleteListener {
      Log.e("RETURN", "" + it.isComplete + " : " + it.isSuccessful)
      Log.e("→", "" + if (it.isSuccessful) it.result else it.exception)
    }
  }
}
