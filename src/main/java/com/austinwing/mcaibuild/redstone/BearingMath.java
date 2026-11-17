package com.austinwing.mcaibuild.redstone;
public final class BearingMath {
    private static final String[] COMPASS={"S","SE","E","NE","N","NW","W","SW"};
    private BearingMath(){}
    public static double wrapDegrees(double d){double v=d%360.0;if(v>=180)v-=360;if(v< -180)v+=360;return v;}
    public static double relativeBearingDegrees(double dx,double dz,double yaw){double world=Math.toDegrees(Math.atan2(dx,dz));return wrapDegrees(-(world+yaw));}
    public static String compass(double dx,double dz){int i=Math.floorMod((int)Math.round(Math.toDegrees(Math.atan2(dx,dz))/45.0),8);return COMPASS[i];}
}
