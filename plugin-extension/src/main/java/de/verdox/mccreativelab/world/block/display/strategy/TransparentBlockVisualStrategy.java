package de.verdox.mccreativelab.world.block.display.strategy;

import de.verdox.mccreativelab.world.block.FakeBlock;
import de.verdox.mccreativelab.world.block.display.TransparentFullBlockEntityDisplay;
import de.verdox.mccreativelab.wrapper.block.MCCBlockFace;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.jetbrains.annotations.Nullable;

public class TransparentBlockVisualStrategy extends FakeBlockVisualStrategy<TransparentBlockVisualStrategy.FakeBlockFullDisplay> {
    public static final TransparentBlockVisualStrategy INSTANCE = new TransparentBlockVisualStrategy();

    @Override
    public void spawnFakeBlockDisplay(Block block, FakeBlock.FakeBlockState fakeBlockState) {
        if (!(fakeBlockState.getFakeBlockDisplay() instanceof TransparentFullBlockEntityDisplay transparentFullBlockEntityDisplay))
            return;
        if (transparentFullBlockEntityDisplay.getFullBlockFakeItem() == null)
            return;
        FakeBlockFullDisplay fakeBlockFullDisplay = getOrCreateFakeBlockDisplayData(block);

        Location blockCenter = block.getLocation().clone().add(0.5, 0.5, 0.5);

        ItemDisplay itemDisplay = (ItemDisplay) block.getWorld().spawnEntity(blockCenter, EntityType.ITEM_DISPLAY);
        setupItemDisplayNBT(itemDisplay, transparentFullBlockEntityDisplay.getFullBlockFakeItem(), block, fakeBlockState);
        fakeBlockFullDisplay.setStoredItemDisplay(itemDisplay);
    }

    @Override
    public void blockUpdate(Block block, FakeBlock.FakeBlockState fakeBlockState, MCCBlockFace direction, BlockData neighbourBlockData) {
        blockUpdateRemovalLogic(block, fakeBlockState, direction, neighbourBlockData);
    }

    @Override
    protected void loadItemDisplayAsBlockDisplay(PotentialItemDisplay potentialItemDisplay) {
        if (!(potentialItemDisplay.storedFakeBlockState().getFakeBlockDisplay() instanceof TransparentFullBlockEntityDisplay transparentFullBlockEntityDisplay))
            return;
        Block block = potentialItemDisplay.block();
        ItemDisplay itemDisplay = potentialItemDisplay.itemDisplay();
        FakeBlock.FakeBlockState fakeBlockState = potentialItemDisplay.storedFakeBlockState();

        if (itemDisplay == null || block == null || transparentFullBlockEntityDisplay.getFullBlockFakeItem() == null)
            return;

        setupItemDisplayNBT(itemDisplay, transparentFullBlockEntityDisplay.getFullBlockFakeItem(), block, fakeBlockState);
        getOrCreateFakeBlockDisplayData(block).setStoredItemDisplay(itemDisplay);
    }

    @Override
    protected FakeBlockFullDisplay newData() {
        return new FakeBlockFullDisplay();
    }

    protected static class FakeBlockFullDisplay extends FakeBlockDisplayData {
        @Nullable
        private ItemDisplay storedItemDisplay;

        FakeBlockFullDisplay() {
        }

        public void setStoredItemDisplay(ItemDisplay storedItemDisplay) {
            if (this.storedItemDisplay != null && !this.storedItemDisplay.equals(storedItemDisplay))
                safelyRemoveItemDisplay(this.storedItemDisplay);
            this.storedItemDisplay = storedItemDisplay;
        }

        @Override
        protected void moveTo(int x, int y, int z) {

        }

        @Override
        public void destroy() {
            setStoredItemDisplay(null);
        }
    }
}
