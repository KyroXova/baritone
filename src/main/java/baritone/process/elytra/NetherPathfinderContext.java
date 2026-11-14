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

package baritone.process.elytra;

import baritone.Baritone;
import baritone.api.event.events.BlockChangeEvent;
import baritone.api.utils.BetterBlockPos;
import dev.babbaj.pathfinder.NetherPathfinder;
import dev.babbaj.pathfinder.Octree;
import dev.babbaj.pathfinder.PathSegment;
import net.minecraft.init.Blocks;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import sun.misc.Unsafe;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Brady
 */
public final class NetherPathfinderContext implements IElytraPathFinder {

    private static final Unsafe UNSAFE;
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    public final ReentrantReadWriteLock.ReadLock readLock = rwl.readLock();
    public final ReentrantReadWriteLock.WriteLock writeLock = rwl.writeLock();
    private final int maxHeight;

    final long context;
    private final long seed;
    private final ExecutorService writeExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService readExecutor = Executors.newSingleThreadExecutor();
    private final int dimensionId;
    final int minY;
    private final BlockStateOctreeInterface boi;

    public NetherPathfinderContext(long seed, Path cache, World world) {
        this.dimensionId = world.provider.dimensionId;
        this.minY = 0;
        final int dim;
        if (this.dimensionId == -1) {
            dim = NetherPathfinder.DIMENSION_NETHER;
        } else if (this.dimensionId == 1) {
            dim = NetherPathfinder.DIMENSION_END;
        } else {
            dim = NetherPathfinder.DIMENSION_OVERWORLD;
        }
        int height = 256;
        if (!Baritone.settings().elytraAllowAboveRoof.value && dim == NetherPathfinder.DIMENSION_NETHER) {
            height = Math.min(height, 128);
        }
        this.maxHeight = height;
        this.context = NetherPathfinder.newContext(seed, cache != null ? cache.toString() : null, dim, height, Baritone.settings().elytraCustomAllocator.value);
        this.seed = seed;
        this.boi = new BlockStateOctreeInterface(this);
    }

    public boolean hasChunk(ChunkCoordIntPair pos) {
        return NetherPathfinder.hasChunkFromJava(this.context, pos.chunkXPos, pos.chunkZPos);
    }

    public void queueCacheCulling(int chunkX, int chunkZ, int maxDistanceBlocks) {
        this.writeExecutor.execute(() -> {
            writeLock.lock();
            try {
                this.boi.chunkPtr = 0L;
                NetherPathfinder.cullFarChunks(this.context, chunkX, chunkZ, maxDistanceBlocks);
            } finally {
                writeLock.unlock();
            }
        });
    }

    public void queueForPacking(final Chunk chunkIn) {
        final SoftReference<Chunk> ref = new SoftReference<>(chunkIn);
        this.writeExecutor.execute(() -> {
            final Chunk chunk = ref.get();
            if (chunk != null) {
                writeLock.lock();
                try {
                    this.boi.chunkPtr = 0L;
                    long ptr = NetherPathfinder.allocateAndInsertChunk(this.context, chunk.xPosition, chunk.zPosition);
                    writeChunkData(chunk, ptr);
                } finally {
                    writeLock.unlock();
                }
            }
        });
    }

    public void queueBlockUpdate(BlockChangeEvent event) {
        this.writeExecutor.execute(() -> {
            ChunkCoordIntPair chunkPos = event.getChunkPos();
            writeLock.lock();
            try {
                long ptr = NetherPathfinder.getChunk(this.context, chunkPos.chunkXPos, chunkPos.chunkZPos);
                if (ptr == 0) return;
                event.getBlocks().forEach(pair -> {
                    BetterBlockPos pos = pair.first();
                    if (pos.y < 0 || pos.y >= 384) return;
                    boolean isSolid = pair.second().getBlock() != Blocks.air;
                    Octree.setBlock(ptr, pos.x & 15, pos.y, pos.z & 15, isSolid);
                });
            } finally {
                writeLock.unlock();
            }
        });
    }

    @Override
    public CompletableFuture<UnpackedSegment> pathFindAsync(final BetterBlockPos src, final BetterBlockPos dst) {
        final BetterBlockPos adjustedSrc = src.below(minY);
        final BetterBlockPos adjustedDst = dst.below(minY);
        boolean generate = Baritone.settings().elytraPredictTerrain.value && this.dimensionId == -1;
        Lock l = generate ? writeLock : readLock;
        ExecutorService exec = generate ? writeExecutor : readExecutor;
        return CompletableFuture.supplyAsync(() -> {
            l.lock();
            try {
                final PathSegment segment = NetherPathfinder.pathFind(
                        this.context,
                        adjustedSrc.x, adjustedSrc.y, adjustedSrc.z,
                        adjustedDst.x, adjustedDst.y, adjustedDst.z,
                        !Baritone.settings().elytraAllowTightSpaces.value,
                        false,
                        10000,
                        !generate,
                        8.0
                );
                if (segment == null) {
                    throw new PathCalculationException("Path calculation failed");
                }

                return new UnpackedSegment(UnpackedSegment.from(segment).collect().stream().map(pos -> pos.above(minY)), segment.finished);
            } finally {
                l.unlock();
            }
        }, exec);
    }

