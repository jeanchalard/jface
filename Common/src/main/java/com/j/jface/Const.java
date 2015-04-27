package com.j.jface;

public class Const
{
  public static final int SCREEN_SIZE = 320;
  public static final int ANIM_DURATION = 1000; // milliseconds
  public static final long SECONDS_TO_UTC = -9 * 3600;
  public static final long UPDATE_DELAY_MILLIS = 7 * 24 * 60 * 60 * 1000; // One week in millis

  public static final String[] WEEKDAYS = { "日", "月", "火", "水", "木", "金", "土", "日" };
  public static final String DATA_PATH = "/jwatch/Data";
  public static final String CONFIG_PATH = "/jwatch/Conf";
  public static final String LOCATION_PATH = "/jwatch/Location";

  public static final String DATA_KEY_DEPTIME = "depTime";
  public static final String DATA_KEY_EXTRA = "extra";
  public static final String DATA_KEY_DEPLIST = "depList";
  public static final String DATA_KEY_ADHOC = "adHoc";
  public static final String DATA_KEY_INSIDE = "inside";
  public static final String DATA_KEY_SUCCESSFUL_UPDATE_DATE = "updateTime";
  public static final String DATA_KEY_LAST_STATUS = "lastStatus";
  public static final String DATA_KEY_STATUS_UPDATE_DATE = "lastStatusDate";
  public static final String CONFIG_KEY_BACKGROUND = "background";

  public static final String 日比谷線_北千住_平日 = "日比谷線・北千住・平日";
  public static final String 日比谷線_北千住_休日 = "日比谷線・北千住・休日";
  public static final String 日比谷線_六本木_平日 = "日比谷線・六本木・平日";
  public static final String 日比谷線_六本木_休日 = "日比谷線・六本木・休日";
  public static final String 京成線_上野方面_平日 = "京成線・上野方面・平日";
  public static final String 京成線_上野方面_休日 = "京成線・上野方面・休日";
  public static final String 京成線_成田方面_平日 = "京成線・成田方面・平日";
  public static final String 京成線_成田方面_休日 = "京成線・成田方面・休日";
  public static final String 京成線_日暮里_平日 = "京成線・日暮里・平日";
  public static final String 京成線_日暮里_休日 = "京成線・日暮里・休日";

  public static final String[] ALL_DEPLIST_DATA_PATHS =
   { 日比谷線_北千住_平日, 日比谷線_北千住_休日, 日比谷線_六本木_平日, 日比谷線_六本木_休日,
    京成線_上野方面_平日, 京成線_上野方面_休日, 京成線_成田方面_平日, 京成線_成田方面_休日,
    京成線_日暮里_平日, 京成線_日暮里_休日 };

  public static final String HOME_FENCE_NAME = "home";
  public static final String WORK_FENCE_NAME = "work";
  public static final String 日暮里_FENCE_NAME = "nippori";
  public static final String 東京_FENCE_NAME = "tokyo";
  public static final String[] ALL_FENCE_NAMES =
   { HOME_FENCE_NAME, WORK_FENCE_NAME, 日暮里_FENCE_NAME, 東京_FENCE_NAME };
}
