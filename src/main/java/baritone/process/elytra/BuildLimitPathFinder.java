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
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class BuildLimitPathFinder implements IElytraPathFinder {
    final int flightLevel;
    final IPlayerContext playerCtx;
    final NetherPathfinderContext netherCtx;

    public BuildLimitPathFinder(IPlayerContext ctx, NetherPathfinderContext netherCtx) {
        if (ctx == null) {
            throw new IllegalArgumentException("IPlayerContext cannot be null");
        }
        this.playerCtx = ctx;

        if (netherCtx == null) {
            throw new IllegalArgumentException("NetherPathfinderContext cannot be null");
        }

        this.flightLevel = 256 + 16;
        this.netherCtx = netherCtx;

        if (netherCtx.getMaxHeight() < 256) {
            throw new IllegalStateException("Nether pathfinder max height is below world build limit, cannot proceed");
        }
    }

    public Pair<List<BetterBlockPos>, Boolean> generateDirectPath(BetterBlockPos start, BetterBlockPos destination, int bufferDistance, int maxPathSize) {
        final LinkedList<BetterBlockPos> path = new LinkedList<>();
        final int stepDistance = 32;

        final BetterBlockPos startFixed = start.y == flightLevel ? start : new BetterBlockPos(start.x, flightLevel, start.z);
        final BetterBlockPos destinationFixed = destination.y == flightLevel ? destination : new BetterBlockPos(destination.x, flightLevel, destination.z);

        BetterBlockPos cur = startFixed;
        path.add(cur);

        while (path.size() < maxPathSize) {
            double deltaX = destinationFixed.x - cur.x;
            double deltaZ = destinationFixed.z - cur.z;
            double remainingDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            double remainingDistanceSq = deltaX * deltaX + deltaZ * deltaZ;

            if (remainingDistanceSq <= bufferDistance * bufferDistance) {
                return new Pair<>(path, true);
            } else if (remainingDistance <= stepDistance) {
                path.add(destinationFixed);
                return new Pair<>(path, true);
            }

            double stepRatio = stepDistance / remainingDistance;
            int nextX = (int) Math.round(cur.x + deltaX * stepRatio);
            int nextZ = (int) Math.round(cur.z + deltaZ * stepRatio);

            cur = new BetterBlockPos(nextX, flightLevel, nextZ);
            path.add(cur);
        }

        return new Pair<>(path, false);
    }

    public Pair<List<BetterBlockPos>, Boolean> generateTransitionUp(BetterBlockPos start, BetterBlockPos destination) {
        final double deltaX = destination.x - start.x;
        final double deltaZ = destination.z - start.z;
        final double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        final double scale = 8 / distance;
        final double stepX = deltaX * scale;
        final double stepZ = deltaZ * scale;

        final int netherMaxHeight = netherCtx.getMaxHeight() - 1;
        final int chunkX = start.x >> 4;
        final int chunkZ = start.z >> 4;

        if (!isSkyClear(chunkX, chunkZ, start.y)) {
            return new Pair<>(new LinkedList<>(), false);
        }

        LinkedList<BetterBlockPos> path = new LinkedList<>();
        final BetterBlockPos middlePos = new BetterBlockPos((chunkX << 4) + 8, netherMaxHeight + 4, (chunkZ << 4) + 8);

        for (int i = 2; i <= 2; i++) {
            BetterBlockPos next = new BetterBlockPos(
                    (int) (middlePos.x + (stepX * i)),
                    netherMaxHeight + (i * 8),
                    (int) (middlePos.z + (stepZ * i))
            );
            path.add(next);
        }

        return new Pair<>(path, true);
    }

    public Pair<List<BetterBlockPos>, Boolean> generateTransitionDown(BetterBlockPos start) {
        final int netherMaxHeight = netherCtx.getMaxHeight() - 1;
        final int chunkX = start.x >> 4;
        final int chunkZ = start.z >> 4;

        LinkedList<BetterBlockPos> path = new LinkedList<>();

        if (!isSkyClear(chunkX, chunkZ, netherMaxHeight - 16)) {
            return new Pair<>(new LinkedList<>(), false);
        }

        path.add(new BetterBlockPos((chunkX << 4) + 8, netherMaxHeight - 8, (chunkZ << 4) + 8));
        return new Pair<>(path, true);
    }

    public boolean isSkyClear(int chunkX, int chunkZ, int y) {
        if (!playerCtx.world().getChunkProvider().chunkExists(chunkX, chunkZ)) {
            return false;
        }

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int height = playerCtx.world().getHeightValue(baseX + x, baseZ + z);
                if (height > y) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public CompletableFuture<UnpackedSegment> pathFindAsync(BetterBlockPos src, BetterBlockPos dst) {
        final int netherMaxHeight = netherCtx.getMaxHeight() - 1;
        final int maxDirectPathSize = 500;

        final double maxDistance = Baritone.settings().elytraLongDistanceThreshold.value >= 32 ? (double) Baritone.settings().elytraLongDistanceThreshold.value : Double.POSITIVE_INFINITY;

        double dx = dst.x - src.x;
        double dz = dst.z - src.z;
        final double distanceXZ = dx * dx + dz * dz;
        final boolean isLongDistance = distanceXZ > maxDistance * maxDistance;
        final boolean srcAboveSupportedHeight = src.y >= netherMaxHeight;
        final boolean dstAboveSupportedHeight = dst.y >= netherMaxHeight;

        if (srcAboveSupportedHeight && dstAboveSupportedHeight) {
            Pair<List<BetterBlockPos>, Boolean> path = generateDirectPath(src, dst, 0, maxDirectPathSize);
            return CompletableFuture.completedFuture(new UnpackedSegment(path.first().stream(), path.second()));
        }

        if (isLongDistance) {
            if (srcAboveSupportedHeight) {
                Pair<List<BetterBlockPos>, Boolean> directPath = generateDirectPath(src, dst, (int) maxDistance, maxDirectPathSize);
                return CompletableFuture.completedFuture(
                        new UnpackedSegment(
                                directPath.first().stream(),
                                dstAboveSupportedHeight ? directPath.second() : false
                        )
                );
            } else {
                Pair<List<BetterBlockPos>, Boolean> transition = generateTransitionUp(src, dst);
                List<BetterBlockPos> path = transition.first();
                boolean success = transition.second();

                if (success) {
                    Pair<List<BetterBlockPos>, Boolean> directPath = generateDirectPath(path.get(path.size() - 1), dst, (int) maxDistance, maxDirectPathSize);
                    path.addAll(directPath.first());

                    return CompletableFuture.completedFuture(
                            new UnpackedSegment(
                                    path.stream(),
                                    dstAboveSupportedHeight ? directPath.second() : false
                            )
                    );
                }
            }

            final double deltaX = dst.x - src.x;
            final double deltaZ = dst.z - src.z;
            final double scale = (maxDistance / 2) / Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            final double stepX = deltaX * scale;
            final double stepZ = deltaZ * scale;
            final BetterBlockPos midDst = new BetterBlockPos((int) (src.x + stepX), netherMaxHeight, (int) (src.z + stepZ));

            return incompletePathfind(src, midDst);
        } else {
            if (srcAboveSupportedHeight) {
                Pair<List<BetterBlockPos>, Boolean> transition = generateTransitionDown(src);
                List<BetterBlockPos> path = transition.first();
                boolean success = transition.second();

                if (!success) {
                    BetterBlockPos newDest = distanceXZ > 32 ? dst : new BetterBlockPos(dst.x, 256, dst.z);
                    Pair<List<BetterBlockPos>, Boolean> directPath = generateDirectPath(src, newDest, 0, 2);
                    return CompletableFuture.completedFuture(new UnpackedSegment(directPath.first().stream(), directPath.second()));
                }

                return CompletableFuture.supplyAsync(() -> {
                    UnpackedSegment np = blockingPathFind(path.get(path.size() - 1), dst);
                    path.addAll(np.collect());
                    return new UnpackedSegment(path.stream(), np.isFinished());
                });
            }

            if (dstAboveSupportedHeight) {
                Pair<List<BetterBlockPos>, Boolean> transition = generateTransitionUp(src, dst);
                List<BetterBlockPos> path = transition.first();
                boolean success = transition.second();

                if (success) {
                    Pair<List<BetterBlockPos>, Boolean> directPath = generateDirectPath(path.get(path.size() - 1), dst, 0, maxDirectPathSize);
                    path.addAll(directPath.first());
                    return CompletableFuture.completedFuture(new UnpackedSegment(path.stream(), directPath.second()));
                }

                return netherCtx.pathFindAsync(src, new BetterBlockPos(dst.x, netherMaxHeight, dst.z));
            }

            return netherCtx.pathFindAsync(src, dst);
        }
    }

    private CompletableFuture<UnpackedSegment> incompletePathfind(BetterBlockPos src, BetterBlockPos dst) {
        return CompletableFuture.supplyAsync(() -> {
            UnpackedSegment packed = blockingPathFind(src, dst);
            return new UnpackedSegment(
                    packed.collect().stream(),
                    false
            );
        });
    }

    private UnpackedSegment blockingPathFind(BetterBlockPos src, BetterBlockPos dst) {
        try {
            return netherCtx.pathFindAsync(src, dst).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof PathCalculationException) {
                throw (PathCalculationException) cause;
            }
            throw new RuntimeException(e);
        }
    }
}
