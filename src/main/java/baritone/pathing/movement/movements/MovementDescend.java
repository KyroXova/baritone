/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.pathing.movement.movements;

import baritone.api.IBaritone;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockState;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.BlockStateInterface;
import baritone.utils.pathing.MutableMoveResult;
import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.init.Blocks;
import net.minecraft.util.Vec3;

import java.util.Set;

public class MovementDescend extends Movement {

    private int numTicks = 0;
    public boolean forceSafeMode = false;

    public MovementDescend(IBaritone baritone, BetterBlockPos start, BetterBlockPos end) {
        super(baritone, start, end, new BetterBlockPos[]{end.above(2), end.above(), end}, end.below());
    }

    @Override
    public void reset() {
        super.reset();
        numTicks = 0;
        forceSafeMode = false;
    }

    /**
     * Called by PathExecutor if needing safeMode can only be detected with knowledge about the next movement
     */
    public void forceSafeMode() {
        forceSafeMode = true;
    }

    @Override
    public double calculateCost(CalculationContext context) {
        MutableMoveResult result = new MutableMoveResult();
        cost(context, src.x, src.y, src.z, dest.x, dest.z, result);
        if (result.y != dest.y) {
            return COST_INF; // doesn't apply to us, this position is a fall not a descend
        }
        return result.cost;
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        return ImmutableSet.of(src, dest.above(), dest);
    }

    public static void cost(CalculationContext context, int x, int y, int z, int destX, int destZ, MutableMoveResult res) {
        double totalCost = 0;
        BlockState destDown = context.get(destX, y - 1, destZ);
        totalCost += MovementHelper.getMiningDurationTicks(context, destX, y - 1, destZ, destDown, false);
        if (totalCost >= COST_INF) {
            return;
        }
        totalCost += MovementHelper.getMiningDurationTicks(context, destX, y, destZ, false);
        if (totalCost >= COST_INF) {
            return;
        }
        totalCost += MovementHelper.getMiningDurationTicks(context, destX, y + 1, destZ, true); // only the top block in the 3 we need to mine needs to consider the falling blocks above
        if (totalCost >= COST_INF) {
            return;
        }

        Block fromDown = context.get(x, y - 1, z).getBlock();
        if (MovementHelper.isClimbable(fromDown)) {
            return;
        }

        // A
        //SA
        // A
        // B
        // C
        // D
        //if S is where you start, B needs to be air for a movementfall
        //A is plausibly breakable by either descend or fall
        //C, D, etc determine the length of the fall

        BlockState below = context.get(destX, y - 2, destZ);
        if (!MovementHelper.canWalkOn(context, destX, y - 2, destZ, below)) {
            dynamicFallCost(context, x, y, z, destX, destZ, totalCost, below, res);
            return;
        }

        if (destDown.getBlock() == Blocks.ladder || destDown.getBlock() == Blocks.vine) {
            return;
        }
        if (MovementHelper.canUseFrostWalker(context, destDown)) { // no need to check assumeWalkOnWater
            return; // the water will freeze when we try to walk into it
        }

        // we walk half the block plus 0.3 to get to the edge, then we walk the other 0.2 while simultaneously falling (math.max because of how it's in parallel)
        double walk = WALK_OFF_BLOCK_COST;
        if (fromDown == Blocks.soul_sand) {
            // use this ratio to apply the soul sand speed penalty to our 0.8 block distance
            walk *= WALK_ONE_OVER_SOUL_SAND_COST / WALK_ONE_BLOCK_COST;
        }
        totalCost += walk + Math.max(FALL_N_BLOCKS_COST[1], CENTER_AFTER_FALL_COST);
        res.x = destX;
        res.y = y - 1;
        res.z = destZ;
        res.cost = totalCost;
    }

