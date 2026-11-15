/* This file is part of Baritone, distributed under the LGPL-3.0-or-later. */
package baritone.utils;

import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.IBlockInteraction;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.RayTraceUtils;
import baritone.Baritone;
import baritone.api.utils.input.Input;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public final class BlockInteraction implements IBlockInteraction {
    private final IPlayerContext ctx;
    private final Baritone baritone;

    public BlockInteraction(IPlayerContext ctx, Baritone baritone) {
        this.ctx = ctx;
        this.baritone = baritone;
    }

    private Result validate(BetterBlockPos pos, ForgeDirection face, Vec3 hit, boolean sneak, boolean requireLineOfSight) {
        Minecraft mc = ctx.minecraft();
        if (!mc.func_152345_ab()) return Result.WRONG_THREAD;
        EntityClientPlayerMP player = ctx.player();
        World world = ctx.world();
        if (player == null || world == null || mc.playerController == null || player.sendQueue == null || mc.currentScreen != null) {
            return Result.UNAVAILABLE;
        }
        if (!validHit(pos, face, hit) || pos.y < 0 || pos.y >= 256
                || !new BlockStateInterface(ctx).worldContainsLoadedChunk(pos.x, pos.z)) return Result.INVALID_TARGET;
        Vec3 eyes = sneak ? RayTraceUtils.inferSneakingEyePosition(player) : ctx.playerHead();
        double reach = ctx.playerController().getBlockReachDistance();
        if (eyes.squareDistanceTo(hit) > reach * reach) return Result.OUT_OF_REACH;
        if (requireLineOfSight) {
            double dx = hit.xCoord - eyes.xCoord, dy = hit.yCoord - eyes.yCoord, dz = hit.zCoord - eyes.zCoord;
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            Vec3 end = length == 0 ? hit : hit.addVector(dx / length * 0.001, dy / length * 0.001, dz / length * 0.001);
            MovingObjectPosition trace = world.func_147447_a(eyes, end, false, false, true);
            if (trace == null || trace.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                    || trace.blockX != pos.x || trace.blockY != pos.y || trace.blockZ != pos.z
                    || trace.sideHit != face.ordinal()) return Result.OBSTRUCTED;
        }
        return Result.ATTEMPTED;
    }

    static boolean validHit(BetterBlockPos pos, ForgeDirection face, Vec3 hit) {
        if (pos == null || face == null || face == ForgeDirection.UNKNOWN || hit == null) return false;
        double x = hit.xCoord - pos.x, y = hit.yCoord - pos.y, z = hit.zCoord - pos.z;
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && x >= 0 && x <= 1 && y >= 0 && y <= 1 && z >= 0 && z <= 1;
    }

    @Override
    public Result useBlock(BetterBlockPos pos, ForgeDirection face, Vec3 hit, boolean sneak, boolean requireLineOfSight) {
        Result result = validate(pos, face, hit, sneak, requireLineOfSight);
        if (result != Result.ATTEMPTED) return result;
        EntityClientPlayerMP player = ctx.player();
        boolean restoreSneak = sneak && !player.isSneaking();
        if (restoreSneak) {
            player.sendQueue.addToSendQueue(new C0BPacketEntityAction(player, 1));
            player.setSneaking(true);
        }
        try {
            ctx.playerController().processRightClickBlock(player, ctx.world(),
                    new MovingObjectPosition(pos.x, pos.y, pos.z, face.ordinal(), hit));
            player.swingItem();
            return Result.ATTEMPTED;
        } finally {
            if (restoreSneak) {
                player.sendQueue.addToSendQueue(new C0BPacketEntityAction(player, 2));
                player.setSneaking(false);
            }
        }
    }

    @Override
    public Result attackBlock(BetterBlockPos pos, ForgeDirection face, Vec3 hit, boolean requireLineOfSight) {
        Result result = validate(pos, face, hit, false, requireLineOfSight);
        if (result != Result.ATTEMPTED) return result;
        ctx.playerController().clickBlock(pos, face);
        ctx.player().swingItem();
        return Result.ATTEMPTED;
    }

    @Override
    public Result startBreaking(BetterBlockPos pos, ForgeDirection face, Vec3 hit) {
        Result result = validate(pos, face, hit, false, true);
        if (result != Result.ATTEMPTED) return result;
        if (baritone.getPathingBehavior().isPathing() || baritone.getPathingBehavior().getInProgress().isPresent()
                || baritone.getInputOverrideHandler().isInputForcedDown(Input.CLICK_LEFT)) return Result.UNAVAILABLE;
        return baritone.getInputOverrideHandler().getBlockBreakHelper().lockTarget(pos) ? Result.ATTEMPTED : Result.UNAVAILABLE;
    }

    @Override
    public void stopBreaking() {
        baritone.getInputOverrideHandler().getBlockBreakHelper().clearTarget();
    }

    @Override
    public boolean isBreaking() {
        return baritone.getInputOverrideHandler().getBlockBreakHelper().hasTarget();
    }
}
