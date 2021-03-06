package com.j.jface.feed

import com.j.jface.Const

class DataSource private constructor(val name : String, val url : String, val parser : Class<out FeedParser?>, val arg : Any? = null) {
  companion object {
    private val J_SOURCES = arrayOf(
     DataSource(Const.日比谷線_北千住_平日,
      "https://www.tokyometro.jp/station/kita-senju/timetable/hibiya/a/index.html", HibiyaParser::class.java),
     DataSource(Const.日比谷線_北千住_休日,
      "https://www.tokyometro.jp/station/kita-senju/timetable/hibiya/a/holiday.html", HibiyaParser::class.java),
     DataSource(Const.日比谷線_六本木_平日,
      "https://www.tokyometro.jp/station/roppongi/timetable/hibiya/b/index.html", HibiyaParser::class.java),
     DataSource(Const.日比谷線_六本木_休日,
      "https://www.tokyometro.jp/station/roppongi/timetable/hibiya/b/holiday.html", HibiyaParser::class.java),
     DataSource(Const.山手線_日暮里_渋谷方面_平日,
      "https://ekitan.com/timetable/railway/line-station/182-17/d2?dw=0&view=list", EkitanParser::class.java, 2),
     DataSource(Const.山手線_日暮里_渋谷方面_休日,
      "https://ekitan.com/timetable/railway/line-station/182-17/d2?dw=2&view=list", EkitanParser::class.java, 2),
     DataSource(Const.山手線_渋谷_日暮里方面_平日,
      "https://ekitan.com/timetable/railway/line-station/182-4/d1?dw=0&view=list", EkitanParser::class.java, 1),
     DataSource(Const.山手線_渋谷_日暮里方面_休日,
      "https://ekitan.com/timetable/railway/line-station/182-4/d1?dw=2&view=list", EkitanParser::class.java, 1),
     DataSource(Const.京成線_千住大橋_上野方面_平日,
      "http://keisei.ekitan.com/norikae/pc/T5?dir=35&dw=0&slCode=254-4&d=1", KeiseiParser::class.java),
     DataSource(Const.京成線_千住大橋_上野方面_休日,
      "http://keisei.ekitan.com/norikae/pc/T5?dir=49&dw=1&slCode=254-4&d=1", KeiseiParser::class.java),
     DataSource(Const.京成線_千住大橋_成田方面_平日,
      "http://keisei.ekitan.com/norikae/pc/T5?dir=35&dw=0&slCode=254-4&d=2", KeiseiParser::class.java),
     DataSource(Const.京成線_千住大橋_成田方面_休日,
      "http://keisei.ekitan.com/norikae/pc/T5?dir=50&dw=1&slCode=254-4&d=2", KeiseiParser::class.java),
     DataSource(Const.京成線_日暮里_千住大橋方面_平日,
      "http://keisei.ekitan.com/norikae/pc/T5?dir=44&dw=0&slCode=254-1&d=2", KeiseiParser::class.java),
     DataSource(Const.京成線_日暮里_千住大橋方面_休日,
      "http://keisei.ekitan.com/norikae/pc/T5?dir=52&dw=1&slCode=254-1&d=2", KeiseiParser::class.java)
    )
    private val RIO_SOURCES = arrayOf(
     DataSource(Const.京王線_稲城駅_新宿方面_平日,
      "https://timetable.ekitan.com/railway/line-station/261-4?d=1&dw=0&view=list", EkitanParser::class.java, 1),
     DataSource(Const.京王線_稲城駅_新宿方面_休日,
      "https://timetable.ekitan.com/railway/line-station/261-4?d=1&dw=2&view=list", EkitanParser::class.java, 1),
     DataSource(Const.都営三田線_本蓮沼_目黒方面_平日,
      "https://timetable.ekitan.com/railway/line-station/224-19?d=1&dw=0&view=list", EkitanParser::class.java, 1),
     DataSource(Const.都営三田線_本蓮沼_目黒方面_休日,
      "https://timetable.ekitan.com/railway/line-station/224-19?d=1&dw=2&view=list", EkitanParser::class.java, 1),
     DataSource(Const.大江戸線_六本木_新宿方面_平日,
      "https://timetable.ekitan.com/railway/line-station/225-35?d=2&view=list", EkitanParser::class.java, 2),
     DataSource(Const.大江戸線_六本木_新宿方面_休日,
      "https://timetable.ekitan.com/railway/line-station/225-35?d=2&dw=2&view=list", EkitanParser::class.java, 2))
    @JvmField
    val ALL_SOURCES = if (Const.RIO_MODE) RIO_SOURCES else J_SOURCES
  }

}