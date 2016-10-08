package com.j.jface;

import java.util.HashMap;

public class Const
{
  public static final boolean RIO_MODE = false;
  public static final boolean ROUND_SCREEN = !RIO_MODE;
  public static final int SCREEN_SIZE = RIO_MODE ? 280 : 320;

  public static final int ANIM_DURATION = 1000; // milliseconds
  public static final long MILLISECONDS_TO_UTC = 9 * 3600 * 1000;
  public static final long UPDATE_DELAY_MILLIS = 7 * 24 * 60 * 60 * 1000; // One week in millis
  public static final int DISPLAYED_DEPARTURES_PER_LINE = 3;

  public static final String[] WEEKDAYS = { "日", "月", "火", "水", "木", "金", "土", "日" };
  public static final String DATA_PATH = "/jwatch/Data";
  public static final String CONFIG_PATH = "/jwatch/Conf";
  public static final String LOCATION_PATH = "/jwatch/Location";
  public static final String ACTIVITY_PATH = "/jwatch/Activity";
  public static final String DATA_PATH_SUFFIX_STATUS = "_status";

  public static final String DATA_KEY_DEPTIME = "depTime";
  public static final String DATA_KEY_EXTRA = "extra";
  public static final String DATA_KEY_DEPLIST = "depList";
  public static final String DATA_KEY_TOPIC = "topic";
  public static final String DATA_KEY_ADHOC = "adHoc";
  public static final String DATA_KEY_INSIDE = "inside";
  public static final String DATA_KEY_SUCCESSFUL_UPDATE_DATE = "updateTime";
  public static final String DATA_KEY_LAST_STATUS = "lastStatus";
  public static final String DATA_KEY_STATUS_UPDATE_DATE = "lastStatusDate";
  public static final String DATA_KEY_DEBUG_TIME_OFFSET = "debugTimeOffset";
  public static final String DATA_KEY_DEBUG_FENCES = "debugFences";
  public static final String DATA_KEY_LAST_ACTIVITY_MNEMONIC = "lastActivityMnemonic";
  public static final String DATA_KEY_LAST_ACTIVITY_START_TIME = "lastActivityStartTime";
  public static final String CONFIG_KEY_BACKGROUND = "background";

  public static final String 日比谷線_北千住_平日 = "日比谷線・北千住・平日";
  public static final String 日比谷線_北千住_休日 = "日比谷線・北千住・休日";
  public static final String 日比谷線_六本木_平日 = "日比谷線・六本木・平日";
  public static final String 日比谷線_六本木_休日 = "日比谷線・六本木・休日";
  public static final String 日比谷線_三ノ輪_中目黒方面_平日 = "日比谷線・三ノ輪・中目黒方面・平日";
  public static final String 日比谷線_三ノ輪_中目黒方面_休日 = "日比谷線・三ノ輪・中目黒方面・休日";
  public static final String 日比谷線_三ノ輪_北千住方面_平日 = "日比谷線・三ノ輪・北千住方面・平日";
  public static final String 日比谷線_三ノ輪_北千住方面_休日 = "日比谷線・三ノ輪・北千住方面・休日";
  public static final String 京成線_千住大橋_上野方面_平日 = "京成線・千住大橋・上野方面・平日";
  public static final String 京成線_千住大橋_上野方面_休日 = "京成線・千住大橋・上野方面・休日";
  public static final String 京成線_千住大橋_成田方面_平日 = "京成線・千住大橋・成田方面・平日";
  public static final String 京成線_千住大橋_成田方面_休日 = "京成線・千住大橋・成田方面・休日";
  public static final String 京成線_日暮里_千住大橋方面_平日 = "京成線・日暮里・千住大橋方面・平日";
  public static final String 京成線_日暮里_千住大橋方面_休日 = "京成線・日暮里・千住大橋方面・休日";
  public static final String 京成線_お花茶屋_上野方面_平日 = "京成線・お花茶屋・上野方面・平日";
  public static final String 京成線_お花茶屋_上野方面_休日 = "京成線・お花茶屋・上野方面・休日";
  public static final String 京成線_立石_人形町方面_平日 = "京成線・立石・人形町方面・平日";
  public static final String 京成線_立石_人形町方面_休日 = "京成線・立石・人形町方面・休日";

