package com.j.jface.action.firebase

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.j.jface.org.todo.TodoCore

fun updateTodo(todo : TodoCore) = { db : CollectionReference ->
  db.document("JOrg").collection("todo").document(todo.id).set(todo)
}

fun todoList() : (CollectionReference) -> List<TodoCore> = { db : CollectionReference ->
    val snapshot = Tasks.await(db.document("JOrg").collection("todo").whereEqualTo("completionTime", 0).orderBy("ord").get())
    snapshot.documents.map {
      TodoCore(it.getString("id"),
       it.getString("ord"),
       it.getLong("creationTime"),
       it.getLong("completionTime"),
       it.get("text") as String,
       it.getLong("depth").toInt(),
       it.getLong("lifeline"),
       it.getLong("deadline"),
       it.getLong("hardness").toInt(),
       it.getLong("constraint").toInt(),
       it.getLong("estimatedTime").toInt())
    }
}
