package baritone.api.cache;

import baritone.api.utils.BetterBlockPos;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class SavedTileEntitySearchTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void findsSavedTileByNbtAndRadius() throws Exception {
        Path dir = folder.newFolder().toPath();
        NBTTagCompound tile = new NBTTagCompound();
        tile.setString("id", "example:marker");
        tile.setInteger("x", -15); tile.setInteger("y", 35); tile.setInteger("z", 20);
        NBTTagList tiles = new NBTTagList();
        tiles.appendTag(tile);
        NBTTagCompound level = new NBTTagCompound();
        level.setTag("TileEntities", tiles);
        NBTTagCompound root = new NBTTagCompound();
        root.setTag("Level", level);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(new DeflaterOutputStream(bytes))) {
            CompressedStreamTools.write(root, data);
        }
        byte[] compressed = bytes.toByteArray();
        try (RandomAccessFile region = new RandomAccessFile(dir.resolve("r.-1.0.mca").toFile(), "rw")) {
            region.setLength(12288);
            region.seek(63 * 4L); region.writeInt((2 << 8) | 1);
            region.seek(8192); region.writeInt(compressed.length + 1); region.writeByte(2); region.write(compressed);
        }
        List<BetterBlockPos> found = SavedTileEntitySearch.find(dir, 0, 0, 30, 10, tag -> "example:marker".equals(tag.getString("id")));
        assertEquals(1, found.size());
        assertEquals(new BetterBlockPos(-15, 35, 20), found.get(0));
        assertTrue(SavedTileEntitySearch.find(dir, 0, 0, 10, 10, tag -> true).isEmpty());
        assertTrue(SavedTileEntitySearch.find(dir, 0, 0, 30, 10, tag -> false).isEmpty());
    }

    @Test
    public void skipsRegionsOutsideSearchRadius() throws Exception {
        Path dir = folder.newFolder().toPath();
        java.nio.file.Files.createFile(dir.resolve("r.50.50.mca"));
        assertTrue(SavedTileEntitySearch.find(dir, 0, 0, 30, 10, tag -> true).isEmpty());
    }

    @Test(expected = java.io.IOException.class)
    public void reportsTruncatedRegions() throws Exception {
        Path dir = folder.newFolder().toPath();
        java.nio.file.Files.createFile(dir.resolve("r.0.0.mca"));
        SavedTileEntitySearch.find(dir, 0, 0, 30, 10, tag -> true);
    }
}
