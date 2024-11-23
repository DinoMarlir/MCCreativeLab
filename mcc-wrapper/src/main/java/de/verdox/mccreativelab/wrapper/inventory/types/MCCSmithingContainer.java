package de.verdox.mccreativelab.wrapper.inventory.types;

import de.verdox.mccreativelab.wrapper.inventory.MCCMenuType;
import de.verdox.mccreativelab.wrapper.inventory.MCCMenuTypes;
import de.verdox.mccreativelab.wrapper.inventory.source.MCCBlockContainerSource;
import de.verdox.mccreativelab.wrapper.item.MCCItemStack;
import org.bukkit.inventory.Recipe;
import org.jetbrains.annotations.Nullable;

public interface MCCSmithingContainer extends MCCResultContainer<MCCBlockContainerSource> {
    /**
     * Check what item is in the result slot of this smithing table.
     *
     * @return the result item
     */
    @Nullable
    MCCItemStack getResult();

    /**
     * Set the item in the result slot of the smithing table
     *
     * @param newResult the new result item
     */
    void setResult(@Nullable MCCItemStack newResult);

    /**
     * Get the current recipe formed on the smithing table, if any.
     *
     * @return the recipe, or null if the current contents don't match any
     * recipe
     */
    @Nullable
    //TODO MCCWrapper for Recipe
    Recipe getRecipe();

    // Paper start

    /**
     * Gets the input template (first slot).
     *
     * @return input template item
     */
    default @Nullable MCCItemStack getInputTemplate() {
        return this.getItem(0);
    }

    /**
     * Sets the input template (first slot).
     *
     * @param itemStack item to set
     */
    default void setInputTemplate(@Nullable MCCItemStack itemStack) {
        this.setItem(0, itemStack);
    }

    /**
     * Gets the input equipment (second slot).
     *
     * @return input equipment item
     */
    default @Nullable MCCItemStack getInputEquipment() {
        return this.getItem(1);
    }

    /**
     * Sets the input equipment (second slot).
     *
     * @param itemStack item to set
     */
    default void setInputEquipment(@Nullable MCCItemStack itemStack) {
        this.setItem(1, itemStack);
    }

    /**
     * Gets the input mineral (third slot).
     *
     * @return input mineral item
     */
    default @Nullable MCCItemStack getInputMineral() {
        return this.getItem(2);
    }

    /**
     * Sets the input mineral (third slot).
     *
     * @param itemStack item to set
     */
    default void setInputMineral(@Nullable MCCItemStack itemStack) {
        this.setItem(2, itemStack);
    }

    @Override
    default MCCMenuType getType() {
        return MCCMenuTypes.SMITHING;
    }

    @Override
    default boolean canBeOpened() {
        return true;
    }
}