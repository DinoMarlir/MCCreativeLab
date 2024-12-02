package de.verdox.mccreativelab.util.player.inventory;

import de.verdox.mccreativelab.world.item.FakeItem;
import de.verdox.mccreativelab.wrapper.item.MCCItemStack;
import de.verdox.mccreativelab.wrapper.item.MCCItemType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public interface PlayerInventoryCacheStrategy {
    /**
     * Called when a new item is cached
     * @param slot - The slot of the new item
     * @param stack - The item
     */
    void cacheItemInSlot(int slot, MCCItemStack stack);

    /**
     * Called when an old item is removed from cache
     * @param slot - The slot of the new item
     * @param stack - The item
     */
    void removeSlotFromCache(int slot, MCCItemStack stack);

    class CachedAmounts implements PlayerInventoryCacheStrategy {
        private final Map<MCCItemType, Integer> cachedAmounts = new HashMap<>();

        public int getAmount(MCCItemType mccItemType){
            return cachedAmounts.getOrDefault(mccItemType, 0);
        }

        public int getAmount(MCCItemStack stack){
            return getAmount(stack.getType());
        }

        public int getAmount(FakeItem fakeItem){
            return getAmount(fakeItem.asItemType());
        }

        @Override
        public void cacheItemInSlot(int slot, MCCItemStack stack) {
            MCCItemType type = stack.getType();
            int newAmount = stack.getAmount();
            if (cachedAmounts.containsKey(type))
                newAmount += cachedAmounts.get(type);
            cachedAmounts.put(type, newAmount);
        }

        @Override
        public void removeSlotFromCache(int slot, MCCItemStack stack) {
            MCCItemType type = stack.getType();
            if (!cachedAmounts.containsKey(type))
                return;
            int newAmount = cachedAmounts.get(type);
            newAmount -= stack.getAmount();
            if (newAmount > 0)
                cachedAmounts.put(type, newAmount);
            else
                cachedAmounts.remove(type);
        }
    }

    class CachedSlots implements PlayerInventoryCacheStrategy {
        private final Map<MCCItemType, Set<Integer>> dataToSlotMapping = new HashMap<>();
        private final Map<Integer, MCCItemType> slotToDataMapping = new HashMap<>();

        @Override
        public void cacheItemInSlot(int slot, MCCItemStack stack) {
            MCCItemType mccItemType = stack.getType();
            dataToSlotMapping.computeIfAbsent(mccItemType, v -> new HashSet<>()).add(slot);
            slotToDataMapping.put(slot, mccItemType);
        }

        @Override
        public void removeSlotFromCache(int slot, MCCItemStack stack) {
            if (!slotToDataMapping.containsKey(slot))
                return;
            MCCItemType mccItemType = slotToDataMapping.get(slot);
            slotToDataMapping.remove(slot);
            if (!dataToSlotMapping.containsKey(mccItemType))
                return;
            dataToSlotMapping.get(mccItemType).remove(slot);
        }
    }
}
