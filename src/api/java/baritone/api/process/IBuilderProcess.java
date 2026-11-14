/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.api.process;

import baritone.api.schematic.ISchematic;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockState;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * @author Brady
 * @since 1/15/2019
 */
public interface IBuilderProcess extends IBaritoneProcess {

    /**
     * Requests a build for the specified schematic, labeled as specified, with the specified origin.
     *
     * @param name      A user-friendly name for the schematic
     * @param schematic The object representation of the schematic
     * @param origin    The origin position of the schematic being built
     */
    void build(String name, ISchematic schematic, BetterBlockPos origin);

    /**
     * Requests a build for the specified schematic, labeled as specified, with the specified origin.
     *
     * @param name      A user-friendly name for the schematic
     * @param schematic The file path of the schematic
     * @param origin    The origin position of the schematic being built
     * @return Whether or not the schematic was able to load from file
     */
    boolean build(String name, File schematic, BetterBlockPos origin);

    @Deprecated
    default boolean build(String schematicFile, BetterBlockPos origin) {
        File file = new File(new File(Minecraft.getMinecraft().mcDataDir, "schematics"), schematicFile);
        return build(schematicFile, file, origin);
    }

    void buildOpenSchematic();

    void buildOpenLitematic(int i);

    void pause();

    boolean isPaused();

    void resume();

    void clearArea(BetterBlockPos corner1, BetterBlockPos corner2);

    /**
     * @return A list of block states that are estimated to be placeable by this builder process. You can use this in
     * schematics, for example, to pick a state that the builder process will be happy with, because any variation will
     * cause it to give up. This is updated every tick, but only while the builder process is active.
     */
    List<BlockState> getApproxPlaceable();

    /**
     * Returns the lower bound of the current mining layer if mineInLayers is true.
     * If mineInLayers is false, this will return an empty optional.
     * @return The lower bound of the current mining layer
     */
    Optional<Integer> getMinLayer();

    /**
     * Returns the upper bound of the current mining layer if mineInLayers is true.
     * If mineInLayers is false, this will return an empty optional.
     * @return The upper bound of the current mining layer
     */
    Optional<Integer> getMaxLayer();
}
