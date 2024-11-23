package de.verdox.mccreativelab.wrapper.inventory.types;

import de.verdox.mccreativelab.wrapper.inventory.MCCContainer;
import de.verdox.mccreativelab.wrapper.inventory.MCCMenuType;
import de.verdox.mccreativelab.wrapper.inventory.MCCMenuTypes;
import de.verdox.mccreativelab.wrapper.inventory.source.MCCBlockContainerSource;
import de.verdox.mccreativelab.wrapper.item.MCCItemStack;
import org.bukkit.inventory.StonecuttingRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface MCCStonecutterContainer extends MCCContainer<MCCBlockContainerSource> {

    /**
     * Gets the current index of the selected recipe.
     *
     * @return The index of the selected recipe in the stonecutter or -1 if null
     */
    int getSelectedRecipeIndex();

    /**
     * Gets a copy of all recipes currently available to the player.
     *
     * @return A copy of the {@link StonecuttingRecipe}'s currently available
     * for the player
     */
    @NotNull
    //TODO MCCWrapper for Recipe
    List<StonecuttingRecipe> getRecipes();

    /**
     * Gets the amount of recipes currently available.
     *
     * @return The amount of recipes currently available for the player
     */
    int getRecipeAmount();

    /**
     * Gets the input item.
     *
     * @return input item
     */
    @Nullable
    default MCCItemStack getInputItem() {
        return getItem(0);
    }

    /**
     * Sets the input item.
     *
     * @param itemStack item to set
     */
    default void setInputItem(@Nullable MCCItemStack itemStack) {
        setItem(0, itemStack);
    }

    /**
     * Gets the result item.
     *
     * @return result
     */
    @Nullable
    default MCCItemStack getResult() {
        return getItem(1);
    }

    /**
     * Sets the result item.
     *
     * @param itemStack item to set
     */
    default void setResult(@Nullable MCCItemStack itemStack) {
        setItem(1, itemStack);
    }

    @Override
    default MCCMenuType getType() {
        return MCCMenuTypes.STONECUTTER;
    }

    @Override
    default boolean canBeOpened() {
        return true;
    }
}