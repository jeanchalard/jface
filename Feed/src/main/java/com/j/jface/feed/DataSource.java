package com.j.jface.feed;

import com.j.jface.Const;

class DataSource
{
  public final String name;
  public final String url;
  public final Class<? extends FeedParser> parser;

  public DataSource(final String n, final String u, final Class<? extends FeedParser> p)
  {
    name = n;
    url = u;
    parser = p;
  }

  public static final DataSource[] ALL_SOURCES = {
   new DataSource(Const.日比谷線_北千住_平日,
    "http://www.tokyometro.jp/station/kita-senju/timetable/hibiya/a/index.html", HibiyaParser.class),
   new DataSource(Const.日比谷線_北千住_休日,
    "http://www.tokyometro.jp/station/kita-senju/timetable/hibiya/a/holiday.html", HibiyaParser.class),
   new DataSource(Const.日比谷線_六本木_平日,
    "http://www.tokyometro.jp/station/roppongi/timetable/hibiya/b/index.html", HibiyaParser.class),
   new DataSource(Const.日比谷線_六本木_休日,
    "http://www.tokyometro.jp/station/roppongi/timetable/hibiya/b/holiday.html", HibiyaParser.class),
   new DataSource(Const.京成線_上野方面_平日,
    "http://keisei.ekitan.com/norikae/pc/T5?dir=35&dw=0&slCode=254-4&d=1", KeiseiParser.class),
   new DataSource(Const.京成線_上野方面_休日,
    "http://keisei.ekitan.com/norikae/pc/T5?dir=49&dw=1&slCode=254-4&d=1", KeiseiParser.class),
   new DataSource(Const.京成線_成田方面_平日,
    "http://keisei.ekitan.com/norikae/pc/T5?dir=35&dw=0&slCode=254-4&d=2", KeiseiParser.class),
   new DataSource(Const.京成線_成田方面_休日,
    "http://keisei.ekitan.com/norikae/pc/T5?dir=50&dw=1&slCode=254-4&d=2", KeiseiParser.class),
   new DataSource(Const.京成線_日暮里_平日,
    "http://keisei.ekitan.com/norikae/pc/T5?dir=44&dw=0&slCode=254-1&d=2", KeiseiParser.class),
   new DataSource(Const.京成線_日暮里_休日,
    "http://keisei.ekitan.com/norikae/pc/T5?dir=52&dw=1&slCode=254-1&d=2", KeiseiParser.class)
  };
}
