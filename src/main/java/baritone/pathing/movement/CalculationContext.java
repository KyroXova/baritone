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

package baritone.pathing.movement;

import baritone.Baritone;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.movement.ActionCosts;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockState;
import baritone.cache.WorldData;
import baritone.pathing.precompute.PrecomputedData;
import baritone.utils.BlockStateInterface;
import baritone.utils.ToolSet;
import baritone.utils.pathing.BetterWorldBorder;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.world.World;
import net.minecraft.block.Block;

import java.util.ArrayList;
import java.util.List;

import static baritone.api.pathing.movement.ActionCosts.COST_INF;

/**
 * @author Brady
 * @since 8/7/2018
 */
public class CalculationContext {

    public final boolean safeForThreadedUse;
    public final IBaritone baritone;
    public final World world;
    public final WorldData worldData;
    public final BlockStateInterface bsi;
    public final ToolSet toolSet;
    public final boolean hasWaterBucket;
    public final boolean hasThrowaway;
    public final boolean canSprint;
    protected final double placeBlockCost; // protected because you should call the function instead
    public final boolean allowBreak;
    public final List<Block> allowBreakAnyway;
    public final boolean allowParkour;
    public final boolean allowParkourPlace;
    public final boolean allowJumpAtBuildLimit;
    public final boolean allowParkourAscend;
    public final boolean assumeWalkOnWater;
    public boolean allowFallIntoLava;
    public final int frostWalker;
    public final boolean allowDiagonalDescend;
    public final boolean allowDiagonalAscend;
    public final boolean allowDownward;
    public int minFallHeight;
    public int maxFallHeightNoWater;
    public final int maxFallHeightBucket;
    public final double waterWalkSpeed;
    public final double breakBlockAdditionalCost;
    public double backtrackCostFavoringCoefficient;
    public double jumpPenalty;
    public final double walkOnWaterOnePenalty;
    public final boolean allowWalkOnMagmaBlocks;
    public final BetterWorldBorder worldBorder;

    public final PrecomputedData precomputedData;
    private final GoalBlock protectedPlacementGoal;

    public CalculationContext(IBaritone baritone) {
        this(baritone, false);
    }

    public CalculationContext(IBaritone baritone, boolean forUseOnAnotherThread) {
        this(baritone, forUseOnAnotherThread, false);
    }

    public CalculationContext(IBaritone baritone, boolean forUseOnAnotherThread, boolean forbidBreaking) {
        this(baritone, forUseOnAnotherThread, forbidBreaking, false);
    }

    public CalculationContext(IBaritone baritone, boolean forUseOnAnotherThread, boolean forbidBreaking, boolean allowPlacement) {
        this(baritone, forUseOnAnotherThread, forbidBreaking, allowPlacement, null);
    }

