/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.pathing.movement;

import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.movement.ActionCosts;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.*;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.MovementState.MovementTarget;
import baritone.pathing.precompute.Ternary;
import baritone.utils.BlockStateInterface;
import baritone.utils.ToolSet;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static baritone.api.utils.RotationUtils.DEG_TO_RAD_F;
import static baritone.pathing.movement.Movement.HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP;
import static baritone.pathing.precompute.Ternary.*;

/**
 * Static helpers for cost calculation and movement operations in Forge 1.7.10.
 */
public interface MovementHelper extends ActionCosts, Helper {

    static boolean avoidBreaking(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
        if (!bsi.worldBorder.canPlaceAt(x, z)) {
            return true;
        }
        Block b = state.getBlock();
        return Baritone.settings().blocksToDisallowBreaking.value.contains(b)
                || b == Blocks.ice
                || b == Blocks.monster_egg
                || avoidAdjacentBreaking(bsi, x, y + 1, z, true)
                || avoidAdjacentBreaking(bsi, x + 1, y, z, false)
                || avoidAdjacentBreaking(bsi, x - 1, y, z, false)
                || avoidAdjacentBreaking(bsi, x, y, z + 1, false)
                || avoidAdjacentBreaking(bsi, x, y, z - 1, false);
    }

    static boolean avoidAdjacentBreaking(BlockStateInterface bsi, int x, int y, int z, boolean directlyAbove) {
        BlockState state = bsi.get0(x, y, z);
        Block block = state.getBlock();
        if (!directlyAbove
                && block instanceof BlockFalling
                && Baritone.settings().avoidUpdatingFallingBlocks.value
                && canFallThrough(bsi, x, y - 1, z)) {
            return true;
        }
        if (block instanceof BlockLiquid) {
            if (directlyAbove || Baritone.settings().strictLiquidCheck.value) {
                return true;
            }
            int level = state.getMetadata();
            if (level == 0) {
                return true;
            }
            return !(bsi.get0(x, y - 1, z).getBlock() instanceof BlockLiquid);
        }
        return false;
    }

    static boolean canFallThrough(BlockStateInterface bsi, int x, int y, int z) {
        Block block = bsi.get0(x, y, z).getBlock();
        return block.isAir(bsi.access, x, y, z) || block == Blocks.fire || block.getMaterial().isLiquid();
    }

    static boolean canWalkThrough(IPlayerContext ctx, BetterBlockPos pos) {
        return canWalkThrough(new BlockStateInterface(ctx), pos.x, pos.y, pos.z);
    }

    static boolean canWalkThrough(BlockStateInterface bsi, int x, int y, int z) {
        return canWalkThrough(bsi, x, y, z, bsi.get0(x, y, z));
    }

    static boolean canWalkThrough(CalculationContext context, int x, int y, int z, BlockState state) {
        return context.precomputedData.canWalkThrough(context.bsi, x, y, z, state);
    }

    static boolean canWalkThrough(CalculationContext context, int x, int y, int z) {
        return context.precomputedData.canWalkThrough(context.bsi, x, y, z, context.get(x, y, z));
    }

    static boolean canWalkThrough(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
        Ternary canWalkThrough = LegacyMovementRules.canWalkThroughBlockState(state);
        if (canWalkThrough == YES) {
            return true;
        }
        if (canWalkThrough == NO) {
            return false;
        }
        return LegacyMovementRules.canWalkThroughPosition(bsi, x, y, z, state);
    }

    static Ternary canWalkThroughBlockState(BlockState state) {
        return LegacyMovementRules.canWalkThroughBlockState(state);
    }

    static boolean canWalkThroughPosition(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
        return LegacyMovementRules.canWalkThroughPosition(bsi, x, y, z, state);
    }

    static Ternary fullyPassableBlockState(BlockState state) {
        return LegacyMovementRules.fullyPassableBlockState(state);
    }

    static boolean fullyPassable(CalculationContext context, int x, int y, int z) {
        return fullyPassable(context, x, y, z, context.get(x, y, z));
    }

    static boolean fullyPassable(CalculationContext context, int x, int y, int z, BlockState state) {
        return context.precomputedData.fullyPassable(context.bsi, x, y, z, state);
    }

    static boolean fullyPassable(IPlayerContext ctx, BetterBlockPos pos) {
        return fullyPassable(new BlockStateInterface(ctx), pos.x, pos.y, pos.z);
    }

