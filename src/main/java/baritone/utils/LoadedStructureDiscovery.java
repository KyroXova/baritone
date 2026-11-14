/* This file is part of Baritone, distributed under the LGPL-3.0-or-later. */
package baritone.utils;

import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.ILoadedStructureDiscovery;
import baritone.api.utils.IPlayerContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public final class LoadedStructureDiscovery implements ILoadedStructureDiscovery {
    private final IPlayerContext ctx;

    public LoadedStructureDiscovery(IPlayerContext ctx) { this.ctx = ctx; }

    @Override
    public List<BetterBlockPos> findTileEntities(Predicate<? super TileEntity> predicate, double radius, int limit) {
        Objects.requireNonNull(predicate, "predicate");
        if (!Double.isFinite(radius) || radius < 0 || limit < 0) throw new IllegalArgumentException("Invalid radius or limit");
        if (!ctx.minecraft().func_152345_ab()) throw new IllegalStateException("Must run on the client thread");
        EntityClientPlayerMP player = ctx.player();
        World world = ctx.world();
        if (player == null || world == null) throw new IllegalStateException("No world is loaded");
        List<BetterBlockPos> found = new ArrayList<>();
        if (limit == 0) return found;
        double radiusSquared = radius * radius;
        BlockStateInterface blocks = new BlockStateInterface(ctx);
        for (Object entry : new ArrayList<>(world.loadedTileEntityList)) {
            if (!(entry instanceof TileEntity)) continue;
            TileEntity tile = (TileEntity) entry;
            if (tile.isInvalid() || tile.getWorldObj() != world || !blocks.worldContainsLoadedChunk(tile.xCoord, tile.zCoord)) continue;
            double x = tile.xCoord + 0.5 - player.posX, y = tile.yCoord + 0.5 - player.posY, z = tile.zCoord + 0.5 - player.posZ;
            if (x * x + y * y + z * z <= radiusSquared && predicate.test(tile)) found.add(new BetterBlockPos(tile.xCoord, tile.yCoord, tile.zCoord));
        }
        found.sort(Comparator.comparingDouble(pos -> {
            double x = pos.x + 0.5 - player.posX, y = pos.y + 0.5 - player.posY, z = pos.z + 0.5 - player.posZ;
            return x * x + y * y + z * z;
        }));
        return found.size() <= limit ? found : new ArrayList<>(found.subList(0, limit));
    }
}
