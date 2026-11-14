/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.utils;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

/** Exposes the pathing block view to legacy block queries. */
public final class BlockStateInterfaceAccessWrapper implements IBlockAccess {
    private final BlockStateInterface bsi;

    BlockStateInterfaceAccessWrapper(BlockStateInterface bsi) {
        this.bsi = bsi;
    }

    @Override public Block getBlock(int x, int y, int z) { return bsi.get0(x, y, z).getBlock(); }
    @Override public int getBlockMetadata(int x, int y, int z) { return bsi.get0(x, y, z).getMetadata(); }
    @Override public TileEntity getTileEntity(int x, int y, int z) { return null; }
    @Override public int getLightBrightnessForSkyBlocks(int x, int y, int z, int minimum) {
        return bsi.world.getLightBrightnessForSkyBlocks(x, y, z, minimum);
    }
    @Override public int isBlockProvidingPowerTo(int x, int y, int z, int side) {
        return getBlock(x, y, z).isProvidingWeakPower(this, x, y, z, side);
    }
    @Override public boolean isAirBlock(int x, int y, int z) { return getBlock(x, y, z).isAir(this, x, y, z); }
    @Override public BiomeGenBase getBiomeGenForCoords(int x, int z) { return bsi.world.getBiomeGenForCoords(x, z); }
    @Override public int getHeight() { return 256; }
    @Override public boolean extendedLevelsInChunkCache() { return false; }
    @Override public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean defaultValue) {
        if (y < 0 || y >= 256 || !bsi.isLoaded(x, z)) return defaultValue;
        return getBlock(x, y, z).isSideSolid(this, x, y, z, side);
    }
}
