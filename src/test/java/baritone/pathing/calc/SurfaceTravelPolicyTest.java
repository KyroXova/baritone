package baritone.pathing.calc;

import org.junit.Test;
import static org.junit.Assert.*;

public class SurfaceTravelPolicyTest {
    @Test
    public void rejectsDeepOrUnloadedColumns() {
        assertTrue(SurfaceTravelPolicy.allowed(70, 64));
        assertFalse(SurfaceTravelPolicy.allowed(70, 63));
        assertFalse(SurfaceTravelPolicy.allowed(-1, 64));
    }
}