    public static boolean dynamicFallCost(CalculationContext context, int x, int y, int z, int destX, int destZ, double frontBreak, BlockState below, MutableMoveResult res) {
        if (frontBreak != 0 && context.get(destX, y + 2, destZ).getBlock() instanceof BlockFalling) {
            // if frontBreak is 0 we can actually get through this without updating the falling block and making it actually fall
            // but if frontBreak is nonzero, we're breaking blocks in front, so don't let anything fall through this column,
            // and potentially replace the water we're going to fall into
            return false;
        }
        if (!MovementHelper.canWalkThrough(context, destX, y - 2, destZ, below)) {
            return false;
        }
        double costSoFar = 0;
        int effectiveStartHeight = y;
        for (int fallHeight = 3; true; fallHeight++) {
            int newY = y - fallHeight;
            if (newY < 0) {
                // when pathing in the void / end, prevent going below bottom of world
                return false;
            }
            boolean reachedMinimum = fallHeight >= context.minFallHeight;
            BlockState ontoBlock = context.get(destX, newY, destZ);
            int unprotectedFallHeight = fallHeight - (y - effectiveStartHeight);
            double tentativeCost = WALK_OFF_BLOCK_COST + FALL_N_BLOCKS_COST[unprotectedFallHeight] + frontBreak + costSoFar;
            if (reachedMinimum && MovementHelper.isWater(ontoBlock)) {
                if (!MovementHelper.canWalkThrough(context, destX, newY, destZ, ontoBlock)) {
                    return false;
                }
                if (context.assumeWalkOnWater) {
                    return false;
                }
                if (MovementHelper.isFlowing(destX, newY, destZ, ontoBlock, context.bsi)) {
                    return false;
                }
                if (!MovementHelper.canWalkOn(context, destX, newY - 1, destZ)) {
                    return false;
                }
                res.x = destX;
                res.y = newY;
                res.z = destZ;
                res.cost = tentativeCost;
                return false;
            }
            if (reachedMinimum && context.allowFallIntoLava && MovementHelper.isLava(ontoBlock)) {
                res.x = destX;
                res.y = newY;
                res.z = destZ;
                res.cost = tentativeCost;
                return false;
            }
            if (unprotectedFallHeight <= 11 && MovementHelper.isClimbable(ontoBlock.getBlock())) {
                costSoFar += FALL_N_BLOCKS_COST[unprotectedFallHeight - 1];
                costSoFar += LADDER_DOWN_ONE_COST;
                effectiveStartHeight = newY;
                continue;
            }
            if (MovementHelper.canWalkThrough(context, destX, newY, destZ, ontoBlock)) {
                continue;
            }
            if (!MovementHelper.canWalkOn(context, destX, newY, destZ, ontoBlock)) {
                return false;
            }
            if (MovementHelper.isBottomSlab(ontoBlock)) {
                return false; // falling onto a half slab is really glitchy, and can cause more fall damage than we'd expect
            }
            if (reachedMinimum && unprotectedFallHeight <= context.maxFallHeightNoWater + 1) {
                res.x = destX;
                res.y = newY + 1;
                res.z = destZ;
                res.cost = tentativeCost;
                return false;
            }
            if (reachedMinimum && context.hasWaterBucket && unprotectedFallHeight <= context.maxFallHeightBucket + 1) {
                res.x = destX;
                res.y = newY + 1;
                res.z = destZ;
                res.cost = tentativeCost + context.placeBucketCost();
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        BetterBlockPos playerFeet = ctx.playerFeet();
        BetterBlockPos fakeDest = new BetterBlockPos(dest.x * 2 - src.x, dest.y, dest.z * 2 - src.z);
        if ((playerFeet.equals(dest) || playerFeet.equals(fakeDest)) && (MovementHelper.isLiquid(ctx, dest) || ctx.player().boundingBox.minY - dest.y < 0.5)) {
            // Wait until we're actually on the ground before saying we're done because sometimes we continue to fall if the next action starts immediately
            return state.setStatus(MovementStatus.SUCCESS);
        }
        if (safeMode()) {
            double destX = (src.x + 0.5) * 0.17 + (dest.x + 0.5) * 0.83;
            double destZ = (src.z + 0.5) * 0.17 + (dest.z + 0.5) * 0.83;
            state.setTarget(new MovementState.MovementTarget(
                    RotationUtils.calcRotationFromVec3d(ctx.playerHead(),
                            Vec3.createVectorHelper(destX, dest.y, destZ),
                            ctx.playerRotations()).withPitch(ctx.playerRotations().getPitch()),
                    false
            )).setInput(Input.MOVE_FORWARD, true);
            return state;
        }
        double diffX = ctx.player().posX - (dest.x + 0.5);
        double diffZ = ctx.player().posZ - (dest.z + 0.5);
        double ab = Math.sqrt(diffX * diffX + diffZ * diffZ);
        double x = ctx.player().posX - (src.x + 0.5);
        double z = ctx.player().posZ - (src.z + 0.5);
        double fromStart = Math.sqrt(x * x + z * z);

        if (!playerFeet.equals(dest) || ab > 0.25) {
            if (numTicks++ < 20 && fromStart < 1.25) {
                MovementHelper.moveTowards(ctx, state, fakeDest);
            } else {
                MovementHelper.moveTowards(ctx, state, dest);
            }
        }
        return state;
    }

    public boolean safeMode() {
        if (forceSafeMode) {
            return true;
        }
        // (dest - src) + dest is offset 1 more in the same direction
        // so it's the block we'd need to worry about running into if we decide to sprint straight through this descend
        BetterBlockPos into = dest.subtract(src.below()).offset(dest);
        if (skipToAscend()) {
            // if dest extends into can't walk through, but the two above are can walk through, then we can overshoot and glitch in that weird way
            return true;
        }
        for (int y = 0; y <= 2; y++) { // we could hit any of the three blocks
            if (MovementHelper.avoidWalkingInto(BlockStateInterface.get(ctx, into.above(y)))) {
                return true;
            }
        }
        return false;
    }

    public boolean skipToAscend() {
        BetterBlockPos into = dest.subtract(src.below()).offset(dest);
        return !MovementHelper.canWalkThrough(ctx, into) && MovementHelper.canWalkThrough(ctx, into.above()) && MovementHelper.canWalkThrough(ctx, into.above(2));
    }
}
