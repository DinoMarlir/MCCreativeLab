package de.verdox.mccreativelab.wrapper.item.alchemy;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a MCC Potion
 */
public class MCCPotion {

    @Nullable
    private final String name;

    public MCCPotion(@Nullable String baseName) {
        this.name = baseName;
    }

    /**
     * Gets the name of this potion
     * @return name of this potion
     */
    @Nullable
    public String getName() {
        return name;
    }
}