    static boolean fullyPassable(BlockStateInterface bsi, int x, int y, int z) {
        return fullyPassable(bsi, x, y, z, bsi.get0(x, y, z));
    }

    static boolean fullyPassable(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
        Ternary fullyPassable = LegacyMovementRules.fullyPassableBlockState(state);
        if (fullyPassable == YES) {
            return true;
        }
        if (fullyPassable == NO) {
            return false;
        }
        return LegacyMovementRules.fullyPassablePosition(bsi, x, y, z, state);
    }

    static boolean fullyPassablePosition(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
        return LegacyMovementRules.fullyPassablePosition(bsi, x, y, z, state);
    }

    static boolean isReplaceable(int x, int y, int z, BlockState state, BlockStateInterface bsi) {
        Block block = state.getBlock();
        if (block == Blocks.air) {
            return true;
        }
        if (block == Blocks.snow_layer) {
            if (!bsi.worldContainsLoadedChunk(x, z)) {
                return true;
            }
            return state.getMetadata() == 0;
        }
        if (block == Blocks.tallgrass) {
            return true;
        }
        return block.isReplaceable(bsi.access, x, y, z);
    }

    static boolean isDoorPassable(IPlayerContext ctx, BetterBlockPos doorPos, BetterBlockPos playerPos) {
        if (playerPos.equals(doorPos)) {
            return false;
        }
        BlockState state = BlockStateInterface.get(ctx, doorPos);
        if (!(state.getBlock() instanceof BlockDoor)) {
            return true;
        }
        return ((BlockDoor) state.getBlock()).func_150015_f(new BlockStateInterface(ctx).access, doorPos.x, doorPos.y, doorPos.z);
    }

    static boolean isGatePassable(IPlayerContext ctx, BetterBlockPos gatePos, BetterBlockPos playerPos) {
        if (playerPos.equals(gatePos)) {
            return false;
        }
        BlockState state = BlockStateInterface.get(ctx, gatePos);
        if (!(state.getBlock() instanceof BlockFenceGate)) {
            return true;
        }
        return BlockFenceGate.isFenceGateOpen(state.getMetadata());
    }

    static boolean avoidWalkingInto(BlockState state) {
        return LegacyBlockRules.avoidWalkingInto(state);
    }

    static boolean canWalkOn(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
        Ternary canWalkOn = LegacyMovementRules.canWalkOnBlockState(state);
        if (canWalkOn == NO) {
            return false;
        }
        return (canWalkOn == YES || LegacyMovementRules.canWalkOnPosition(bsi, x, y, z, state))
                && !adjacentTerrainHazard(bsi, x, y, z);
    }

    static Ternary canWalkOnBlockState(BlockState state) {
        return LegacyMovementRules.canWalkOnBlockState(state);
    }

    static boolean canWalkOnPosition(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
        return LegacyMovementRules.canWalkOnPosition(bsi, x, y, z, state);
    }

    static boolean canWalkOn(CalculationContext context, int x, int y, int z, BlockState state) {
        return context.precomputedData.canWalkOn(context.bsi, x, y, z, state)
                && !adjacentTerrainHazard(context.bsi, x, y, z);
    }

    static boolean adjacentTerrainHazard(BlockStateInterface bsi, int x, int y, int z) {
        for (int[] offset : HORIZONTAL_HAZARDS) {
            for (int dy = 0; dy <= 2; dy++) {
                BlockState neighbor = bsi.get0(x + offset[0], y + dy, z + offset[1]);
                if (isHazardNeighbor(neighbor)) return true;
            }
        }
        return false;
    }

    static boolean isHazardNeighbor(BlockState state) {
        return LegacyBlockRules.isTerrainHazard(state)
                || state.getBlock().getMaterial() == net.minecraft.block.material.Material.lava;
    }

    int[][] HORIZONTAL_HAZARDS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    static boolean canWalkOn(CalculationContext context, int x, int y, int z) {
        return canWalkOn(context, x, y, z, context.get(x, y, z));
    }

    static boolean canWalkOn(IPlayerContext ctx, BetterBlockPos pos, BlockState state) {
        return canWalkOn(new BlockStateInterface(ctx), pos.x, pos.y, pos.z, state);
    }

    static boolean canWalkOn(IPlayerContext ctx, BetterBlockPos pos) {
        return canWalkOn(new BlockStateInterface(ctx), pos.x, pos.y, pos.z);
    }

