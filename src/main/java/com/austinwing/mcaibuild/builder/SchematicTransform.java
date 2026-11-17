package com.austinwing.mcaibuild.builder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
public final class SchematicTransform {
    private static final String[] CLOCKWISE={"north","east","south","west"};
    private SchematicTransform(){}
    public static int normalize(int r){return Math.floorMod(r,4);}
    public static int rotatedX(int r,int w,int l,int x,int z){return switch(normalize(r)){case 1->l-1-z;case 2->w-1-x;case 3->z;default->x;};}
    public static int rotatedZ(int r,int w,int l,int x,int z){return switch(normalize(r)){case 1->x;case 2->l-1-z;case 3->w-1-x;default->z;};}
    public static int rotatedWidth(int r,int w,int l){return normalize(r)%2==0?w:l;}
    public static int rotatedLength(int r,int w,int l){return normalize(r)%2==0?l:w;}
    public static String rotateDirection(String d,int r){if(d==null)return null;int i=-1;for(int n=0;n<4;n++)if(CLOCKWISE[n].equals(d))i=n;return i<0?d:CLOCKWISE[Math.floorMod(i+r,4)];}
    public static String rotateProperty(int rotation,String key,String value){
        int r=normalize(rotation);if(r==0||key==null||value==null)return value;
        switch(key){
            case "facing":return rotateDirection(value.toLowerCase(Locale.ROOT),r);
            case "axis":if(r%2==1){if(value.equals("x"))return "z";if(value.equals("z"))return "x";}return value;
            case "rotation":try{return Integer.toString(Math.floorMod(Integer.parseInt(value)+r*4,16));}catch(NumberFormatException e){return value;}
            case "shape":return rotateRailShape(value,r);
            default:return value;
        }
    }
    public static Map<String,String> rotateProperties(int rotation,Map<String,String> properties){
        int r=normalize(rotation);if(r==0||properties==null||properties.isEmpty())return properties==null?Map.of():properties;
        Map<String,String> out=new HashMap<>();
        for(Map.Entry<String,String> e:properties.entrySet()){String k=e.getKey();boolean side=isSideKey(k);out.put(side?rotateDirection(k,r):k,side?e.getValue():rotateProperty(r,k,e.getValue()));}
        return out;
    }
    private static boolean isSideKey(String k){for(String s:CLOCKWISE)if(s.equals(k))return true;return false;}
    static String rotateRailShape(String v,int r){
        if(v.startsWith("ascending_")){String d=v.substring(10);return isSideKey(d)?"ascending_"+rotateDirection(d,r):v;}
        int split=v.indexOf('_');if(split<=0)return v;
        String a=v.substring(0,split),b=v.substring(split+1);if(!isSideKey(a)||!isSideKey(b))return v;
        String ra=rotateDirection(a,r),rb=rotateDirection(b,r);
        boolean aNs=ra.equals("north")||ra.equals("south"),bNs=rb.equals("north")||rb.equals("south");
        if(aNs&&bNs)return "north_south";if(!aNs&&!bNs)return "east_west";
        return aNs?ra+"_"+rb:rb+"_"+ra;
    }
}
