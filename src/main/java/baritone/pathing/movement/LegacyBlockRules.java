/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.pathing.movement;

import baritone.api.utils.BlockState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;

/** Block and metadata rules shared by path calculation and cached chunks. */
public final class LegacyBlockRules {
    private LegacyBlockRules() {}

    public static boolean isWater(BlockState state) {
        return state.getBlock().getMaterial() == Material.water;
    }

    public static boolean possiblyFlowing(BlockState state) {
        Material material = state.getBlock().getMaterial();
        return (material == Material.water || material == Material.lava) && state.getMetadata() != 0;
    }

    public static boolean avoidWalkingInto(BlockState state) {
        Block block = state.getBlock();
        return block.getMaterial().isLiquid() || block == Blocks.cactus || block instanceof BlockFire
                || block == Blocks.end_portal || block == Blocks.web;
    }

    public static boolean isBottomSlab(BlockState state) {
        Block block = state.getBlock();
        return block instanceof BlockSlab && !block.isOpaqueCube() && (state.getMetadata() & 8) == 0;
    }
}
