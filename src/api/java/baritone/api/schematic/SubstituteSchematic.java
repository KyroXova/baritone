/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.api.schematic;

import baritone.api.utils.BlockState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.init.Blocks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubstituteSchematic extends AbstractSchematic {

    private final ISchematic schematic;
    private final Map<Block, List<Block>> substitutions;
    private final Map<BlockState, Map<Block, BlockState>> blockStateCache = new HashMap<>();

    public SubstituteSchematic(ISchematic schematic, Map<Block, List<Block>> substitutions) {
        super(schematic.widthX(), schematic.heightY(), schematic.lengthZ());
        this.schematic = schematic;
        this.substitutions = substitutions;
    }

    @Override
    public boolean inSchematic(int x, int y, int z, BlockState currentState) {
        return schematic.inSchematic(x, y, z, currentState);
    }

    @Override
    public BlockState desiredState(int x, int y, int z, BlockState current, List<BlockState> approxPlaceable) {
        BlockState desired = schematic.desiredState(x, y, z, current, approxPlaceable);
        if (desired == null) {
            return null;
        }
        Block desiredBlock = desired.getBlock();
        if (!substitutions.containsKey(desiredBlock)) {
            return desired;
        }
        List<Block> substitutes = substitutions.get(desiredBlock);
        if (current != null && substitutes.contains(current.getBlock()) && !(current.getBlock() instanceof BlockAir)) {
            return withBlock(desired, current.getBlock());
        }
        for (Block substitute : substitutes) {
            if (substitute instanceof BlockAir) {
                return (current != null && current.getBlock() instanceof BlockAir) ? current : new BlockState(Blocks.air, 0);
            }
            for (BlockState placeable : approxPlaceable) {
                if (substitute.equals(placeable.getBlock())) {
                    return withBlock(desired, placeable.getBlock());
                }
            }
        }
        return withBlock(desired, substitutes.get(0));
    }

    private BlockState withBlock(BlockState state, Block block) {
        if (blockStateCache.containsKey(state) && blockStateCache.get(state).containsKey(block)) {
            return blockStateCache.get(state).get(block);
        }
        BlockState newState = new BlockState(block, state.getMetadata());
        blockStateCache.computeIfAbsent(state, s -> new HashMap<>()).put(block, newState);
        return newState;
    }
}
