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

package baritone.launch.mixins;

import baritone.utils.accessor.IPlayerControllerMP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.world.WorldSettings.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerControllerMP.class)
public abstract class MixinPlayerController implements IPlayerControllerMP {

    @Accessor("isHittingBlock")
    @Override
    public abstract void setIsHittingBlock(boolean isHittingBlock);

    @Accessor("isHittingBlock")
    @Override
    public abstract boolean isHittingBlock();

    @Accessor("currentBlockX")
    @Override
    public abstract int getCurrentBlockX();

    @Accessor("currentBlockY")
    @Override
    public abstract int getCurrentBlockY();

    @Accessor("currentblockZ")
    @Override
    public abstract int getCurrentBlockZ();

    @Accessor("currentGameType")
    @Override
    public abstract GameType getCurrentGameType();

    @Invoker("syncCurrentPlayItem")
    @Override
    public abstract void callSyncCurrentPlayItem();

    @Accessor("blockHitDelay")
    @Override
    public abstract void setDestroyDelay(int destroyDelay);
}
