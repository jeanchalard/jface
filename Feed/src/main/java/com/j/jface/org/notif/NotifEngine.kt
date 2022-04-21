package com.j.jface.org.notif

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import com.j.jface.*
import com.j.jface.action.NotificationAction
import com.j.jface.org.todo.TodoCore
import com.j.jface.org.todo.TodoListReadonlyFullView
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

private const val SCHEDULE_CLUE_JOB_ID = Integer.MAX_VALUE - 1 // Must be globally unique... :/
private const val MIN_CLUE_INTERVAL = 4 * 3600_000 // 4h
private const val MAX_CLUE_INTERVAL = 48 * 3600_000

@SuppressLint("NewApi")
class NotifEngine : JobService()
{
  enum class NotifType(override val weight : Int) : WeightedChoice
  {
    SPLIT(150), // Split this TODO into multiple TODOs.
    FILLIN(300), // Please add missing info : deadline + hardness, estimated time, constraint
    SUGGESTION(100), // How about you do this now ? Constraints are fulfilled and it's not too hard
    REMINDER(0), // This deadline is very soon, make sure you don't forget
  }

  companion object
  {
    private const val CHANNEL_ID = "jorg_todo"
    private const val CHANNEL_NAME = "Jormungand : Todo"
    const val LAST_NOTIF_ID = "todo_lastId"

    internal fun getChannel(context : Context) : NotificationChannel
    {
      val notifManager = context.notifManager
      val existing = notifManager.getNotificationChannel(CHANNEL_ID)
      if (null != existing) return existing
      val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
      // Configure the notification channel.
      channel.description = "Jormungand : todo notifications"
      channel.enableLights(true)
      channel.lightColor = context.getColor(R.color.jormungand_color)
      channel.vibrationPattern = longArrayOf(0L, 30L, 30L, 60L) // First number is delay to vibrator on
      notifManager.createNotificationChannel(channel)
      return notifManager.getNotificationChannel(CHANNEL_ID)
    }

    /**
     * Clues (automatic split/fillin notifications) are managed as follow.
     * Each time the JOrg activity is started or a notification is either dismissed or used,
     * scheduleClue is called. It starts with checking the existence of the job to put up a
     * notification, and exits immediately if it already exists.
     * The job is scheduled at a random time between [4h,48h] to which ±2h of leeway is given
     * to JobScheduler. When the job starts executing, it randomly selects one work item to do
     * from the list of items offered by both styles of notification, and delegates putting up
     * the notification to the relevant NotificationBuilder.
     *
     * The notification stays up until it is dismissed or acted upon, at which point scheduleClue
     * is called again.
     */
    fun scheduleClue(context : Context)
    {
      val jobScheduler = context.jobScheduler
      if (jobScheduler.getPendingJob(SCHEDULE_CLUE_JOB_ID) != null) return
      val interval = (MIN_CLUE_INTERVAL + Random().nextInt(MAX_CLUE_INTERVAL - MIN_CLUE_INTERVAL)).toLong()
      val job = JobInfo.Builder(SCHEDULE_CLUE_JOB_ID, ComponentName(context, NotifEngine::class.java)).apply {
        setMinimumLatency(interval - 2 * 3600_000)
        setOverrideDeadline(interval + 2 * 3600_000)
        setPersisted(true)
      }.build()
      NotificationAction.postNotification(context, "Clue scheduled in ${interval}s (${Instant.now().plusMillis(interval).atOffset(ZoneOffset.ofHours(9))})", details = null, pendingIntent = null)
      jobScheduler.schedule(job)
    }
  }

  private fun notify(builder : NotificationBuilder, todo : TodoCore)
  {
    val id = this.nextNotifId(LAST_NOTIF_ID)
    val notification = builder.buildNotification(id, todo)
    notifManager.notify(id, notification)
  }

  override fun onStartJob(params : JobParameters?) : Boolean // returns whether work is still happening in another thread
  {
    val list = TodoListReadonlyFullView(this)
    val fillinNotification = FillinNotification(this)
    val splitNotification = SplitNotification(this)
    val fnn = fillinNotification.remainingItems(list)
    val snn = splitNotification.remainingItems(list)
    if (fnn.isEmpty() && snn.isEmpty()) return false

    if (Random().nextInt(fnn.size + snn.size) < fnn.size)
      notify(fillinNotification, fnn.randomItem())
    else
      notify(splitNotification, snn.randomItem())
    return false
  }

  override fun onStopJob(params : JobParameters?) = false
}
