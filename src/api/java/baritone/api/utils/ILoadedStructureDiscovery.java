/* This file is part of Baritone, distributed under the LGPL-3.0-or-later. */
package baritone.api.utils;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.tileentity.TileEntity;

/** Finds caller-defined structure markers in currently loaded client chunks. */
public interface ILoadedStructureDiscovery {
    /**
     * Returns positions of matching, valid loaded tile entities within radius of the player.
     * Must be called on the client thread with a world loaded. Unloaded chunks are not searched.
     * The predicate runs on the client thread and must not mutate the world or the entity list.
     */
    List<BetterBlockPos> findTileEntities(Predicate<? super TileEntity> predicate, double radius, int limit);
}
