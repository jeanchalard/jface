package com.j.jface;

public class Const
{
  public static final int SCREEN_SIZE = 320;
  public static final int ANIM_DURATION = 1000; // milliseconds
  public static final long SECONDS_TO_UTC = -9 * 3600;

  public static final String[] WEEKDAYS = { "日", "月", "火", "水", "木", "金", "土", "日" };
  public static final String DATA_PATH = "/jwatch/Data";
  public static final String CONFIG_PATH = "/jwatch/Conf";

  public static final String DATA_KEY_DEPTIME = "depTime";
  public static final String DATA_KEY_EXTRA = "extra";
  public static final String DATA_KEY_DEPLIST = "depList";
  public static final String DATA_KEY_ADHOC = "adHoc";
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

  public static class GeofenceParams
  {
    public final String name;
    public final double latitude; public final double longitude; public final float radius;
    public GeofenceParams(final String n, final double lat, final double lon, final float rad)
    { name = n; latitude = lat; longitude = lon; radius = rad; }
  }
  // 1km around the center. That should include home and 北千住.
  public static final GeofenceParams GEOFENCE_HOME = new GeofenceParams("home", 35.7466148, 139.7996878, 1000);
  // For work 400m is enough.
  public static final GeofenceParams GEOFENCE_WORK = new GeofenceParams("work", 35.6607004, 139.7291515, 400);
  // For Nippori we want info when heading there, so 2~2.5km should be fine so as not to encroach on home.
  public static final GeofenceParams GEOFENCE_NIPPORI = new GeofenceParams("nippori", 35.7278246, 139.7715682, 2000);
}
