package com.austinwing.mcaibuild.schematic;
import static org.junit.jupiter.api.Assertions.*;import org.junit.jupiter.api.Test;
class LitematicBitArrayTest {
 @Test void widths(){assertEquals(2,LitematicBitArray.bitsFor(1));assertEquals(2,LitematicBitArray.bitsFor(4));assertEquals(3,LitematicBitArray.bitsFor(5));assertEquals(4,LitematicBitArray.bitsFor(9));}
 @Test void crossingEntry(){assertEquals(22,LitematicBitArray.get(new long[]{6L<<60,1L},12,5));}
 @Test void roundTrip(){for(int b=2;b<=12;b++){int[] v=new int[173];for(int i=0;i<v.length;i++)v[i]=(i*31+7)%(1<<b);long[] p=LitematicBitArray.pack(v,b);for(int i=0;i<v.length;i++)assertEquals(v[i],LitematicBitArray.get(p,i,b));}}
 @Test void invalidReads(){assertEquals(-1,LitematicBitArray.get(null,0,3));assertEquals(-1,LitematicBitArray.get(new long[]{0},100,3));assertEquals(-1,LitematicBitArray.get(new long[]{0},0,0));}
}
