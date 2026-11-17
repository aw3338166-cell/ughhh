package com.austinwing.mcaibuild.materials;
import java.util.Map;
public final class MaterialMath {
    private MaterialMath(){}
    public static int itemCount(String blockId,Map<String,String> properties){
        if(blockId==null)return 0;Map<String,String> p=properties==null?Map.of():properties;
        String path=blockId.substring(blockId.indexOf(':')+1);
        if("upper".equals(p.get("half")))return 0;
        if(path.endsWith("_bed")&&"head".equals(p.get("part")))return 0;
        if(path.endsWith("_slab")&&"double".equals(p.get("type")))return 2;
        if(path.equals("snow"))return positive(p.get("layers"),1);
        if(path.equals("candle")||path.endsWith("_candle"))return positive(p.get("candles"),1);
        if(path.equals("sea_pickle"))return positive(p.get("pickles"),1);
        if(path.equals("turtle_egg"))return positive(p.get("eggs"),1);
        return 1;
    }
    private static int positive(String v,int fallback){if(v==null)return fallback;try{int n=Integer.parseInt(v);return n>=1?n:fallback;}catch(NumberFormatException e){return fallback;}}
}