    public CalculationContext(IBaritone baritone, boolean forUseOnAnotherThread, boolean forbidBreaking, boolean allowPlacement, GoalBlock protectedPlacementGoal) {
        this.protectedPlacementGoal = protectedPlacementGoal;
        this.precomputedData = new PrecomputedData();
        this.safeForThreadedUse = forUseOnAnotherThread;
        this.baritone = baritone;
        EntityClientPlayerMP player = baritone.getPlayerContext().player();
        this.world = baritone.getPlayerContext().world();
        this.worldData = (WorldData) baritone.getPlayerContext().worldData();
        this.bsi = new BlockStateInterface(baritone.getPlayerContext(), forUseOnAnotherThread);
        this.toolSet = new ToolSet(player);
        this.hasThrowaway = (!forbidBreaking || allowPlacement) && Baritone.settings().allowPlace.value && ((Baritone) baritone).getInventoryBehavior().hasGenericThrowaway();
        boolean waterBucketOnHotbar = false;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.inventory.mainInventory[slot];
            if (stack != null && stack.getItem() == Items.water_bucket) {
                waterBucketOnHotbar = true;
                break;
            }
        }
        this.hasWaterBucket = !forbidBreaking && Baritone.settings().allowWaterBucketFall.value && waterBucketOnHotbar && !world.provider.isHellWorld;
        this.canSprint = Baritone.settings().allowSprint.value && player.getFoodStats().getFoodLevel() > 6;
        this.placeBlockCost = Baritone.settings().blockPlacementPenalty.value;
        this.allowBreak = !forbidBreaking && Baritone.settings().allowBreak.value;
        this.allowBreakAnyway = forbidBreaking ? new ArrayList<>() : new ArrayList<>(Baritone.settings().allowBreakAnyway.value);
        this.allowParkour = Baritone.settings().allowParkour.value;
        this.allowParkourPlace = (!forbidBreaking || allowPlacement) && Baritone.settings().allowParkourPlace.value;
        this.allowJumpAtBuildLimit = Baritone.settings().allowJumpAtBuildLimit.value;
        this.allowParkourAscend = Baritone.settings().allowParkourAscend.value;
        this.assumeWalkOnWater = Baritone.settings().assumeWalkOnWater.value;
        this.allowFallIntoLava = false; // Super secret internal setting for ElytraBehavior
        this.frostWalker = 0; // Frost Walker was introduced after 1.7.10.
        this.allowDiagonalDescend = Baritone.settings().allowDiagonalDescend.value;
        this.allowDiagonalAscend = Baritone.settings().allowDiagonalAscend.value;
        this.allowDownward = Baritone.settings().allowDownward.value;
        this.minFallHeight = 3; // Minimum fall height used by MovementFall
        this.maxFallHeightNoWater = Baritone.settings().maxFallHeightNoWater.value;
        this.maxFallHeightBucket = Baritone.settings().maxFallHeightBucket.value;
        this.waterWalkSpeed = ActionCosts.WALK_ONE_IN_WATER_COST;
        this.breakBlockAdditionalCost = Baritone.settings().blockBreakAdditionalPenalty.value;
        this.backtrackCostFavoringCoefficient = Baritone.settings().backtrackCostFavoringCoefficient.value;
        this.jumpPenalty = Baritone.settings().jumpPenalty.value;
        this.walkOnWaterOnePenalty = Baritone.settings().walkOnWaterOnePenalty.value;
        this.allowWalkOnMagmaBlocks = Baritone.settings().allowWalkOnMagmaBlocks.value;
        // why cache these things here, why not let the movements just get directly from settings?
        // because if some movements are calculated one way and others are calculated another way,
        // then you get a wildly inconsistent path that isn't optimal for either scenario.
        this.worldBorder = new BetterWorldBorder();
    }

    public final IBaritone getBaritone() {
        return baritone;
    }

    public BlockState get(int x, int y, int z) {
        return bsi.get0(x, y, z); // laughs maniacally
    }

    public boolean isLoaded(int x, int z) {
        return bsi.isLoaded(x, z);
    }

    public BlockState get(BetterBlockPos pos) {
        return get(pos.getX(), pos.getY(), pos.getZ());
    }

    public Block getBlock(int x, int y, int z) {
        return get(x, y, z).getBlock();
    }

    public double costOfPlacingAt(int x, int y, int z, BlockState current) {
        if (!hasThrowaway) { // only true if allowPlace is true, see constructor
            return COST_INF;
        }
        if (protectedPlacementGoal != null && Math.abs(x - protectedPlacementGoal.x) <= 8
                && Math.abs(z - protectedPlacementGoal.z) <= 8 && y >= protectedPlacementGoal.y - 1) {
            return COST_INF;
        }
        if (isPossiblyProtected(x, y, z)) {
            return COST_INF;
        }
        if (!worldBorder.canPlaceAt(x, z)) {
            return COST_INF;
        }
        boolean liquid = current.getBlock().getMaterial().isLiquid();
        if (!Baritone.settings().allowPlaceInFluidsSource.value && liquid && current.getMetadata() == 0) {
            return COST_INF;
        }
        if (!Baritone.settings().allowPlaceInFluidsFlow.value && liquid && current.getMetadata() != 0) {
            return COST_INF;
        }
        return placeBlockCost;
    }

    public double breakCostMultiplierAt(int x, int y, int z, BlockState current) {
        if (!allowBreak && !allowBreakAnyway.contains(current.getBlock())) {
            return COST_INF;
        }
        if (isPossiblyProtected(x, y, z)) {
            return COST_INF;
        }
        return 1;
    }

    public double placeBucketCost() {
        return placeBlockCost; // shrug
    }

    public boolean isPossiblyProtected(int x, int y, int z) {
        // TODO more protection logic here; see #220
        return false;
    }
}
