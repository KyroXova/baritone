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
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Block and metadata rules shared by path calculation and cached chunks. */
public final class LegacyBlockRules {
    private static final ConcurrentMap<Block, Boolean> HAZARDS = new ConcurrentHashMap<>();
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
        return isTerrainHazard(state) || block.getMaterial().isLiquid() || block == Blocks.cactus || block instanceof BlockFire
                || block == Blocks.end_portal || block == Blocks.web;
    }

    public static boolean isTerrainHazard(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.air) return false;
        if (block == Blocks.cactus || block instanceof BlockFire) return true;
        String type = block.getClass().getName();
        int metadata = state.getMetadata();
        Boolean specific = metadataHazard(type, metadata);
        if (specific != null) return specific;
        return HAZARDS.computeIfAbsent(block, b -> hazardName((b.getClass().getName() + " "
                + String.valueOf(Block.blockRegistry.getNameForObject(b))).toLowerCase(Locale.ROOT)));
    }

    static Boolean metadataHazard(String type, int metadata) {
        if ("biomesoplenty.common.blocks.BlockMud".equals(type)) return metadata == 0 || metadata == 1;
        if ("biomesoplenty.common.blocks.BlockBOPPlant".equals(type)) return metadata == 5 || metadata == 12;
        if ("biomesoplenty.common.blocks.BlockBOPFlower".equals(type)) return metadata == 2;
        if ("biomesoplenty.common.blocks.BlockBOPFoliage".equals(type)) return metadata == 7;
        return null;
    }

    static boolean hazardName(String name) {
        return name.contains("quicksand") || name.contains("berrybush") || name.contains("thorn")
                || name.contains("bramble") || name.contains("spike") || name.contains("beartrap")
                || name.contains("punji") || name.contains("witherrose") || name.contains("saguaroblock")
                || name.contains("heatsand") || name.contains("blocktfhedge") || name.contains("blocktfknightmetal")
                || name.contains("blocktaintfibres") || name.contains("blockfluxgas") || name.contains("blockfluxgoo")
                || name.contains("blockfluiddeath") || name.contains("blockpoisonfluid")
                || name.contains("blockwitchcactus") || name.contains("blocklaserbeam");
    }

    public static boolean isBottomSlab(BlockState state) {
        Block block = state.getBlock();
        return block instanceof BlockSlab && !block.isOpaqueCube() && (state.getMetadata() & 8) == 0;
    }
}
