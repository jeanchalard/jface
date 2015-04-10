package com.j.jface.feed;

import android.support.annotation.Nullable;

import com.j.jface.Const;

public class Fences
{
  public static class Params
  {
    public final String name;
    public final double latitude; public final double longitude; public final float radius;
    public Params(final String n, final double lat, final double lon, final float rad)
    { name = n; latitude = lat; longitude = lon; radius = rad; }
  }
  // 1km around the center. That should include home and 北千住.
  public static final Params HOME = new Params(Const.HOME_FENCE_NAME, 35.7466148, 139.7996878, 1000);
  // For work 400m is enough.
  public static final Params WORK = new Params(Const.WORK_FENCE_NAME, 35.6607004, 139.7291515, 400);
  // For 日暮里 we want info when heading there, so 2~2.5km should be fine so as not to encroach on home.
  public static final Params 日暮里 = new Params(Const.日暮里_FENCE_NAME, 35.7278246, 139.7715682, 2000);
  public static final Params 東京 = new Params(Const.東京_FENCE_NAME, 35.687163, 139.7578258, 15000);

  @Nullable public static Params paramsFromName(final String name)
  {
    if (HOME.name.equals(name)) return HOME;
    if (WORK.name.equals(name)) return WORK;
    if (日暮里.name.equals(name)) return 日暮里;
    if (東京.name.equals(name)) return 東京;
    return null;
  }
}
