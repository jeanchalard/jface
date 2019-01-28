package com.j.jface.org.notif

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.org.AutomaticEditorProcessor
import com.j.jface.org.editor.TodoEditor
import com.j.jface.org.todo.TodoCore
import java.time.Instant
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlin.collections.ArrayList

fun endOfMonth(date : Long) = Instant.ofEpochMilli(date).with(TemporalAdjusters.lastDayOfMonth()).epochSecond
fun endOfYear(date : Long) = Instant.ofEpochMilli(date).with(TemporalAdjusters.lastDayOfYear()).epochSecond

class FillinNotification(val context : Context) : NotificationBuilder
{
  data class Reply(val label : String, val transform : (TodoCore) -> TodoCore)
  enum class Field(val weight : Int, val description : String, val fieldId : Int, vararg val replies : Reply)
  {
    NOT_AN_ATTRIBUTE(0, "Hey hey a bug in your code ˙ ͜ʟ˙", -1 ),
    DEADLINE(80, "When is the deadline ?", R.id.todoDetails_estimatedTime,
     Reply("This month") { it.withDeadline(endOfMonth(it.deadline)) },
     Reply("This year") { it.withDeadline(endOfYear(it.deadline)) }),
    HARDNESS(15, "How hard is the deadline ?", R.id.todoDetails_hardness,
     Reply("Soft") { it.withHardness(TodoCore.DEADLINE_SOFT) },
     Reply("Semihard") { it.withHardness(TodoCore.DEADLINE_SEMIHARD) },
     Reply("Hard") { it.withHardness(TodoCore.DEADLINE_HARD) }),
    CONSTRAINT(10, "Where and when can this be done ?", R.id.todoDetails_constraint,
     Reply("Home") { it.withConstraint(TodoCore.ON_HOME) },
     Reply("Work") { it.withConstraint(TodoCore.ON_WORK) }),
    ESTIMATED_TIME(30, "How long will this take ?", R.id.todoDetails_estimatedTime,
     Reply("5'") { it.withEstimatedMinutes(5) },
     Reply("15'") { it.withEstimatedMinutes(15) },
     Reply("1h") { it.withEstimatedMinutes(60) });
  }

  private fun buildFillinNotificationActions(existingIntent : Intent, attr : Field) : List<Notification.Action>
  {
    return attr.replies.mapIndexed { i, reply ->
      val intent = Intent(existingIntent).setClass(context, AutomaticEditorProcessor.Receiver::class.java).putExtra(Const.EXTRA_FILLIN_REPLY_INDEX, i)
      val pendingIntent = PendingIntent.getBroadcast(context, Const.NOTIFICATION_RESULT_CODE, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT)
      Notification.Action.Builder(null, reply.label, pendingIntent).build()
    }
  }

  private fun getMissingAttributes(todo : TodoCore) : ArrayList<Field>
  {
    val r = ArrayList<Field>()
    if (0L == todo.deadline) r.add(Field.DEADLINE)
    else if (TodoCore.UNKNOWN == todo.hardness) r.add(Field.HARDNESS)
    if (TodoCore.UNKNOWN == todo.constraint) r.add(Field.CONSTRAINT)
    if (0 == todo.estimatedMinutes) r.add(Field.ESTIMATED_TIME)
    return r
  }

  private fun chooseAttribute(missingAttributes : ArrayList<Field>) : Field
  {
    var lot = Random().nextInt(missingAttributes.sumBy { it.weight })
    missingAttributes.forEach {
      lot -= it.weight
      if (lot <= 0) return it
    }
    return Field.NOT_AN_ATTRIBUTE
  }

  override fun buildNotification(id : Int, todo : TodoCore) : Notification
  {
    val missingAttributes = getMissingAttributes(todo)
    val attr = chooseAttribute(missingAttributes)
    val title = "Tell me more about : " + todo.text
    val description = attr.description
    val intent = Intent(context, TodoEditor.activityClass())
    intent.putExtra(Const.EXTRA_TODO_ID, todo.id)
     .putExtra(Const.EXTRA_NOTIF_ID, id)
     .putExtra(Const.EXTRA_NOTIF_TYPE, NotifEngine.NotifType.FILLIN.ordinal)
     .putExtra(Const.EXTRA_FILLIN_FIELD, attr.ordinal)
     .putExtra(Const.EXTRA_FILLIN_REPLY_INDEX, -1)
     .putExtra(Const.EXTRA_NOTIF_TYPE, Const.NOTIFICATION_TYPE_FILLIN)
    val pendingIntent = PendingIntent.getActivity(context, Const.NOTIFICATION_RESULT_CODE, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT)
    return Notification.Builder(context, NotifEngine.getChannel(context).id).apply {
      setShowWhen(true)
      setWhen(System.currentTimeMillis())
      setSmallIcon(R.drawable.jormungand)
      setColor(context.getColor(R.color.jormungand_color))
      setContentIntent(pendingIntent)
      setContentTitle(title)
      setContentText(description)
      setStyle(Notification.BigTextStyle().bigText(description))
      setAutoCancel(true)
      setOnlyAlertOnce(true)
      setCategory(Notification.CATEGORY_REMINDER)
      // setVisibility(Notification.VISIBILITY_SECRET) // Broken in the latest custom build apparently
      buildFillinNotificationActions(intent, attr).forEach { addAction(it) }
    }.build()
  }
}
