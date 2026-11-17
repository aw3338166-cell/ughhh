package com.austinwing.mcaibuild.redstone;
import static org.junit.jupiter.api.Assertions.*;import org.junit.jupiter.api.Test;
class BearingMathTest {
 @Test void relativeAngles(){assertEquals(0,BearingMath.relativeBearingDegrees(0,10,0),1e-6);assertEquals(-90,BearingMath.relativeBearingDegrees(10,0,0),1e-6);}
 @Test void compassAxes(){assertEquals("S",BearingMath.compass(0,5));assertEquals("E",BearingMath.compass(5,0));assertEquals("N",BearingMath.compass(0,-5));assertEquals("W",BearingMath.compass(-5,0));}
 @Test void wrapping(){assertEquals(-170,BearingMath.wrapDegrees(190),1e-6);assertEquals(170,BearingMath.wrapDegrees(-190),1e-6);assertEquals(-180,BearingMath.wrapDegrees(180),1e-6);}
}
