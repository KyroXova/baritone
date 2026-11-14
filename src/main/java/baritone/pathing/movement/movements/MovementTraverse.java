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
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockSlab;
import net.minecraft.init.Blocks;
import net.minecraft.util.Vec3;

import java.util.Optional;
import java.util.Set;

public class MovementTraverse extends Movement {

    public MovementTraverse(IBaritone baritone, BetterBlockPos from, BetterBlockPos to) {
        super(baritone, from, to, new BetterBlockPos[]{to.above(), to}, to.below());
    }

    @Override
    public double calculateCost(CalculationContext context) {
        return cost(context, src.x, src.y, src.z, dest.x, dest.z);
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        return ImmutableSet.of(src, dest);
    }

    public static double cost(CalculationContext context, int x, int y, int z, int destX, int destZ) {
        BlockState pb0 = context.get(destX, y + 1, destZ);
        BlockState pb1 = context.get(destX, y, destZ);
        BlockState destOn = context.get(destX, y - 1, destZ);
        BlockState srcDown = context.get(x, y - 1, z);
        Block srcDownBlock = srcDown.getBlock();
        boolean standingOnABlock = MovementHelper.mustBeSolidToWalkOn(context, x, y - 1, z, srcDown);
        boolean frostWalker = standingOnABlock && !context.assumeWalkOnWater && MovementHelper.canUseFrostWalker(context, destOn);
        if (frostWalker || MovementHelper.canWalkOn(context, destX, y - 1, destZ, destOn)) {
            double WC = WALK_ONE_BLOCK_COST;
            boolean water = false;
            boolean sneaking = false;
            if (MovementHelper.isWater(pb0) || MovementHelper.isWater(pb1)) {
                WC = context.waterWalkSpeed;
                water = true;
            } else {
                if (destOn.getBlock() == Blocks.soul_sand) {
                    WC += (WALK_ONE_OVER_SOUL_SAND_COST - WALK_ONE_BLOCK_COST) / 2;
                } else if (frostWalker) {
                    // frostwalker
                } else if (MovementHelper.isWater(destOn)) {
                    WC += context.walkOnWaterOnePenalty;
                }
                if (srcDownBlock == Blocks.soul_sand) {
                    WC += (WALK_ONE_OVER_SOUL_SAND_COST - WALK_ONE_BLOCK_COST) / 2;
                }
            }
            double hardness1 = MovementHelper.getMiningDurationTicks(context, destX, y, destZ, pb1, false);
            if (hardness1 >= COST_INF) {
                return COST_INF;
            }
            double hardness2 = MovementHelper.getMiningDurationTicks(context, destX, y + 1, destZ, pb0, true);
            if (hardness1 == 0 && hardness2 == 0) {
                if (!water && !sneaking && context.canSprint) {
                    WC *= SPRINT_MULTIPLIER;
                }
                return WC;
            }
            if (MovementHelper.isClimbable(srcDownBlock)) {
                hardness1 *= 5;
                hardness2 *= 5;
            }
            return WC + hardness1 + hardness2;
        } else {
            if (MovementHelper.isClimbable(srcDownBlock)) {
                return COST_INF;
            }
            if (MovementHelper.isReplaceable(destX, y - 1, destZ, destOn, context.bsi)) {
                boolean throughWater = MovementHelper.isWater(pb0) || MovementHelper.isWater(pb1);
                if (MovementHelper.isWater(destOn) && throughWater) {
                    return COST_INF;
                }
                double placeCost = context.costOfPlacingAt(destX, y - 1, destZ, destOn);
                if (placeCost >= COST_INF) {
                    return COST_INF;
                }
                double hardness1 = MovementHelper.getMiningDurationTicks(context, destX, y, destZ, pb1, false);
                if (hardness1 >= COST_INF) {
                    return COST_INF;
                }
                double hardness2 = MovementHelper.getMiningDurationTicks(context, destX, y + 1, destZ, pb0, true);
                double WC = throughWater ? context.waterWalkSpeed : WALK_ONE_BLOCK_COST;
                for (int i = 0; i < 5; i++) {
                    int againstX = destX + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[i].offsetX;
                    int againstY = y - 1 + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[i].offsetY;
                    int againstZ = destZ + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[i].offsetZ;
                    if (againstX == x && againstZ == z) {
                        continue;
                    }
                    if (MovementHelper.canPlaceAgainst(context.bsi, againstX, againstY, againstZ)) {
                        return WC + placeCost + hardness1 + hardness2;
                    }
                }
                if (srcDownBlock == Blocks.soul_sand || MovementHelper.isBottomSlab(srcDown)) {
                    return COST_INF;
                }
                if (!standingOnABlock) {
                    return COST_INF;
                }
                Block blockSrc = context.getBlock(x, y, z);
                if ((blockSrc == Blocks.waterlily || blockSrc instanceof BlockCarpet) && MovementHelper.isLiquid(srcDown)) {
                    return COST_INF;
                }
                WC = WC * (SNEAK_ONE_BLOCK_COST / WALK_ONE_BLOCK_COST);
                return WC + placeCost + hardness1 + hardness2;
            }
            return COST_INF;
        }
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        BlockState pb0 = BlockStateInterface.get(ctx, positionsToBreak[0]);
        BlockState pb1 = BlockStateInterface.get(ctx, positionsToBreak[1]);
        if (state.getStatus() != MovementStatus.RUNNING) {
            if (!Baritone.settings().walkWhileBreaking.value) {
                return state;
            }
            if (state.getStatus() != MovementStatus.PREPPING) {
                return state;
            }
            if (MovementHelper.avoidWalkingInto(pb0)) {
                return state;
            }
            if (MovementHelper.avoidWalkingInto(pb1)) {
                return state;
            }
            double dist = Math.max(Math.abs(ctx.player().posX - (dest.x + 0.5D)), Math.abs(ctx.player().posZ - (dest.z + 0.5D)));
            if (dist < 0.83) {
                return state;
            }
            if (!state.getTarget().getRotation().isPresent()) {
                return state;
            }

            float yawToDest = RotationUtils.calcRotationFromVec3d(ctx.playerHead(), VecUtils.calculateBlockCenter(ctx.world(), dest), ctx.playerRotations()).getYaw();
            float pitchToBreak = state.getTarget().getRotation().get().getPitch();
            if ((MovementHelper.isBlockNormalCube(pb0) || pb0.getBlock() instanceof BlockAir && (MovementHelper.isBlockNormalCube(pb1) || pb1.getBlock() instanceof BlockAir))) {
                pitchToBreak = 26;
            }

            return state.setTarget(new MovementState.MovementTarget(new Rotation(yawToDest, pitchToBreak), true))
                    .setInput(Input.MOVE_FORWARD, true)
                    .setInput(Input.SPRINT, true);
        }

        Block fd = BlockStateInterface.get(ctx, src.below()).getBlock();
        boolean ladder = MovementHelper.isClimbable(fd);

        if (pb0.getBlock() instanceof BlockDoor || pb1.getBlock() instanceof BlockDoor) {
            boolean notPassable = pb0.getBlock() instanceof BlockDoor && !MovementHelper.isDoorPassable(ctx, src, dest) || pb1.getBlock() instanceof BlockDoor && !MovementHelper.isDoorPassable(ctx, dest, src);
            boolean canOpen = !(Blocks.iron_door.equals(pb0.getBlock()) || Blocks.iron_door.equals(pb1.getBlock()));

            if (notPassable && canOpen) {
                return state.setTarget(new MovementState.MovementTarget(RotationUtils.calcRotationFromVec3d(ctx.playerHead(), VecUtils.calculateBlockCenter(ctx.world(), positionsToBreak[0]), ctx.playerRotations()), true))
                        .setInput(Input.CLICK_RIGHT, true);
            }
        }

        if (pb0.getBlock() instanceof BlockFenceGate || pb1.getBlock() instanceof BlockFenceGate) {
            BetterBlockPos blocked = !MovementHelper.isGatePassable(ctx, positionsToBreak[0], src.above()) ? positionsToBreak[0]
                    : !MovementHelper.isGatePassable(ctx, positionsToBreak[1], src) ? positionsToBreak[1]
                    : null;
            if (blocked != null) {
                Optional<Rotation> rotation = RotationUtils.reachable(ctx, blocked);
                if (rotation.isPresent()) {
                    return state.setTarget(new MovementState.MovementTarget(rotation.get(), true)).setInput(Input.CLICK_RIGHT, true);
                }
            }
        }

        boolean isTheBridgeBlockThere = MovementHelper.canWalkOn(ctx, positionToPlace) || ladder || MovementHelper.canUseFrostWalker(ctx, positionToPlace);
        BetterBlockPos feet = ctx.playerFeet();
        if (isTheBridgeBlockThere) {
            if (ladder) {
                state.setInput(Input.SNEAK, true);
            }
            double diffX = ctx.player().posX - (dest.x + 0.5D);
            double diffZ = ctx.player().posZ - (dest.z + 0.5D);
            double flatDistToDest = Math.sqrt(diffX * diffX + diffZ * diffZ);

            if (flatDistToDest < 0.25D) {
                state.setStatus(MovementStatus.SUCCESS);
                return state;
            }

            if (feet.equals(dest)) {
                MovementHelper.moveTowardsWithoutRotation(ctx, state, dest);
                return state;
            }

            MovementHelper.moveTowards(ctx, state, dest);
            return state;
        } else {
            double dist1 = Math.max(Math.abs(ctx.player().posX - (dest.x + 0.5D)), Math.abs(ctx.player().posZ - (dest.z + 0.5D)));

            PlaceResult p = MovementHelper.attemptToPlaceABlock(state, baritone, dest.below(), false, !Baritone.settings().assumeSafeWalk.value);
            if ((p == PlaceResult.READY_TO_PLACE || dist1 < 0.6) && !Baritone.settings().assumeSafeWalk.value) {
                state.setInput(Input.SNEAK, true);
            }
            switch (p) {
                case READY_TO_PLACE: {
                    if (ctx.player().isSneaking() || Baritone.settings().assumeSafeWalk.value) {
                        state.setInput(Input.CLICK_RIGHT, true);
                    }
                    return state;
                }
                case ATTEMPTING: {
                    if (dist1 > 0.83) {
                        float yaw = RotationUtils.calcRotationFromVec3d(ctx.playerHead(), VecUtils.getBlockPosCenter(dest), ctx.playerRotations()).getYaw();
                        if (Math.abs(state.getTarget().rotation.getYaw() - yaw) < 0.1) {
                            return state.setInput(Input.MOVE_FORWARD, true);
                        }
                    } else if (ctx.playerRotations().isReallyCloseTo(state.getTarget().rotation)) {
                        return state.setInput(Input.CLICK_LEFT, true);
                    }
                    return state;
                }
                default:
                    break;
            }
            if (feet.equals(dest)) {
                double faceX = (dest.x + src.x + 1.0D) * 0.5D;
                double faceY = (dest.y + src.y - 1.0D) * 0.5D;
                double faceZ = (dest.z + src.z + 1.0D) * 0.5D;
                BetterBlockPos goalLook = src.below();

                Rotation backToFace = RotationUtils.calcRotationFromVec3d(ctx.playerHead(), Vec3.createVectorHelper(faceX, faceY, faceZ), ctx.playerRotations());
                float pitch = backToFace.getPitch();
                double dist2 = Math.max(Math.abs(ctx.player().posX - faceX), Math.abs(ctx.player().posZ - faceZ));
                if (dist2 < 0.29) {
                    float yaw = RotationUtils.calcRotationFromVec3d(VecUtils.getBlockPosCenter(dest), ctx.playerHead(), ctx.playerRotations()).getYaw();
                    state.setTarget(new MovementState.MovementTarget(new Rotation(yaw, pitch), true));
                    state.setInput(Input.MOVE_BACK, true);
                } else {
                    state.setTarget(new MovementState.MovementTarget(backToFace, true));
                }
                if (ctx.isLookingAt(goalLook)) {
                    return state.setInput(Input.CLICK_RIGHT, true);
                }
                if (ctx.playerRotations().isReallyCloseTo(state.getTarget().rotation)) {
                    state.setInput(Input.CLICK_LEFT, true);
                }
                return state;
            }
            MovementHelper.moveTowardsWithSlightRotation(ctx, state, dest);
            return state;
        }
    }

    @Override
    public boolean safeToCancel(MovementState state) {
        return state.getStatus() != MovementStatus.RUNNING || MovementHelper.canWalkOn(ctx, dest.below());
    }

    @Override
    protected boolean prepared(MovementState state) {
        if (ctx.playerFeet().equals(src) || ctx.playerFeet().equals(src.below())) {
            Block block = BlockStateInterface.getBlock(ctx, src.below());
            if (MovementHelper.isClimbable(block)) {
                state.setInput(Input.SNEAK, true);
            }
        }
        return super.prepared(state);
    }
}
