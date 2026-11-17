/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.pathing.movement;

import baritone.Baritone;
import baritone.api.utils.BlockState;
import baritone.pathing.precompute.Ternary;
import baritone.utils.BlockStateInterface;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;

import static baritone.pathing.precompute.Ternary.*;

/** Movement classifications for the block and metadata model used by 1.7.10. */
public final class LegacyMovementRules {
    private LegacyMovementRules() {}

    public static Ternary canWalkThroughBlockState(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.air) return YES;
        if (LegacyBlockRules.isTerrainHazard(state)) return NO;
        if (LegacyBlockRules.avoidWalkingInto(state) && !LegacyBlockRules.isWater(state)) return NO;
        if (Baritone.settings().blocksToAvoid.value.contains(block)
                || block instanceof BlockSlab || block instanceof BlockTrapDoor
                || block == Blocks.cocoa || block == Blocks.skull || block == Blocks.cauldron) return NO;
        if (block instanceof BlockDoor) return block == Blocks.iron_door ? NO : YES;
        if (block instanceof BlockFenceGate) return YES;
        if (LegacyBlockRules.isWater(state) && state.getMetadata() != 0) return NO;
        // Modded blocks and several vanilla blocks depend on their position or neighbours.
        return MAYBE;
    }

    public static boolean canWalkThroughPosition(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.carpet || block == Blocks.snow_layer) {
            if (block == Blocks.snow_layer && state.getMetadata() >= 2) return false;
            return canWalkOn(bsi, x, y - 1, z, bsi.get0(x, y - 1, z));
        }
        if (LegacyBlockRules.isWater(state)) {
            if (isFlowing(bsi, x, y, z, state) || Baritone.settings().assumeWalkOnWater.value) return false;
            BlockState above = bsi.get0(x, y + 1, z);
            return !above.getBlock().getMaterial().isLiquid();
        }
        return block.getBlocksMovement(bsi.access, x, y, z);
    }

    public static Ternary fullyPassableBlockState(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.air) return YES;
        if (LegacyBlockRules.avoidWalkingInto(state) || block == Blocks.tripwire
                || block == Blocks.vine || block == Blocks.ladder || block == Blocks.cocoa
                || block == Blocks.snow_layer || block == Blocks.skull
                || block instanceof BlockDoor || block instanceof BlockFenceGate
                || block instanceof BlockTrapDoor) return NO;
        return MAYBE;
    }

    public static boolean fullyPassablePosition(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
        return state.getBlock().getBlocksMovement(bsi.access, x, y, z);
    }

    public static Ternary canWalkOnBlockState(BlockState state) {
        Block block = state.getBlock();
        if (LegacyBlockRules.isTerrainHazard(state)) return NO;
        if (LegacyBlockRules.isWater(state)) return MAYBE;
        if (block.getMaterial() == Material.lava) return Baritone.settings().assumeWalkOnLava.value ? MAYBE : NO;
        if (block == Blocks.ladder || (block == Blocks.vine && Baritone.settings().allowVines.value)
                || block == Blocks.farmland || block == Blocks.soul_sand
                || block == Blocks.chest || block == Blocks.trapped_chest || block == Blocks.ender_chest
                || block == Blocks.glass || block == Blocks.stained_glass || block instanceof BlockStairs) return YES;
        if (block instanceof BlockSlab) {
            return !LegacyBlockRules.isBottomSlab(state) || Baritone.settings().allowWalkOnBottomSlab.value ? YES : NO;
        }
        return MAYBE;
    }

    public static boolean canWalkOnPosition(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
        if (LegacyBlockRules.isWater(state)) {
            BlockState above = bsi.get0(x, y + 1, z);
            if (above.getBlock() == Blocks.carpet) return true;
            if (isFlowing(bsi, x, y, z, state)
                    || (LegacyBlockRules.isWater(above) && LegacyBlockRules.possiblyFlowing(above))) return false;
            return LegacyBlockRules.isWater(above) ^ Baritone.settings().assumeWalkOnWater.value;
        }
        if (state.getBlock().getMaterial() == Material.lava) {
            return Baritone.settings().assumeWalkOnLava.value && !isFlowing(bsi, x, y, z, state);
        }
        return state.getBlock().isNormalCube(bsi.access, x, y, z);
    }

    private static boolean canWalkOn(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
        Ternary result = canWalkOnBlockState(state);
        return result == YES || (result == MAYBE && canWalkOnPosition(bsi, x, y, z, state));
    }

    private static boolean isFlowing(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
        return LegacyBlockRules.possiblyFlowing(state)
                || LegacyBlockRules.possiblyFlowing(bsi.get0(x + 1, y, z))
                || LegacyBlockRules.possiblyFlowing(bsi.get0(x - 1, y, z))
                || LegacyBlockRules.possiblyFlowing(bsi.get0(x, y, z + 1))
                || LegacyBlockRules.possiblyFlowing(bsi.get0(x, y, z - 1));
    }
}
