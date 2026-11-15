package baritone.launch.mixins;

import java.lang.reflect.Method;
import java.util.Map;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.Chunk;
import org.junit.Test;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.junit.Assert.*;

public class MixinClientChunkProviderTest {
    @Test
    public void tracksLoadedChunksWhenVanillaListIsRemoved() throws Exception {
        MixinClientChunkProvider provider = new MixinClientChunkProvider() {};
        Chunk first = new Chunk(null, -2, 3);
        Method load = MixinClientChunkProvider.class.getDeclaredMethod("baritone$trackLoadedChunk", int.class, int.class, CallbackInfoReturnable.class);
        load.setAccessible(true);
        load.invoke(provider, -2, 3, new CallbackInfoReturnable<Chunk>("loadChunk", false, first));
        Map<Long, Chunk> snapshot = provider.snapshotLoadedChunks();
        assertSame(first, snapshot.get(ChunkCoordIntPair.chunkXZ2Int(-2, 3)));

        Method unload = MixinClientChunkProvider.class.getDeclaredMethod("baritone$trackUnloadedChunk", int.class, int.class, CallbackInfo.class);
        unload.setAccessible(true);
        unload.invoke(provider, -2, 3, new CallbackInfo("unloadChunk", false));
        assertTrue(provider.snapshotLoadedChunks().isEmpty());
        assertEquals(1, snapshot.size());
    }
}
