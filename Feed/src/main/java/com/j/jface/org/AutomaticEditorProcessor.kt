package com.j.jface.org

import android.app.RemoteInput
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import android.util.Log
import com.j.jface.Const
import com.j.jface.firebase.Firebase
import com.j.jface.lifecycle.CommonObjects
import com.j.jface.notifManager
import com.j.jface.org.notif.SplitNotification
import com.j.jface.org.notif.errorNotification
import com.j.jface.org.todo.Todo
import com.j.jface.org.todo.TodoCore
import com.j.jface.org.todo.TodoListReadonlyFullView

class AutomaticEditorProcessor : JobService()
{
  class Receiver : BroadcastReceiver()
  {
    override fun onReceive(context : Context?, intent : Intent?)
    {
      if (null == context || null == intent) return
      val todoId = intent.getStringExtra(Const.EXTRA_TODO_ID) ?: return
      val notifId = intent.getIntExtra(Const.EXTRA_NOTIF_ID, 0)
      if (0 == notifId) return
      val subitems = RemoteInput.getResultsFromIntent(intent)?.getString(Const.EXTRA_TODO_SUBITEMS) ?: return

      val job = JobInfo.Builder(todoId.hashCode(), ComponentName(context, AutomaticEditorProcessor::class.java))
       .setExtras(PersistableBundle().apply {
         putString(Const.EXTRA_TODO_ID, todoId)
         putInt(Const.EXTRA_NOTIF_ID, notifId)
         putString(Const.EXTRA_TODO_SUBITEMS, subitems)
       })
       .setOverrideDeadline(5000)
       .setBackoffCriteria(5000, JobInfo.BACKOFF_POLICY_EXPONENTIAL) // retry after 5s on first failure, then exponential backoff
       .setPersisted(true)
       .build()
      val jobScheduler = context.getSystemService(JobScheduler::class.java)
      Log.e("SEND SPLIT", Integer.toString(jobScheduler.schedule(job)))
    }
  }

  override fun onStartJob(params : JobParameters?) : Boolean
  {
    val todoId = params?.extras?.getString(Const.EXTRA_TODO_ID)
    val notifId = params?.extras?.getInt(Const.EXTRA_NOTIF_ID)
    val subitems = params?.extras?.getString(Const.EXTRA_TODO_SUBITEMS)?.split(",")
    if (null == todoId || null == notifId || 0 == notifId || null == subitems) { Log.e("JOrg", "Split todo but some param is null"); return false }
    if (!Firebase.isLoggedIn()) { Log.e("JOrg", "Split todo, but Firebase is not logged in >.>"); return false }
    CommonObjects.executor.execute {
      val tl = TodoListReadonlyFullView(this)
      val parent : Todo? = tl.findById(todoId)
      if (null == parent) return@execute notifManager.notify(notifId, errorNotification("Somehow can't find todo with ID ${todoId}", this))
      val children = ArrayList<TodoCore>()
      for (subitem in subitems)
        children.add(tl.createAndInsertTodo(subitem.trim(), parent))
      val notif = SplitNotification(this).buildAckNotification(parent, children)
      notifManager.notify(notifId, notif)
      jobFinished(params, false)
    }
    return true
  }

  override fun onStopJob(params : JobParameters?) : Boolean = false
}
