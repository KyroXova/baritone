/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.pathing.movement.movements;

import baritone.Baritone;
import baritone.api.IBaritone;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockState;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.VecUtils;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.BlockStateInterface;
import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.init.Blocks;
import net.minecraft.util.Vec3;

import java.util.Set;

public class MovementPillar extends Movement {

    public MovementPillar(IBaritone baritone, BetterBlockPos start, BetterBlockPos end) {
        super(baritone, start, end, new BetterBlockPos[]{start.above(2)}, start);
    }

    @Override
    public double calculateCost(CalculationContext context) {
        return cost(context, src.x, src.y, src.z);
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        return ImmutableSet.of(src, dest);
    }

    public static double cost(CalculationContext context, int x, int y, int z) {
        BlockState fromState = context.get(x, y, z);
        Block from = fromState.getBlock();
        boolean ladder = MovementHelper.isClimbable(from);
        BlockState fromDown = context.get(x, y - 1, z);
        if (!ladder) {
            if (MovementHelper.isClimbable(fromDown.getBlock())) {
                return COST_INF; // can't pillar from a ladder or vine onto something that isn't also climbable
            }
            if (MovementHelper.isBottomSlab(fromDown)) {
                return COST_INF; // can't pillar up from a bottom slab onto a non ladder
            }
        }
        BlockState toBreak = context.get(x, y + 2, z);
        Block toBreakBlock = toBreak.getBlock();
        if (toBreakBlock instanceof BlockFenceGate) { // see issue #172
            return COST_INF;
        }
        BlockState srcUp = null;
        if (MovementHelper.isWater(toBreak) && MovementHelper.isWater(fromState)) {
            srcUp = context.get(x, y + 1, z);
            if (MovementHelper.isWater(srcUp)) {
                return LADDER_UP_ONE_COST; // allow ascending pillars of water, but only if we're already in one
            }
        }
        double placeCost = 0;
        if (!ladder) {
            // we need to place a block where we started to jump on it
            placeCost = context.costOfPlacingAt(x, y, z, fromState);
            if (placeCost >= COST_INF) {
                return COST_INF;
            }
            if (fromDown.getBlock() instanceof BlockAir) {
                placeCost += 0.1; // slightly (1/200th of a second) penalize pillaring on what's currently air
            }
        }
        if ((MovementHelper.isLiquid(fromState) && !MovementHelper.canPlaceAgainst(context.bsi, x, y - 1, z, fromDown)) || (MovementHelper.isLiquid(fromDown) && context.assumeWalkOnWater)) {
            return COST_INF;
        }
        if ((from == Blocks.waterlily || from instanceof BlockCarpet) && MovementHelper.isLiquid(fromDown)) {
            return COST_INF;
        }
        double hardness = MovementHelper.getMiningDurationTicks(context, x, y + 2, z, toBreak, true);
        if (hardness >= COST_INF) {
            return COST_INF;
        }
        if (hardness != 0) {
            if (MovementHelper.isClimbable(toBreakBlock)) {
                hardness = 0; // we won't actually need to break the ladder / vine because we're going to use it
            } else {
                BlockState check = context.get(x, y + 3, z); // the block on top of the one we're going to break, could it fall on us?
                if (check.getBlock() instanceof BlockFalling) {
                    if (srcUp == null) {
                        srcUp = context.get(x, y + 1, z);
                    }
                    if (!(toBreakBlock instanceof BlockFalling) || !(srcUp.getBlock() instanceof BlockFalling)) {
                        return COST_INF;
                    }
                }
            }
        }
        if (ladder) {
            return LADDER_UP_ONE_COST + hardness * 5;
        } else {
            return JUMP_ONE_BLOCK_COST + placeCost + context.jumpPenalty + hardness;
        }
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        if (ctx.playerFeet().y < src.y) {
            return state.setStatus(MovementStatus.UNREACHABLE);
        }

        BlockState fromDown = BlockStateInterface.get(ctx, src);
        if (MovementHelper.isWater(fromDown) && MovementHelper.isWater(ctx, dest)) {
            // stay centered while swimming up a water column
            state.setTarget(new MovementState.MovementTarget(RotationUtils.calcRotationFromVec3d(ctx.playerHead(), VecUtils.getBlockPosCenter(dest), ctx.playerRotations()), false));
            Vec3 destCenter = VecUtils.getBlockPosCenter(dest);
            if (Math.abs(ctx.player().posX - destCenter.xCoord) > 0.2 || Math.abs(ctx.player().posZ - destCenter.zCoord) > 0.2) {
                state.setInput(Input.MOVE_FORWARD, true);
            }
            if (ctx.playerFeet().equals(dest)) {
                return state.setStatus(MovementStatus.SUCCESS);
            }
            return state;
        }
        boolean ladder = MovementHelper.isClimbable(fromDown.getBlock());

        Rotation rotation = RotationUtils.calcRotationFromVec3d(ctx.playerHead(),
                VecUtils.getBlockPosCenter(positionToPlace),
                ctx.playerRotations());
        if (!ladder) {
            state.setTarget(new MovementState.MovementTarget(ctx.playerRotations().withPitch(rotation.getPitch()), true));
        }

        boolean blockIsThere = MovementHelper.canWalkOn(ctx, src) || ladder;
        if (ladder) {
            if (ctx.playerFeet().equals(dest)) {
                return state.setStatus(MovementStatus.SUCCESS);
            }

            MovementHelper.moveTowards(ctx, state, dest);
            state.setInput(Input.JUMP, true);
            return state;
        } else {
            // Get ready to place a throwaway block
            if (!((Baritone) baritone).getInventoryBehavior().selectThrowawayForLocation(true, src.x, src.y, src.z)) {
                return state.setStatus(MovementStatus.UNREACHABLE);
            }

            state.setInput(Input.SNEAK, true);

            double diffX = ctx.player().posX - (dest.x + 0.5);
            double diffZ = ctx.player().posZ - (dest.z + 0.5);
            double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
            double flatMotion = Math.sqrt(ctx.player().motionX * ctx.player().motionX + ctx.player().motionZ * ctx.player().motionZ);
            if (dist > 0.17) {
                state.setInput(Input.MOVE_FORWARD, true);
                state.setTarget(new MovementState.MovementTarget(rotation, true));
            } else if (flatMotion < 0.05) {
                state.setInput(Input.JUMP, ctx.player().posY < dest.y);
            }

            if (!blockIsThere) {
                BlockState frState = BlockStateInterface.get(ctx, src);
                Block fr = frState.getBlock();
                if (!(fr instanceof BlockAir || (fr != null && fr.isReplaceable(ctx.world(), src.x, src.y, src.z)))) {
                    RotationUtils.reachable(ctx, src, ctx.playerController().getBlockReachDistance())
                            .map(rot -> new MovementState.MovementTarget(rot, true))
                            .ifPresent(state::setTarget);
                    state.setInput(Input.JUMP, false);
                    state.setInput(Input.CLICK_LEFT, true);
                    blockIsThere = false;
                } else if (ctx.player().isSneaking() && (ctx.isLookingAt(src.below()) || ctx.isLookingAt(src)) && ctx.player().posY > dest.y + 0.1) {
                    state.setInput(Input.CLICK_RIGHT, true);
                }
            }
        }

        // If we are at our goal and the block below us is placed
        if (ctx.playerFeet().equals(dest) && blockIsThere) {
            return state.setStatus(MovementStatus.SUCCESS);
        }

        return state;
    }

    @Override
    protected boolean prepared(MovementState state) {
        if (ctx.playerFeet().equals(src) || ctx.playerFeet().equals(src.below())) {
            Block block = BlockStateInterface.getBlock(ctx, src.below());
            if (MovementHelper.isClimbable(block)) {
                state.setInput(Input.SNEAK, true);
            }
        }
        if (MovementHelper.isWater(ctx, dest.above())) {
            return true;
        }
        return super.prepared(state);
    }
}