  public static final HashMap<String, String> HEADSIGNS = new HashMap <>();
  static {
    HEADSIGNS.put(日比谷線_北千住_平日, "北千住 ▶ 六本木");
    HEADSIGNS.put(日比谷線_北千住_休日, "北千住 ▶ 六本木");
    HEADSIGNS.put(日比谷線_六本木_平日, "六本木 ▶ 北千住");
    HEADSIGNS.put(日比谷線_六本木_休日, "六本木 ▶ 北千住");
    HEADSIGNS.put(日比谷線_三ノ輪_中目黒方面_平日, "三ノ輪 ▶ 立石");
    HEADSIGNS.put(日比谷線_三ノ輪_中目黒方面_休日, "三ノ輪 ▶ 立石");
    HEADSIGNS.put(日比谷線_三ノ輪_北千住方面_平日, "三ノ輪 ▶ お花茶屋");
    HEADSIGNS.put(日比谷線_三ノ輪_北千住方面_休日, "三ノ輪 ▶ お花茶屋");
    HEADSIGNS.put(京成線_千住大橋_上野方面_平日, "千住大橋 ▶ 日暮里");
    HEADSIGNS.put(京成線_千住大橋_上野方面_休日, "千住大橋 ▶ 日暮里");
    HEADSIGNS.put(京成線_千住大橋_成田方面_平日, "千住大橋 ▶ 成田");
    HEADSIGNS.put(京成線_千住大橋_成田方面_休日, "千住大橋 ▶ 成田");
    HEADSIGNS.put(京成線_日暮里_千住大橋方面_平日, "日暮里 ▶ 千住大橋");
    HEADSIGNS.put(京成線_日暮里_千住大橋方面_休日, "日暮里 ▶ 千住大橋");
    HEADSIGNS.put(京成線_お花茶屋_上野方面_平日, "お花茶屋 ▶ 三ノ輪");
    HEADSIGNS.put(京成線_お花茶屋_上野方面_休日, "お花茶屋 ▶ 三ノ輪");
    HEADSIGNS.put(京成線_立石_人形町方面_平日, "立石 ▶ 人形町");
    HEADSIGNS.put(京成線_立石_人形町方面_休日, "立石 ▶ 人形町");
  }

  public static final String[] J_DEPLIST_DATA_PATHS =
   {
    日比谷線_北千住_平日, 日比谷線_北千住_休日,
    日比谷線_六本木_平日, 日比谷線_六本木_休日,
    京成線_千住大橋_上野方面_平日, 京成線_千住大橋_上野方面_休日,
    京成線_千住大橋_成田方面_平日, 京成線_千住大橋_成田方面_休日,
    京成線_日暮里_千住大橋方面_平日, 京成線_日暮里_千住大橋方面_休日 };
  public static final String[] RIO_DEPLIST_DATA_PATHS =
   {
    日比谷線_三ノ輪_中目黒方面_平日, 日比谷線_三ノ輪_中目黒方面_休日,
    日比谷線_三ノ輪_北千住方面_平日, 日比谷線_三ノ輪_北千住方面_休日,
    京成線_お花茶屋_上野方面_平日, 京成線_お花茶屋_上野方面_休日,
    京成線_立石_人形町方面_平日, 京成線_立石_人形町方面_休日,
    日比谷線_六本木_平日
   };
  public static final String[] ALL_DEPLIST_DATA_PATHS = RIO_MODE ? RIO_DEPLIST_DATA_PATHS : J_DEPLIST_DATA_PATHS;

  public static final String 千住大橋_FENCE_NAME = "千住大橋";
  public static final String 六本木_FENCE_NAME = "六本木";
  public static final String 日暮里_FENCE_NAME = "日暮里";
  public static final String 東京_FENCE_NAME = "東京";
  public static final String 立石_FENCE_NAME = "立石";
  public static final String 三ノ輪_FENCE_NAME = "三ノ輪";
  public static final String[] J_FENCE_NAMES =
   { 千住大橋_FENCE_NAME, 六本木_FENCE_NAME, 日暮里_FENCE_NAME, 東京_FENCE_NAME };
  public static final String[] RIO_FENCE_NAMES =
   { 三ノ輪_FENCE_NAME, 立石_FENCE_NAME, 六本木_FENCE_NAME, 東京_FENCE_NAME };
  public static final String[] ALL_FENCE_NAMES = RIO_MODE ? RIO_FENCE_NAMES : J_FENCE_NAMES;
}
