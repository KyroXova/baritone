package baritone.pathing.calc;

final class SurfaceTravelPolicy {
    private SurfaceTravelPolicy() {}

    static boolean allowed(int surface, int feetY) {
        return surface >= 0 && surface - feetY <= 6;
    }
}
