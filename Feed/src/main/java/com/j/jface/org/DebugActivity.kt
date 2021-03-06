package com.j.jface.org

import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Transaction
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.firebase.Firebase
import com.j.jface.lifecycle.ActivityWrapper
import com.j.jface.lifecycle.WrappedActivity
import com.j.jface.org.notif.NotifEngine
import java.util.concurrent.Executors

class DebugActivityBoot : ActivityWrapper<DebugActivity>()
class DebugActivity(args : WrappedActivity.Args) : WrappedActivity(args), EventListener<QuerySnapshot?>
{
  init
  {
    mA.setContentView(R.layout.debug_activity)
    mA.findViewById<Button>(R.id.try_notif).setOnClickListener {
      NotifEngine.scheduleClue(mA)
    }

    val applyOneshotButton = mA.findViewById<Button>(R.id.apply_oneshot_change)
    val user = FirebaseAuth.getInstance().currentUser
    if (Firebase.isLoggedIn && null != user)
      applyOneshotButton.setOnClickListener {
        val db = FirebaseFirestore.getInstance().collection(user.uid)
        val executor = Executors.newSingleThreadExecutor()
        db.document(Const.DB_APP_TOP_PATH).collection(Const.DB_ORG_TOP).orderBy("ord").addSnapshotListener(executor, this)
      }
    else
      applyOneshotButton.text = "Firebase not logged in"
  }

  override fun onEvent(snapshot : QuerySnapshot?, exception : FirebaseFirestoreException?)
  {
    if (null != exception) mA.findViewById<TextView>(R.id.debug_exception_text).text = exception.toString()
    if (null == snapshot) return
    FirebaseFirestore.getInstance().runTransaction { transaction ->
      applyOneshotChange(snapshot.documents, transaction)
    }.addOnCompleteListener {
      val success = if (it.isSuccessful) "success" else "failure"
      Toast.makeText(mA, "Update transaction : ${success}", Toast.LENGTH_LONG).show()
    }
  }

  private fun applyOneshotChange(docs : List<DocumentSnapshot>, transaction : Transaction)
  {
    val user = FirebaseAuth.getInstance().currentUser!!
    val collection = FirebaseFirestore.getInstance().collection(user.uid).document(Const.DB_APP_TOP_PATH).collection(Const.DB_ORG_TOP)
    docs.forEach {
      val id = it["id"] as String
      val map = transform(HashMap(it.data!!))
      transaction.set(collection.document(id), map)
    }
  }

  private fun transform(dataMap : HashMap<String, Any?>) : Map<String, Any?>
  {
    dataMap["estimatedMinutes"] = -1
    dataMap.remove("estimatedTime")
    return dataMap
  }
}
