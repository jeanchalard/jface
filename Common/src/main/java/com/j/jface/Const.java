package com.j.jface;

import androidx.annotation.NonNull;

import java.util.HashMap;

@SuppressWarnings({"ConstantConditions", "NonAsciiCharacters"})
public class Const
{
  @NonNull public static final String APP_PACKAGE = "com.j.jface";

  public static final boolean RIO_MODE = false;
  public static final boolean ROUND_SCREEN = !RIO_MODE;
  public static final int SCREEN_SIZE = RIO_MODE ? 280 : 320;

  public static final int ANIM_DURATION = 1000; // milliseconds
  public static final long MILLISECONDS_TO_UTC = 9 * 3600 * 1000;
  public static final long UPDATE_DELAY_MILLIS = 7 * 24 * 60 * 60 * 1000; // One week in millis
  public static final int DISPLAYED_DEPARTURES_PER_LINE = 3;

  public static final int COARSE_AMBIENT_MODE = 1; // Ambient mode has fewer bits per pixel
  public static final int AMBIENT_MODE = 2;
  public static final int BURN_IN_PROTECTION_MODE = 4;
  public static final int WAITING_FOR_TAP = 8;

  public static final int CHOOSE_IMAGE_RESULT_CODE = 100;
  public static final int GOOGLE_SIGN_IN_RESULT_CODE = 300;
  public static final int NOTIFICATION_RESULT_CODE = 400;
  public static final int NOTIFICATION_TYPE_SUGGESTION = 401;
  public static final int NOTIFICATION_TYPE_SPLIT = 402;
  public static final int NOTIFICATION_TYPE_FILLIN = 403;
  public static final int NOTIFICATION_TYPE_CANCEL_DONE = 404;

  public static final int SECONDS_渋谷_TO_日暮里 = 29 * 60 + 30; // About 29 minutes and 30 seconds of riding the train
  public static final int SECONDS_千住大橋_TO_日暮里 = 6 * 60; // About 6 minutes of riding the train

  @NonNull public static final String INTERNAL_PERSISTED_VALUES_FILES = "SystemValues";

  @NonNull public static final String[] WEEKDAYS = { "日", "月", "火", "水", "木", "金", "土", "日" };
  @NonNull private static final String JFACE_TOP = "jface";

  // Must contain JFACE_TOP as it's also used as DB_WEAR_TOP
  @NonNull public static final String DATA_PATH = "/" + JFACE_TOP + "/Data";
  @NonNull public static final String CONFIG_PATH = "/" + JFACE_TOP + "/Conf";
  @NonNull public static final String LOCATION_PATH = "/" + JFACE_TOP + "/Location";
  @NonNull public static final String DATA_PATH_SUFFIX_STATUS = "_status";

  @NonNull public static final String DB_APP_TOP_PATH = "JOrg";
  @NonNull public static final String DB_ORG_TOP = "todo";

  @NonNull public static final String DATA_KEY_DEPTIME = "depTime";
  @NonNull public static final String DATA_KEY_EXTRA = "extra";
  @NonNull public static final String DATA_KEY_DEPLIST = "depList";
  @NonNull public static final String DATA_KEY_USER_MESSAGE = "userMessage";
  @NonNull public static final String DATA_KEY_USER_MESSAGE_COLORS = "userMessageColors";
  @NonNull public static final String DATA_KEY_HEART_MESSAGE = "heartMessage";
  @SuppressWarnings("unused") @NonNull public static final String DATA_KEY_ADHOC = "adHoc";
  @NonNull public static final String DATA_KEY_INSIDE = "inside";
  @NonNull public static final String DATA_KEY_SUCCESSFUL_UPDATE_DATE = "updateTime";
  @NonNull public static final String DATA_KEY_LAST_STATUS = "lastStatus";
  @NonNull public static final String DATA_KEY_STATUS_UPDATE_DATE = "lastStatusDate";
  @NonNull public static final String DATA_KEY_DEBUG_TIME_OFFSET = "debugTimeOffset";
  @NonNull public static final String DATA_KEY_DEBUG_FENCES = "debugFences";
  @NonNull public static final String DATA_KEY_BACKGROUND = "background";
  @NonNull public static final String CONFIG_KEY_BACKGROUND = "background";
  @NonNull public static final String CONFIG_KEY_SERVER_KEY = "key";
  @NonNull public static final String CONFIG_KEY_WEAR_LISTENER_PREFIX = "wearListener_";
  @NonNull public static final String CONFIG_KEY_WEAR_LISTENER_ID = "id";
  @NonNull public static final String FIREBASE_MESSAGE_WEAR_PATH = "wear_path";
  public static final int USER_MESSAGE_DEFAULT_COLOR = 0xFFAACCAA;