    public boolean raytrace(final double startX, final double startY, final double startZ,
                            final double endX, final double endY, final double endZ) {
        final double adjustedStartY = startY - this.minY;
        final double adjustedEndY = endY - this.minY;
        return NetherPathfinder.isVisible(this.context, NetherPathfinder.CACHE_MISS_SOLID, startX, adjustedStartY, startZ, endX, adjustedEndY, endZ);
    }

    public boolean raytrace(final Vec3 start, final Vec3 end) {
        final double adjustedStartY = start.yCoord - this.minY;
        final double adjustedEndY = end.yCoord - this.minY;
        return NetherPathfinder.isVisible(this.context, NetherPathfinder.CACHE_MISS_SOLID, start.xCoord, adjustedStartY, start.zCoord, end.xCoord, adjustedEndY, end.zCoord);
    }

    public boolean raytrace(final int count, final double[] src, final double[] dst, final int visibility) {
        if (src.length != count * 3 || dst.length != count * 3) {
            throw new IllegalArgumentException("Bad array lengths");
        }

        for (int i = 1; i < src.length; i += 3) {
            src[i] -= this.minY;
            dst[i] -= this.minY;
        }

        switch (visibility) {
            case Visibility.ALL:
                return NetherPathfinder.isVisibleMulti(this.context, NetherPathfinder.CACHE_MISS_SOLID, count, src, dst, false) == -1;
            case Visibility.NONE:
                return NetherPathfinder.isVisibleMulti(this.context, NetherPathfinder.CACHE_MISS_SOLID, count, src, dst, true) == -1;
            case Visibility.ANY:
                return NetherPathfinder.isVisibleMulti(this.context, NetherPathfinder.CACHE_MISS_SOLID, count, src, dst, true) != -1;
            default:
                throw new IllegalArgumentException("lol");
        }
    }

    public void raytrace(final int count, final double[] src, final double[] dst, final boolean[] hitsOut, final double[] hitPosOut) {
        if (src.length != count * 3 || dst.length != count * 3) {
            throw new IllegalArgumentException("Bad array lengths");
        }

        for (int i = 1; i < src.length; i += 3) {
            src[i] -= this.minY;
            dst[i] -= this.minY;
        }

        NetherPathfinder.raytrace(this.context, NetherPathfinder.CACHE_MISS_SOLID, count, src, dst, hitsOut, hitPosOut);
    }

    public boolean passable(int x, int y, int z) {
        return !this.boi.get0(x, y, z);
    }

    public void cancel() {
        NetherPathfinder.cancel(this.context);
    }

    public void destroy() {
        this.cancel();
        this.readExecutor.shutdownNow();
        this.writeExecutor.shutdownNow();

        try {
            while (!this.readExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {}
            while (!this.writeExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {}
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        NetherPathfinder.freeContext(this.context);
    }

    public long getSeed() {
        return this.seed;
    }

    public void acquireReadLock() {
        this.readLock.lock();
    }

    public boolean tryAcquireReadLock() {
        return this.readLock.tryLock();
    }

    public void releaseReadLock() {
        this.readLock.unlock();
    }

    public int getMaxHeight() {
        return this.maxHeight;
    }

    private static void writeChunkData(Chunk chunk, long chunkPtr) {
        try {
            ExtendedBlockStorage[] chunkInternalStorageArray = chunk.getBlockStorageArray();
            final int maxSections = Math.min(chunkInternalStorageArray.length, 16);
            for (int y0 = 0; y0 < maxSections; y0++) {
                final ExtendedBlockStorage extendedblockstorage = chunkInternalStorageArray[y0];
                if (extendedblockstorage == null || extendedblockstorage.isEmpty()) {
                    continue;
                }
                byte[] lsb = extendedblockstorage.getBlockLSBArray();
                NibbleArray msbArray = extendedblockstorage.getBlockMSBArray();
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            int idx = y << 8 | z << 4 | x;
                            int id = lsb[idx] & 0xFF;
                            if (msbArray != null) {
                                id |= msbArray.get(x, y, z) << 8;
                            }
                            // 0 is air, 39 brown mushroom, 40 red mushroom
                            if (id != 0 && id != 39 && id != 40) {
                                Octree.setBlock(chunkPtr, x, (y0 << 4) + y, z, true);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static boolean isSupported() {
        return NetherPathfinder.isThisSystemSupported();
    }

    public static final class Visibility {
        public static final int ALL = 0;
        public static final int NONE = 1;
        public static final int ANY = 2;
        private Visibility() {}
    }
}
