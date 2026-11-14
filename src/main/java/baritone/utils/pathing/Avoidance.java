/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.utils.pathing;

import baritone.Baritone;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.IPlayerContext;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySpider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Avoidance {

    private final int centerX;
    private final int centerY;
    private final int centerZ;
    private final double coefficient;
    private final int radius;
    private final int radiusSq;

    public Avoidance(BetterBlockPos center, double coefficient, int radius) {
        this(center.x, center.y, center.z, coefficient, radius);
    }

    public Avoidance(int centerX, int centerY, int centerZ, double coefficient, int radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.coefficient = coefficient;
        this.radius = radius;
        this.radiusSq = radius * radius;
    }

    public double coefficient(int x, int y, int z) {
        int xDiff = x - centerX;
        int yDiff = y - centerY;
        int zDiff = z - centerZ;
        return xDiff * xDiff + yDiff * yDiff + zDiff * zDiff <= radiusSq ? coefficient : 1.0D;
    }

    public static List<Avoidance> create(IPlayerContext ctx) {
        if (!Baritone.settings().avoidance.value) {
            return Collections.emptyList();
        }
        List<Avoidance> res = new ArrayList<>();
        double mobSpawnerCoeff = Baritone.settings().mobSpawnerAvoidanceCoefficient.value;
        double mobCoeff = Baritone.settings().mobAvoidanceCoefficient.value;
        if (mobSpawnerCoeff != 1.0D && ctx.worldData() != null && ctx.worldData().getCachedWorld() != null) {
            ctx.worldData().getCachedWorld().getLocationsOf("mob_spawner", 1, ctx.playerFeet().x, ctx.playerFeet().z, 2)
                    .forEach(mobspawner -> res.add(new Avoidance(mobspawner, mobSpawnerCoeff, Baritone.settings().mobSpawnerAvoidanceRadius.value)));
        }
        if (mobCoeff != 1.0D) {
            ctx.entitiesStream()
                    .filter(entity -> entity instanceof EntityMob)
                    .filter(entity -> (!(entity instanceof EntitySpider)) || ctx.player().getBrightness(1.0F) < 0.5F)
                    .filter(entity -> !(entity instanceof EntityPigZombie) || ((EntityPigZombie) entity).getAITarget() != null)
                    .filter(entity -> !(entity instanceof EntityEnderman) || ((EntityEnderman) entity).isScreaming())
                    .forEach(entity -> res.add(new Avoidance((int) Math.floor(entity.posX), (int) Math.floor(entity.posY), (int) Math.floor(entity.posZ), mobCoeff, Baritone.settings().mobAvoidanceRadius.value)));
        }
        return res;
    }

    public void applySpherical(Long2DoubleOpenHashMap map) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z <= radius * radius) {
                        long hash = BetterBlockPos.longHash(centerX + x, centerY + y, centerZ + z);
                        map.put(hash, map.get(hash) * coefficient);
                    }
                }
            }
        }
    }
}
