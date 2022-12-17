package com.j.jface.feed

import android.content.Context
import android.util.Log
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

const val LOG_EXPIRY = 1000L * 60 * 60 * 1440 * 30 // 30 days

@Database(entities = [LogEntry::class], version = 1)
abstract class LogDb : RoomDatabase() {
  abstract fun dao() : LogDao
}

@Entity(tableName = "log")
data class LogEntry(@PrimaryKey(autoGenerate = true) val id : Int = 0, val date : Long, val msg : String)

@Dao
interface LogDao {
  @Query("SELECT * FROM log") fun getAll() : List<LogEntry>
  @Insert fun insert(entry : LogEntry)
  fun insert(msg : String) = insert(LogEntry(date = System.currentTimeMillis(), msg = msg))
  @Query("DELETE FROM log") fun clear()
  @Query("DELETE FROM log WHERE date < :cutoff") fun maintain(cutoff : Long = System.currentTimeMillis() - LOG_EXPIRY)
}

object LocalLog {
  private var db : LogDao? = null
  fun getDb(context : Context) : LogDao {
    synchronized(this) {
      val q = db
      if (null != q) return q
      return Room.databaseBuilder(context, LogDb::class.java, "logDb").build().dao().also { db = it }
    }
  }

  fun log(context : Context, msg : String) {
    GlobalScope.launch(Dispatchers.IO) { getDb(context).insert(msg) }
    Log.e("jface", msg)
  }
}
