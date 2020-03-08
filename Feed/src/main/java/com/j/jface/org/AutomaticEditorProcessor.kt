package com.j.jface.org

import android.annotation.SuppressLint
import android.app.PendingIntent
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
import com.j.jface.*
import com.j.jface.firebase.Firebase
import com.j.jface.lifecycle.CommonObjects
import com.j.jface.org.notif.NotifEngine
import com.j.jface.org.notif.SplitNotification
import com.j.jface.org.notif.SuggestionNotification
import com.j.jface.org.notif.errorNotification
import com.j.jface.org.todo.Todo
import com.j.jface.org.todo.TodoCore
import com.j.jface.org.todo.TodoListReadonlyFullView
import java.util.Collections

@SuppressLint("NewApi")
class AutomaticEditorProcessor : JobService()
{
  companion object
  {
    fun reschedulePendingIntent(context : Context, intent : Intent) : PendingIntent {
      val deleteIntent = Intent(intent).apply {
        setClass(context, AutomaticEditorProcessor.Receiver::class.java)
        putExtra(Const.EXTRA_WAS_DISMISSED, true)
      }
      return PendingIntent.getBroadcast(context, Const.NOTIFICATION_RESULT_CODE, deleteIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT)
    }
  }

  class Receiver : BroadcastReceiver()
  {
    override fun onReceive(context : Context?, intent : Intent?)
    {
      if (null == context || null == intent) return
      NotifEngine.scheduleClue(context)
      if (intent.getBooleanExtra(Const.EXTRA_WAS_DISMISSED, false)) return

      val todoId = intent.getStringExtra(Const.EXTRA_TODO_ID) ?: return
      val notifId = intent.getIntExtra(Const.EXTRA_NOTIF_ID, 0)
      if (0 == notifId) return

      val notifType = intent.getIntExtra(Const.EXTRA_NOTIF_TYPE, 0)

      val job = JobInfo.Builder(notifId, ComponentName(context, AutomaticEditorProcessor::class.java))
       .setExtras(PersistableBundle().apply {
         putString(Const.EXTRA_TODO_ID, todoId)
         putInt(Const.EXTRA_NOTIF_ID, notifId)
         putInt(Const.EXTRA_NOTIF_TYPE, notifType)
         if (Const.NOTIFICATION_TYPE_SPLIT == notifType)
         {
           val subitems = RemoteInput.getResultsFromIntent(intent)?.getString(Const.EXTRA_TODO_SUBITEMS) ?: return
           putString(Const.EXTRA_TODO_SUBITEMS, subitems)
         }
       })
       .setOverrideDeadline(5000)
       .setBackoffCriteria(5000, JobInfo.BACKOFF_POLICY_EXPONENTIAL) // retry after 5s on first failure, then exponential backoff
       .setPersisted(true)
       .build()
      Log.e("RECEIVED NOTIF", "todoId ${todoId} ; notifId ${notifId} ; notifType ${notifType} → ${intent.extras}")
      Log.e("SEND " + if (Const.NOTIFICATION_TYPE_SPLIT == notifType) "SPLIT" else "SUGGESTION", Integer.toString(context.jobScheduler.schedule(job)))
    }
  }

  private fun logErrorAndFalse(msg : String) : Boolean
  {
    Log.e("JOrg", msg)
    return false
  }

  override fun onStartJob(params : JobParameters?) : Boolean
  {
    if (!Firebase.isLoggedIn) { Log.e("JOrg", "Click on notif but Firebase is not logged in >.>"); return false }
    val todoId = params?.extras?.getString(Const.EXTRA_TODO_ID)
    val notifId = params?.extras?.getInt(Const.EXTRA_NOTIF_ID)
    val notifType = params?.extras?.getInt(Const.EXTRA_NOTIF_TYPE)
    if (null == todoId || null == notifId || 0 == notifId || null == notifType) return logErrorAndFalse("Click on notif but some param is null")
    val subitems = when (notifType)
    {
      Const.NOTIFICATION_TYPE_SPLIT -> params.extras.getString(Const.EXTRA_TODO_SUBITEMS)?.split(",") ?: return logErrorAndFalse("Split todo but subitems are null")
      Const.NOTIFICATION_TYPE_SUGGESTION -> Collections.emptyList()
      else -> return logErrorAndFalse("Click on notif with wrong type ${notifType}")
    }
    CommonObjects.executor.execute {
      val tl = TodoListReadonlyFullView(this)
      val parent : Todo? = tl.findById(todoId)
      Log.e("INJOB", "todoId ${todoId} ; notifId ${notifId} ; notifType ${notifType} : ${parent}")
      if (null == parent) return@execute notifManager.notify(notifId, errorNotification("Somehow can't find todo with ID ${todoId}", this))
      val notif = when (notifType) {
        Const.NOTIFICATION_TYPE_SPLIT ->
        {
          val children = ArrayList<TodoCore>()
          for (subitem in subitems) children.add(tl.createAndInsertTodo(subitem.trim(), parent))
          SplitNotification(this).buildAckNotification(parent, children)
        }
        Const.NOTIFICATION_TYPE_SUGGESTION ->
        {
          tl.markTodoCompleteAndReturnOldTree(parent)
          SuggestionNotification(this).buildAckOrCancelNotification(notifId, parent)
        }
        else -> return@execute
      }
      Log.e("AUTOP NOTIF", "${notifId} ${notif}")
      notifManager.notify(notifId, notif)
      jobFinished(params, false)
    }
    return true
  }

  override fun onStopJob(params : JobParameters?) : Boolean = false
}
