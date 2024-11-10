package de.verdox.mccreativelab.wrapper.block;

import de.verdox.mccreativelab.wrapper.world.MCCLocation;
import de.verdox.mccreativelab.wrapper.MCCKeyedWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Describes a block type and its block states
 */
public interface MCCBlockType extends MCCKeyedWrapper {

    /**
     * Changes a block at the provided location to this block type
     *
     * @param location the location to change the block at
     */
    default void setBlock(@NotNull MCCLocation location){
        setBlock(location, false);
    }

    /**
     * Changes a block at the provided location to this block type
     *
     * @param location the location to change the block at
     * @param applyPhysics whether a block update is triggered
     */
    default void setBlock(@NotNull MCCLocation location, boolean applyPhysics){
        location.world().setBlock(this, location, applyPhysics);
    }

    /**
     * Returns all defined block states for this block type
     *
     * @return a list of block states
     */
    List<MCCBlockState> getAllBlockStates();

    /**
     * Returns the sound group of this block type
     *
     * @return the sound group
     */
    MCCBlockSoundGroup getSoundGroup();
}
