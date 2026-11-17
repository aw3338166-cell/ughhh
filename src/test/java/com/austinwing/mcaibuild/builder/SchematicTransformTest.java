package com.austinwing.mcaibuild.builder;
import static org.junit.jupiter.api.Assertions.*;import java.util.Map;import org.junit.jupiter.api.Test;
class SchematicTransformTest {
 @Test void rectangularFootprint(){int w=5,l=3;for(int r=0;r<4;r++){int rw=SchematicTransform.rotatedWidth(r,w,l),rl=SchematicTransform.rotatedLength(r,w,l);boolean[][] seen=new boolean[rw][rl];for(int x=0;x<w;x++)for(int z=0;z<l;z++){int rx=SchematicTransform.rotatedX(r,w,l,x,z),rz=SchematicTransform.rotatedZ(r,w,l,x,z);assertTrue(rx>=0&&rx<rw&&rz>=0&&rz<rl);assertFalse(seen[rx][rz]);seen[rx][rz]=true;}}}
 @Test void directionsAndAxes(){assertEquals("east",SchematicTransform.rotateProperty(1,"facing","north"));assertEquals("up",SchematicTransform.rotateProperty(2,"facing","up"));assertEquals("z",SchematicTransform.rotateProperty(1,"axis","x"));assertEquals("4",SchematicTransform.rotateProperty(1,"rotation","0"));}
 @Test void stairsAndRails(){assertEquals("inner_left",SchematicTransform.rotateProperty(1,"shape","inner_left"));assertEquals("east_west",SchematicTransform.rotateProperty(1,"shape","north_south"));assertEquals("ascending_east",SchematicTransform.rotateProperty(1,"shape","ascending_north"));}
 @Test void sideProperties(){Map<String,String> p=Map.of("north","true","east","false","south","false","west","false","waterlogged","false");Map<String,String> q=SchematicTransform.rotateProperties(1,p);assertEquals("true",q.get("east"));assertEquals("false",q.get("north"));assertEquals(5,q.size());}
}
