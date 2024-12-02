package com.j.jface.feed

import com.google.android.gms.wearable.DataMap
import com.j.jface.Const
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.text.ParseException
import java.util.ArrayList

fun ekitanOptions(tab : Int = 1, typeParser : EkitanParser.TypeParser = EkitanParser.TypeParser.Standard) = EkitanParser.Options(tab, typeParser)

class EkitanParser : FeedParser() {
  sealed class TypeParser {
    // Parse should return null if the departure is to be ignored (this train is not relevant), otherwise it should return a string (preferably 0 or 1 chars for
    // screen-space reasons) to adorn the departure with, typically 快 for a 快速 train.
    abstract fun parse(type : String) : String?
    object Standard : TypeParser() {
      override fun parse(type : String) = if (type.startsWith("各") || type.startsWith("普")) "" else type.substring(0, type.offsetByCodePoints(0, 1))
    }
    object Keisei : TypeParser() {
      override fun parse(type : String) = when (type) {
        "普通" -> ""
        "快速" -> "快"
        else   -> null
      }
    }
  }
  // tab is 1-indexed
  data class Options(val tab : Int, val typeParser : TypeParser)

  @kotlin.Throws(IOException::class, ParseException::class)
  public override fun parseStream(dataName : String, srcStream : BufferedInputStream, arg : Any?) : DataMap {
    arg as Options
    val result = DataMap()
    val src = BufferedReader(InputStreamReader(srcStream))

    repeat(arg.tab - 1) { find(src, "class=\"search-result-body\"") }
    val contents = find(src, "class=\"search-result-body\"") ?: throw ParseException("EkitanParser : can't parse page ${dataName}", 0)

    val buildData = ArrayList<DataMap>()
    while (true) {
      val next = find(src, "data-tr-type=\"") ?: break // End of page
      if (next.contains("search-result-footer")) break // End of this tab

      val type = find(src, "\"") ?: throw ParseException("EkitanParser : can't find train type", -1) // too annoying to figure out the position so -1
      find(src, "&departure=")
      val timeText = find(src, "&") ?: throw ParseException("EkitanParser : can't find departure time", -1)
      val time = timeText.toInt() ?: throw ParseException("EkitanParser : can't parse departure time : \"${timeText}\"", -1) // Format is HHMM
      val hour = time / 100
      val minute = time % 100

      val mark = arg.typeParser.parse(type)
      if (null != mark) {
        val departure = DataMap()
        departure.putInt(Const.DATA_KEY_DEPTIME, (if (hour < 3) 24 + hour else hour) * 3600 + minute * 60)
        departure.putString(Const.DATA_KEY_EXTRA, mark)
        buildData.add(departure)
      }
    }

    result.putDataMapArrayList(Const.DATA_KEY_DEPLIST, buildData)
    return result
  }
}
