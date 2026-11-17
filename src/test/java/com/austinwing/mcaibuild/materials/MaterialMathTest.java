package com.austinwing.mcaibuild.materials;
import static org.junit.jupiter.api.Assertions.*;import java.util.Map;import org.junit.jupiter.api.Test;
class MaterialMathTest {
 @Test void ordinaryBlock(){assertEquals(1,MaterialMath.itemCount("minecraft:stone",Map.of()));}
 @Test void multiblockHalves(){assertEquals(0,MaterialMath.itemCount("minecraft:oak_door",Map.of("half","upper")));assertEquals(0,MaterialMath.itemCount("minecraft:red_bed",Map.of("part","head")));}
 @Test void stateCounts(){assertEquals(2,MaterialMath.itemCount("minecraft:stone_slab",Map.of("type","double")));assertEquals(3,MaterialMath.itemCount("minecraft:snow",Map.of("layers","3")));assertEquals(4,MaterialMath.itemCount("minecraft:white_candle",Map.of("candles","4")));assertEquals(2,MaterialMath.itemCount("minecraft:sea_pickle",Map.of("pickles","2")));}
}