    static boolean canWalkOn(BlockStateInterface bsi, int x, int y, int z) {
        return canWalkOn(bsi, x, y, z, bsi.get0(x, y, z));
    }

    static boolean canUseFrostWalker(CalculationContext context, BlockState state) {
        return false;
    }

    static boolean canUseFrostWalker(IPlayerContext ctx, BetterBlockPos pos) {
        return false;
    }

    static boolean mustBeSolidToWalkOn(CalculationContext context, int x, int y, int z, BlockState state) {
        Block block = state.getBlock();
        if (isClimbable(block)) {
            return false;
        }
        if (LegacyBlockRules.isWater(state)) {
            if (context.assumeWalkOnWater) {
                return false;
            }
            Block blockAbove = context.getBlock(x, y + 1, z);
            if (blockAbove.getMaterial().isLiquid()) {
                return false;
            }
        }
        return true;
    }

    static boolean canPlaceAgainst(BlockStateInterface bsi, int x, int y, int z) {
        return canPlaceAgainst(bsi, x, y, z, bsi.get0(x, y, z));
    }

    static boolean canPlaceAgainst(BlockStateInterface bsi, BetterBlockPos pos) {
        return canPlaceAgainst(bsi, pos.x, pos.y, pos.z);
    }

    static boolean canPlaceAgainst(IPlayerContext ctx, BetterBlockPos pos) {
        return canPlaceAgainst(new BlockStateInterface(ctx), pos);
    }

    static boolean canPlaceAgainst(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
        if (!bsi.worldBorder.canPlaceAt(x, z)) {
            return false;
        }
        Block block = state.getBlock();
        return block.isNormalCube(bsi.access, x, y, z) || block == Blocks.glass || block == Blocks.stained_glass;
    }

    static boolean isClimbable(Block block) {
        return block == Blocks.ladder || block == Blocks.vine;
    }

    static double getMiningDurationTicks(CalculationContext context, int x, int y, int z, boolean includeFalling) {
        return getMiningDurationTicks(context, x, y, z, context.get(x, y, z), includeFalling);
    }

    static double getMiningDurationTicks(CalculationContext context, int x, int y, int z, BlockState state, boolean includeFalling) {
        Block block = state.getBlock();
        if (LegacyBlockRules.isTerrainHazard(state)) return COST_INF;
        if (!canWalkThrough(context, x, y, z, state)) {
            if (block.getMaterial().isLiquid()) {
                return COST_INF;
            }
            double mult = context.breakCostMultiplierAt(x, y, z, state);
            if (mult >= COST_INF) {
                return COST_INF;
            }
            if (avoidBreaking(context.bsi, x, y, z, state)) {
                return COST_INF;
            }
            double strVsBlock = context.toolSet.getStrVsBlock(state, x, y, z);
            if (strVsBlock <= 0) {
                return COST_INF;
            }
            double result = 1 / strVsBlock;
            result += context.breakBlockAdditionalCost;
            if (block.getMaterial() == net.minecraft.block.material.Material.leaves) result += 10;
            result *= mult;
            if (includeFalling) {
                BlockState above = context.get(x, y + 1, z);
                if (above.getBlock() instanceof BlockFalling) {
                    result += getMiningDurationTicks(context, x, y + 1, z, above, true);
                }
            }
            return result;
        }
        return 0;
    }

    static boolean isBottomSlab(BlockState state) {
        return LegacyBlockRules.isBottomSlab(state);
    }

    static void switchToBestToolFor(IPlayerContext ctx, BlockState b) {
        switchToBestToolFor(ctx, b, new ToolSet(ctx.player()), BaritoneAPI.getSettings().preferSilkTouch.value);
    }

    static void switchToBestToolFor(IPlayerContext ctx, BlockState b, ToolSet ts, boolean preferSilkTouch) {
        if (Baritone.settings().autoTool.value && !Baritone.settings().assumeExternalAutoTool.value) {
            int bestHotbar = ts.getBestSlot(b, preferSilkTouch);
            ctx.player().inventory.currentItem = bestHotbar;
            if (Baritone.settings().allowInventory.value) {
                int bestInvy = ts.getBestSlotInInventory(b, preferSilkTouch);
                if (bestInvy >= 9) {
                    ItemStack invStack = ctx.player().inventory.mainInventory[bestInvy];
                    ItemStack hotStack = ctx.player().inventory.mainInventory[bestHotbar];
                    double invSpeed = ToolSet.calculateSpeedVsBlock(invStack, b, ctx.world(), 0, 0, 0);
                    double hotSpeed = ToolSet.calculateSpeedVsBlock(hotStack, b, ctx.world(), 0, 0, 0);
                    if (invSpeed > hotSpeed) {
                        IBaritone primary = BaritoneAPI.getProvider().getPrimaryBaritone();
                        if (primary instanceof Baritone) {
                            ((Baritone) primary).getInventoryBehavior().attemptToPutOnHotbar(bestInvy, slot -> slot == bestHotbar);
                        }
                    }
                }
            }
        }
    }

