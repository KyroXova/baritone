/* This file is part of Baritone, distributed under the LGPL-3.0-or-later. */
package baritone.api.cache;

import baritone.api.utils.BetterBlockPos;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTSizeTracker;

/** Read-only search of generated 1.7.10 Anvil chunks in an accessible region directory. */
public final class SavedTileEntitySearch {
    private static final Pattern REGION_NAME = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");
    private SavedTileEntitySearch() {}

    /**
     * Scans saved tile NBT in region files. Run off the client thread: this can be slow.
     * The radius is horizontal and in blocks. Results are saved data, possibly stale;
     * they do not cover ungenerated chunks or remote chunks unavailable on disk.
     * An I/O error aborts the scan instead of silently reporting incomplete results.
     * Interrupting the calling thread cancels the scan with InterruptedIOException.
     */
    public static List<BetterBlockPos> find(Path regionDirectory, int centerX, int centerZ, double radius,
                                             int limit, Predicate<? super NBTTagCompound> matches) throws IOException {
        Objects.requireNonNull(regionDirectory, "regionDirectory");
        Objects.requireNonNull(matches, "matches");
        if (!Double.isFinite(radius) || radius < 0 || limit < 0) throw new IllegalArgumentException("Invalid radius or limit");
        List<BetterBlockPos> found = new ArrayList<>();
        if (limit == 0) return found;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(regionDirectory, "r.*.*.mca")) {
            for (Path file : files) {
                Matcher name = REGION_NAME.matcher(file.getFileName().toString());
                if (!name.matches()) continue;
                final int regionX, regionZ;
                try {
                    regionX = Integer.parseInt(name.group(1));
                    regionZ = Integer.parseInt(name.group(2));
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid region coordinates: " + file, e);
                }
                double minX = (double) regionX * 512, minZ = (double) regionZ * 512;
                double regionDx = Math.max(Math.max(minX - centerX, centerX - (minX + 511)), 0);
                double regionDz = Math.max(Math.max(minZ - centerZ, centerZ - (minZ + 511)), 0);
                if (regionDx * regionDx + regionDz * regionDz > radius * radius) continue;
                try (RandomAccessFile region = new RandomAccessFile(file.toFile(), "r")) {
                    if (region.length() < 8192) throw new IOException("Invalid region header: " + file);
                    for (int i = 0; i < 1024; i++) {
                        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Saved world search interrupted");
                        region.seek(i * 4L);
                        int location = region.readInt();
                        if (location == 0) continue;
                        int sectors = location & 255;
                        long offset = (long) (location >>> 8) * 4096L;
                        if (sectors == 0 || offset < 8192 || offset + 5 > region.length()) throw new IOException("Invalid chunk location in " + file);
                        region.seek(offset);
                        int length = region.readInt();
                        int compression = region.readUnsignedByte();
                        if (length <= 1 || length > sectors * 4096 - 4 || offset + 4L + length > region.length()) throw new IOException("Invalid chunk length in " + file);
                        if (compression != 1 && compression != 2) throw new IOException("Unsupported chunk compression in " + file);
                        byte[] bytes = new byte[length - 1];
                        region.readFully(bytes);
                        InputStream raw = new ByteArrayInputStream(bytes);
                        try (DataInputStream input = new DataInputStream(compression == 1 ? new GZIPInputStream(raw) : new InflaterInputStream(raw))) {
                            NBTTagCompound chunk;
                            try {
                                chunk = CompressedStreamTools.func_152456_a(input, new NBTSizeTracker(64L * 1024 * 1024));
                            } catch (RuntimeException e) {
                                throw new IOException("Invalid chunk NBT in " + file, e);
                            }
                            NBTTagList tiles = chunk.getCompoundTag("Level").getTagList("TileEntities", 10);
                            for (int t = 0; t < tiles.tagCount(); t++) {
                                NBTTagCompound tile = tiles.getCompoundTagAt(t);
                                if (!tile.hasKey("x", 99) || !tile.hasKey("y", 99) || !tile.hasKey("z", 99)) continue;
                                int x = tile.getInteger("x"), y = tile.getInteger("y"), z = tile.getInteger("z");
                                double dx = (double) x - centerX, dz = (double) z - centerZ;
                                if (dx * dx + dz * dz <= radius * radius && matches.test(tile)) {
                                    found.add(new BetterBlockPos(x, y, z));
                                    if (found.size() == limit) return found;
                                }
                            }
                        }
                    }
                }
            }
        }
        return found;
    }
}
