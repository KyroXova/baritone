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

package baritone.utils.schematic.schematica;

import baritone.api.schematic.IStaticSchematic;
import baritone.api.utils.BlockState;
import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.util.List;

public final class SchematicAdapter implements IStaticSchematic {

    private final SchematicWorld schematic;

    public SchematicAdapter(SchematicWorld schematicWorld) {
        this.schematic = schematicWorld;
    }

    @Override
    public BlockState desiredState(int x, int y, int z, BlockState current, List<BlockState> approxPlaceable) {
        return this.getDirect(x, y, z);
    }

    @Override
    public BlockState getDirect(int x, int y, int z) {
        ISchematic s = this.schematic.getSchematic();
        if (s == null) {
            return new BlockState(Blocks.air, 0);
        }
        Block block = s.getBlock(x, y, z);
        if (block == null) {
            block = Blocks.air;
        }
        int meta = s.getBlockMetadata(x, y, z);
        return new BlockState(block, meta);
    }

    @Override
    public int widthX() {
        return schematic.getSchematic().getWidth();
    }

    @Override
    public int heightY() {
        return schematic.getSchematic().getHeight();
    }

    @Override
    public int lengthZ() {
        return schematic.getSchematic().getLength();
    }
}
