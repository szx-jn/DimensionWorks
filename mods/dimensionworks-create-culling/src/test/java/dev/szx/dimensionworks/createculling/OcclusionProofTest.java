package dev.szx.dimensionworks.createculling;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OcclusionProofTest {
    private final OcclusionProof.Box box = new OcclusionProof.Box(0, 0, 0, 1, 1, 1);
    private record Cell(int x, int y, int z) {}

    @Test void openAirAndCameraInsideAreVisible() {
        assertFalse(OcclusionProof.hidden(box, .5, .5, 10, (x,y,z) -> false));
        assertFalse(OcclusionProof.hidden(box, .5, .5, .5, (x,y,z) -> true));
    }
    @Test void solidWallsHideFromAllSixDirections() {
        for (int axis = 0; axis < 3; axis++) for (int side : new int[]{-1, 1}) {
            final int a = axis, depth = side > 0 ? 3 : -3;
            double[] camera = {.5,.5,.5}; camera[axis] = side * 10;
            assertTrue(OcclusionProof.hidden(box, camera[0], camera[1], camera[2],
                (x,y,z) -> (a == 0 ? x : a == 1 ? y : z) == depth));
        }
    }
    @Test void centralHoleMustStayVisibleEvenWhenAllCornersAreBlocked() {
        var large = new OcclusionProof.Box(-2, -2, 0, 3, 3, 1);
        assertFalse(OcclusionProof.hidden(large, .5,.5,10,
            (x,y,z) -> z == 3 && (x != 0 || y != 0)));
    }
    @Test void edgeOfLargeWheelMustStayVisibleBehindSmallCover() {
        var wheel = new OcclusionProof.Box(-2,-2,-2,3,3,3);
        assertFalse(OcclusionProof.hidden(wheel,.5,.5,10,(x,y,z) -> z==4 && x==0 && y==0));
    }
    @Test void wallBehindTargetOrBehindCameraDoesNotHide() {
        assertFalse(OcclusionProof.hidden(box,.5,.5,10,(x,y,z) -> z==-3 || z==12));
    }
    @Test void completeShellHidesButMissingFaceDoesNot() {
        assertTrue(OcclusionProof.hidden(box,10,10,10,(x,y,z) ->
            x == -1 || x == 1 || y == -1 || y == 1 || z == -1 || z == 1));
        assertFalse(OcclusionProof.hidden(box,.5,.5,10,(x,y,z) ->
            (x == -1 || x == 1 || y == -1 || y == 1 || z == -1) && z <= 0));
    }
    @Test void unknownOrTransparentCellsAreVisible() {
        assertFalse(OcclusionProof.hidden(box,.5,.5,10,(x,y,z) -> false));
    }
    @Test void invalidOrUnboundedBoxesFailOpen() {
        for (var invalid : new OcclusionProof.Box[]{
            new OcclusionProof.Box(0,0,0,Double.POSITIVE_INFINITY,1,1),
            new OcclusionProof.Box(0,0,0,100,100,100),
            new OcclusionProof.Box(1,1,1,0,0,0)}) {
            assertFalse(OcclusionProof.hidden(invalid,.5,.5,10,(x,y,z) -> true));
        }
        assertFalse(OcclusionProof.hidden(box,Double.NaN,0,10,(x,y,z)->true));
    }
    @Test void negativeCoordinatesWork() {
        assertTrue(OcclusionProof.hidden(new OcclusionProof.Box(-20,-20,-20,-19,-19,-19),
            -19.5,-19.5,-10,(x,y,z) -> z==-15));
    }
    @Test void cameraWithinEnclosureInteriorMustNotBeHiddenByOuterShell() {
        var fractional = new OcclusionProof.Box(.25,.25,.25,1.5,1.5,1.5);
        assertFalse(OcclusionProof.hidden(fractional,1.75,.75,.75,(x,y,z) ->
            x == -1 || x == 2 || y == -1 || y == 2 || z == -1 || z == 2));
    }

    @Test void randomizedProofsAgreeWithIndependentRayBoxOracle() {
        Random random = new Random(420231);
        int hidden = 0;
        for (int scene = 0; scene < 300; scene++) {
            Set<Cell> cells = new HashSet<>();
            for (int x=-4; x<=4; x++) for (int y=-4; y<=4; y++) {
                if (random.nextDouble() < (scene % 2 == 0 ? 1 : .9)) cells.add(new Cell(x,y,3));
            }
            double cx = random.nextDouble()*12-6, cy = random.nextDouble()*12-6, cz = 8;
            if (!OcclusionProof.hidden(box,cx,cy,cz,(x,y,z)->cells.contains(new Cell(x,y,z)))) continue;
            hidden++;
            for (int x=0; x<=10; x++) for (int y=0; y<=10; y++) {
                double tx=x/10., ty=y/10.;
                assertTrue(cells.stream().anyMatch(cell -> intersects(cx,cy,cz,tx,ty,1,cell)),
                    "Culled target has an unblocked ray at " + tx + "," + ty);
            }
        }
        assertTrue(hidden > 50, "Oracle test must exercise positive culling results");
    }

    // Segment/AABB intersection is deliberately independent from the projection proof.
    private static boolean intersects(double x,double y,double z,double tx,double ty,double tz,Cell cell) {
        double[] start={x,y,z}, end={tx,ty,tz}, low={cell.x,cell.y,cell.z};
        double enter=0, exit=1;
        for(int a=0;a<3;a++) {
            double delta=end[a]-start[a];
            if(Math.abs(delta)<1.e-12) { if(start[a]<low[a]||start[a]>low[a]+1) return false; }
            else {
                double t1=(low[a]-start[a])/delta, t2=(low[a]+1-start[a])/delta;
                enter=Math.max(enter,Math.min(t1,t2)); exit=Math.min(exit,Math.max(t1,t2));
                if(enter>exit) return false;
            }
        }
        return enter<exit;
    }
}