    static void moveTowards(IPlayerContext ctx, MovementState state, BetterBlockPos pos) {
        state.setTarget(new MovementTarget(
                RotationUtils.calcRotationFromVec3d(ctx.playerHead(),
                        VecUtils.getBlockPosCenter(pos),
                        ctx.playerRotations()).withPitch(ctx.playerRotations().getPitch()),
                false
        )).setInput(Input.MOVE_FORWARD, true);
    }

    static void moveTowardsWithoutRotation(IPlayerContext ctx, MovementState state, float idealYaw) {
        MovementOption.getOptions(
                MathHelper.sin(ctx.playerRotations().getYaw() * DEG_TO_RAD_F),
                MathHelper.cos(ctx.playerRotations().getYaw() * DEG_TO_RAD_F),
                Baritone.settings().allowSprint.value
        ).min(Comparator.comparingDouble((MovementOption option) -> (double) option.distanceToSq(
                MathHelper.sin(idealYaw * DEG_TO_RAD_F),
                MathHelper.cos(idealYaw * DEG_TO_RAD_F)
        ))).ifPresent(selection -> selection.setInputs(state));
    }

    static void moveTowardsWithoutRotation(IPlayerContext ctx, MovementState state, BetterBlockPos dest) {
        float idealYaw = RotationUtils.calcRotationFromVec3d(
                ctx.playerHead(),
                VecUtils.getBlockPosCenter(dest),
                ctx.playerRotations()
        ).getYaw();
        moveTowardsWithoutRotation(ctx, state, idealYaw);
    }

    static void moveTowardsWithSlightRotation(IPlayerContext ctx, MovementState state, BetterBlockPos dest) {
        float idealYaw = RotationUtils.calcRotationFromVec3d(
                ctx.playerHead(),
                VecUtils.getBlockPosCenter(dest),
                ctx.playerRotations()
        ).getYaw();
        float distance = Rotation.yawDistanceFromOffset(ctx.playerRotations().getYaw(), idealYaw) % 45f;
        float newYaw = distance > 0f ?
                distance > 22.5f ? distance - 45f : distance :
                distance < -22.5f ? distance + 45f : distance;
        state.setTarget(new MovementTarget(new Rotation(
                ctx.playerRotations().getYaw() - newYaw,
                ctx.playerRotations().getPitch()
        ), true));
        moveTowardsWithoutRotation(ctx, state, idealYaw);
    }

    static boolean isWater(BlockState state) {
        return LegacyBlockRules.isWater(state);
    }

    static boolean isWater(IPlayerContext ctx, BetterBlockPos bp) {
        return isWater(BlockStateInterface.get(ctx, bp));
    }

    static boolean isLava(BlockState state) {
        return state.getBlock().getMaterial() == Material.lava;
    }

    static boolean isLiquid(IPlayerContext ctx, BetterBlockPos p) {
        return isLiquid(BlockStateInterface.get(ctx, p));
    }

    static boolean isLiquid(BlockState blockState) {
        return blockState.getBlock().getMaterial().isLiquid();
    }

    static boolean possiblyFlowing(BlockState state) {
        return LegacyBlockRules.possiblyFlowing(state);
    }

    static boolean isFlowing(int x, int y, int z, BlockState state, BlockStateInterface bsi) {
        if (!state.getBlock().getMaterial().isLiquid()) {
            return false;
        }
        if (state.getMetadata() != 0) {
            return true;
        }
        return possiblyFlowing(bsi.get0(x + 1, y, z))
                || possiblyFlowing(bsi.get0(x - 1, y, z))
                || possiblyFlowing(bsi.get0(x, y, z + 1))
                || possiblyFlowing(bsi.get0(x, y, z - 1));
    }

    static boolean isBlockNormalCube(BlockState state) {
        return state.getBlock().isNormalCube();
    }

