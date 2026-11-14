/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.api.event.events;

import baritone.api.utils.Pair;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockState;
import net.minecraft.world.ChunkCoordIntPair;

import java.util.List;

/**
 * @author Brady
 */
public final class BlockChangeEvent {

    private final ChunkCoordIntPair chunk;
    private final List<Pair<BetterBlockPos, BlockState>> blocks;

    public BlockChangeEvent(ChunkCoordIntPair pos, List<Pair<BetterBlockPos, BlockState>> blocks) {
        this.chunk = pos;
        this.blocks = blocks;
    }

    public ChunkCoordIntPair getChunkPos() {
        return this.chunk;
    }

    public List<Pair<BetterBlockPos, BlockState>> getBlocks() {
        return this.blocks;
    }
}