  @NonNull public static final String EXTRA_TODO_ID = "todoId";
  @NonNull public static final String EXTRA_NOTIF_ID = "notifId";
  @NonNull public static final String EXTRA_NOTIF_TYPE = "notifType";
  @NonNull public static final String EXTRA_PATH = "path";
  @NonNull public static final String EXTRA_WEAR_DATA = "wearData";
  @NonNull public static final String EXTRA_FILLIN_FIELD = "fillinField";
  @NonNull public static final String EXTRA_FILLIN_REPLY_INDEX = "fillinReplyIndex";
  @NonNull public static final String EXTRA_WAS_DISMISSED = "wasDismissed";

  @NonNull public static final String EXTRA_TODO_SUBITEMS = "todoSubitems";

  @NonNull public static final String 日比谷線_北千住_平日 = "日比谷線・北千住・平日";
  @NonNull public static final String 日比谷線_北千住_休日 = "日比谷線・北千住・休日";
  @NonNull public static final String 日比谷線_六本木_平日 = "日比谷線・六本木・平日";
  @NonNull public static final String 日比谷線_六本木_休日 = "日比谷線・六本木・休日";
  @NonNull public static final String 山手線_日暮里_渋谷方面_平日 = "山手線・日暮里・平日";
  @NonNull public static final String 山手線_日暮里_渋谷方面_休日 = "山手線・日暮里・休日";
  @NonNull public static final String 山手線_渋谷_日暮里方面_平日 = "山手線・渋谷・平日";
  @NonNull public static final String 山手線_渋谷_日暮里方面_休日 = "山手線・渋谷・休日";
  @NonNull public static final String 大江戸線_六本木_新宿方面_平日 = "大江戸線・六本木・新宿方面・平日";
  @NonNull public static final String 大江戸線_六本木_新宿方面_休日 = "大江戸線・六本木・新宿方面・休日";
  @NonNull public static final String 京王線_稲城駅_新宿方面_平日 = "京王線・稲城駅・新宿方面・平日";
  @NonNull public static final String 京王線_稲城駅_新宿方面_休日 = "京王線・稲城駅・新宿方面・休日";
  @NonNull public static final String 都営三田線_本蓮沼_目黒方面_平日 = "都営三田線・本蓮沼・目黒方面・平日";
  @NonNull public static final String 都営三田線_本蓮沼_目黒方面_休日 = "都営三田線・本蓮沼・目黒方面・休日";
  @NonNull public static final String 京成線_千住大橋_上野方面_平日 = "京成線・千住大橋・上野方面・平日";
  @NonNull public static final String 京成線_千住大橋_上野方面_休日 = "京成線・千住大橋・上野方面・休日";
  @NonNull public static final String 京成線_千住大橋_成田方面_平日 = "京成線・千住大橋・成田方面・平日";
  @NonNull public static final String 京成線_千住大橋_成田方面_休日 = "京成線・千住大橋・成田方面・休日";
  @NonNull public static final String 京成線_日暮里_千住大橋方面_平日 = "京成線・日暮里・千住大橋方面・平日";
  @NonNull public static final String 京成線_日暮里_千住大橋方面_休日 = "京成線・日暮里・千住大橋方面・休日";

  @NonNull public static final HashMap<String, String> HEADSIGNS = new HashMap <>();