    static PlaceResult attemptToPlaceABlock(MovementState state, IBaritone baritone, BetterBlockPos placeAt, boolean preferDown, boolean wouldSneak) {
        IPlayerContext ctx = baritone.getPlayerContext();
        Optional<Rotation> direct = RotationUtils.reachable(ctx, placeAt, wouldSneak);
        boolean found = false;
        if (direct.isPresent()) {
            state.setTarget(new MovementTarget(direct.get(), true));
            found = true;
        }
        for (int i = 0; i < 5; i++) {
            ForgeDirection dir = HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[i];
            BetterBlockPos against1 = placeAt.relative(dir);
            if (MovementHelper.canPlaceAgainst(ctx, against1)) {
                if (!((Baritone) baritone).getInventoryBehavior().selectThrowawayForLocation(false, placeAt.x, placeAt.y, placeAt.z)) {
                    Helper.HELPER.logDebug("bb pls get me some blocks. dirt, netherrack, cobble");
                    state.setStatus(MovementStatus.UNREACHABLE);
                    return PlaceResult.NO_OPTION;
                }
                double faceX = (placeAt.x + against1.x + 1.0D) * 0.5D;
                double faceY = (placeAt.y + against1.y + 0.5D) * 0.5D;
                double faceZ = (placeAt.z + against1.z + 1.0D) * 0.5D;
                Rotation place = RotationUtils.calcRotationFromVec3d(
                        wouldSneak ? RayTraceUtils.inferSneakingEyePosition(ctx.player()) : ctx.playerHead(),
                        Vec3.createVectorHelper(faceX, faceY, faceZ),
                        ctx.playerRotations()
                );
                Rotation actual = baritone.getLookBehavior().getAimProcessor().peekRotation(place);
                MovingObjectPosition res = RayTraceUtils.rayTraceTowards(ctx.player(), actual, ctx.playerController().getBlockReachDistance(), wouldSneak);
                if (res != null && res.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                        && res.blockX == against1.x && res.blockY == against1.y && res.blockZ == against1.z
                        && against1.relative(ForgeDirection.getOrientation(res.sideHit)).equals(placeAt)) {
                    state.setTarget(new MovementTarget(place, true));
                    found = true;

                    if (!preferDown) {
                        break;
                    }
                }
            }
        }
        if (ctx.getSelectedBlock().isPresent()) {
            BetterBlockPos selectedBlock = ctx.getSelectedBlock().get();
            MovingObjectPosition mop = ctx.objectMouseOver();
            if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                ForgeDirection side = ForgeDirection.getOrientation(mop.sideHit);
                if (selectedBlock.equals(placeAt) || (MovementHelper.canPlaceAgainst(ctx, selectedBlock) && selectedBlock.relative(side).equals(placeAt))) {
                    if (wouldSneak) {
                        state.setInput(Input.SNEAK, true);
                    }
                    ((Baritone) baritone).getInventoryBehavior().selectThrowawayForLocation(true, placeAt.x, placeAt.y, placeAt.z);
                    return PlaceResult.READY_TO_PLACE;
                }
            }
        }
        if (found) {
            if (wouldSneak) {
                state.setInput(Input.SNEAK, true);
            }
            ((Baritone) baritone).getInventoryBehavior().selectThrowawayForLocation(true, placeAt.x, placeAt.y, placeAt.z);
            return PlaceResult.ATTEMPTING;
        }
        return PlaceResult.NO_OPTION;
    }

    enum PlaceResult {
        READY_TO_PLACE, ATTEMPTING, NO_OPTION;
    }

    static boolean isTransparent(Block b) {
        return b instanceof BlockAir
                || b.getMaterial() == Material.air
                || b.getMaterial().isLiquid()
                || b == Blocks.lava
                || b == Blocks.flowing_lava
                || b == Blocks.water
                || b == Blocks.flowing_water;
    }

    static List<BetterBlockPos> steppingOnBlocks(IPlayerContext ctx) {
        List<BetterBlockPos> blocks = new ArrayList<>();
        int minX = MathHelper.floor_double(ctx.player().boundingBox.minX);
        int maxX = MathHelper.floor_double(ctx.player().boundingBox.maxX);
        int minY = MathHelper.floor_double(ctx.player().boundingBox.minY);
        int minZ = MathHelper.floor_double(ctx.player().boundingBox.minZ);
        int maxZ = MathHelper.floor_double(ctx.player().boundingBox.maxZ);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                blocks.add(new BetterBlockPos(x, minY - 1, z));
            }
        }
        return blocks;
    }
}
