/* This file is part of Baritone, distributed under the LGPL-3.0-or-later. */
package baritone.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

/** The interpolated camera origin used by the current world-render pass. */
public final class WorldRenderOrigin {
    private static double x;
    private static double y;
    private static double z;

    private WorldRenderOrigin() {}

    public static void update(float partialTicks) {
        Entity camera = Minecraft.getMinecraft().renderViewEntity;
        if (camera == null) return;
        x = interpolate(camera.lastTickPosX, camera.posX, partialTicks);
        y = interpolate(camera.lastTickPosY, camera.posY, partialTicks);
        z = interpolate(camera.lastTickPosZ, camera.posZ, partialTicks);
    }

    static double interpolate(double previous, double current, float partialTicks) {
        return previous + (current - previous) * partialTicks;
    }

    public static double x() { return x; }
    public static double y() { return y; }
    public static double z() { return z; }
}
