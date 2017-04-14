package com.j.jface.feed;

import android.support.annotation.Nullable;

import com.j.jface.Const;

public class Fences
{
  public static class Params
  {
    public final String name;
    public final double latitude; public final double longitude; public final float radius;
    private Params(final String n, final double lat, final double lon, final float rad)
    { name = n; latitude = lat; longitude = lon; radius = rad; }
  }
  // 1km around the center. That should include home and 北千住.
  private static final Params 千住大橋 = new Params(Const.千住大橋_FENCE_NAME, 35.7466148, 139.7996878, 1000);
  // For work 400m is enough.
  private static final Params 六本木 = new Params(Const.六本木_FENCE_NAME, 35.6607004, 139.7291515, 400);
  // For 日暮里 we want info when heading there, so 2~2.5km should be fine so as not to encroach on home.
  private static final Params 日暮里 = new Params(Const.日暮里_FENCE_NAME, 35.7278246, 139.7715682, 2000);
  private static final Params 東京 = new Params(Const.東京_FENCE_NAME, 35.687163, 139.7578258, 15000);
  private static final Params 本蓮沼 = new Params(Const.本蓮沼_FENCE_NAME, 35.7687808, 139.7020865, 2000);
  private static final Params 稲城 = new Params(Const.稲城_FENCE_NAME, 35.6347501, 139.5008493, 600);

  @Nullable public static Params paramsFromName(final String name)
  {
    if (千住大橋.name.equals(name)) return 千住大橋;
    if (六本木.name.equals(name)) return 六本木;
    if (日暮里.name.equals(name)) return 日暮里;
    if (本蓮沼.name.equals(name)) return 本蓮沼;
    if (稲城.name.equals(name)) return 稲城;
    if (東京.name.equals(name)) return 東京;
    return null;
  }
}