  static {
    HEADSIGNS.put(日比谷線_北千住_平日, "北千住 ▶ 六本木");
    HEADSIGNS.put(日比谷線_北千住_休日, "北千住 ▶ 六本木");
    HEADSIGNS.put(日比谷線_六本木_平日, "六本木 ▶ 北千住");
    HEADSIGNS.put(日比谷線_六本木_休日, "六本木 ▶ 北千住");
    HEADSIGNS.put(山手線_日暮里_渋谷方面_平日, "日暮里 ▶ 渋谷");
    HEADSIGNS.put(山手線_日暮里_渋谷方面_休日, "日暮里 ▶ 渋谷");
    HEADSIGNS.put(山手線_渋谷_日暮里方面_平日, "渋谷 ▶ 日暮里");
    HEADSIGNS.put(山手線_渋谷_日暮里方面_休日, "渋谷 ▶ 日暮里");
    HEADSIGNS.put(大江戸線_六本木_新宿方面_平日, "六本木 ▶ 新宿");
    HEADSIGNS.put(大江戸線_六本木_新宿方面_休日, "六本木 ▶ 新宿");
    HEADSIGNS.put(京王線_稲城駅_新宿方面_平日, "稲城 ▶ 新宿");
    HEADSIGNS.put(京王線_稲城駅_新宿方面_休日, "稲城 ▶ 新宿");
    HEADSIGNS.put(都営三田線_本蓮沼_目黒方面_平日, "本蓮沼 ▶ 巣鴨");
    HEADSIGNS.put(都営三田線_本蓮沼_目黒方面_休日, "本蓮沼 ▶ 巣鴨");
    HEADSIGNS.put(京成線_千住大橋_上野方面_平日, "千住大橋 ▶ 日暮里");
    HEADSIGNS.put(京成線_千住大橋_上野方面_休日, "千住大橋 ▶ 日暮里");
    HEADSIGNS.put(京成線_千住大橋_成田方面_平日, "千住大橋 ▶ 成田");
    HEADSIGNS.put(京成線_千住大橋_成田方面_休日, "千住大橋 ▶ 成田");
    HEADSIGNS.put(京成線_日暮里_千住大橋方面_平日, "日暮里 ▶ 千住大橋");
    HEADSIGNS.put(京成線_日暮里_千住大橋方面_休日, "日暮里 ▶ 千住大橋");
  }

  @NonNull private static final String[] J_DEPLIST_DATA_PATHS =
   {
//    日比谷線_北千住_平日, 日比谷線_北千住_休日,
//    日比谷線_六本木_平日, 日比谷線_六本木_休日,
    山手線_日暮里_渋谷方面_平日, 山手線_日暮里_渋谷方面_休日,
    山手線_渋谷_日暮里方面_平日, 山手線_渋谷_日暮里方面_休日,
    京成線_千住大橋_上野方面_平日, 京成線_千住大橋_上野方面_休日,
    京成線_千住大橋_成田方面_平日, 京成線_千住大橋_成田方面_休日,
    京成線_日暮里_千住大橋方面_平日, 京成線_日暮里_千住大橋方面_休日 };
  @NonNull private static final String[] RIO_DEPLIST_DATA_PATHS =
   {
    京王線_稲城駅_新宿方面_平日, 京王線_稲城駅_新宿方面_休日,
    都営三田線_本蓮沼_目黒方面_平日, 都営三田線_本蓮沼_目黒方面_休日,
    大江戸線_六本木_新宿方面_平日, 大江戸線_六本木_新宿方面_休日
   };
  @NonNull public static final String[] ALL_DEPLIST_DATA_PATHS = RIO_MODE ? RIO_DEPLIST_DATA_PATHS : J_DEPLIST_DATA_PATHS;

  @NonNull public static final String 千住大橋_FENCE_NAME = "千住大橋";
  @NonNull public static final String 六本木_FENCE_NAME = "六本木";
  @NonNull public static final String 渋谷_FENCE_NAME = "渋谷";
  @NonNull public static final String 日暮里_FENCE_NAME = "日暮里";
  @NonNull public static final String 東京_FENCE_NAME = "東京";
  @NonNull public static final String 稲城_FENCE_NAME = "稲城";
  @NonNull public static final String 本蓮沼_FENCE_NAME = "本蓮沼";
  @NonNull private static final String[] J_FENCE_NAMES = { 千住大橋_FENCE_NAME, 渋谷_FENCE_NAME, 日暮里_FENCE_NAME, 東京_FENCE_NAME, 六本木_FENCE_NAME };
  @NonNull private static final String[] RIO_FENCE_NAMES = { 稲城_FENCE_NAME, 本蓮沼_FENCE_NAME, 六本木_FENCE_NAME, 東京_FENCE_NAME };
  @NonNull public static final String[] ALL_FENCE_NAMES = RIO_MODE ? RIO_FENCE_NAMES : J_FENCE_NAMES;
}
