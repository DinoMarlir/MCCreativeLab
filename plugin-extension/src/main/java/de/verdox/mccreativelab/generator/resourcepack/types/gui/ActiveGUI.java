package de.verdox.mccreativelab.generator.resourcepack.types.gui;

import de.verdox.mccreativelab.MCCreativeLabExtension;
import de.verdox.mccreativelab.generator.resourcepack.types.gui.element.active.ActiveGUIElement;
import de.verdox.mccreativelab.generator.resourcepack.types.rendered.ActiveComponentRendered;
import de.verdox.mccreativelab.util.player.fakeinv.FakeInventory;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ActiveGUI extends ActiveComponentRendered<ActiveGUI, CustomGUIBuilder> implements Listener {
    private long lastShift = System.currentTimeMillis();
    private static final long SHIFT_COOLDOWN_MILLIS = 20;
    private final Map<String, Object> tempData = new HashMap<>();
    private final Map<String, ActiveGUIElement<?>> activeGUIElements = new HashMap<>();
    private final Map<Integer, ActiveGUIElement<?>> guiElementsBySlot = new HashMap<>();
    private boolean isOpen;
    private Inventory inventory;
    private boolean isUpdating;
    private BukkitTask updateTask;

    public ActiveGUI(Player player, CustomGUIBuilder customGUIBuilder, @Nullable Consumer<ActiveGUI> initialSetup) {
        super(player, customGUIBuilder);
        Bukkit.getPluginManager().registerEvents(this, MCCreativeLabExtension.getInstance());

        customGUIBuilder.guiElements.forEach((s, guiElement) -> {
            var activeElement = guiElement.toActiveElement(this);
            activeGUIElements.put(s, activeElement);
        });

        forceUpdate();

        if (initialSetup != null) {
            initialSetup.accept(this);
            forEachElementBehavior((activeGUIRenderedRenderedElementBehavior, rendered) -> activeGUIRenderedRenderedElementBehavior.onOpen(this, getPlayer(), rendered));
            forEachGUIElementBehavior((guiElementBehavior, activeGUIElement) -> guiElementBehavior.onOpen(this, getPlayer(), activeGUIElement));
            forceUpdate();
        }

        if (customGUIBuilder.onOpen != null) {
            customGUIBuilder.onOpen.accept(this);
            forceUpdate();
        }

        if (customGUIBuilder.updateInterval > 0) {
            updateTask = Bukkit.getScheduler().runTaskTimer(MCCreativeLabExtension.getInstance(), () -> {
                if (isOpen) {
                    if (customGUIBuilder.whileOpen != null) customGUIBuilder.whileOpen.accept(this);

                    forEachElementBehavior((activeGUIRenderedRenderedElementBehavior, rendered) -> activeGUIRenderedRenderedElementBehavior.whileOpen(this, getPlayer(), rendered));
                    forEachGUIElementBehavior((guiElementBehavior, activeGUIElement) -> guiElementBehavior.whileOpen(this, getPlayer(), activeGUIElement));
                }
            }, 0, customGUIBuilder.updateInterval);
        }
    }

    public final void forEachGUIElementBehavior(BiConsumer<GUIElementBehavior<ActiveGUIElement<?>>, ActiveGUIElement<?>> forEach) {
        this.activeGUIElements.forEach((s, activeGUIElement) -> {
            GUIElementBehavior<ActiveGUIElement<?>> guiElementBehavior = (GUIElementBehavior<ActiveGUIElement<?>>) getComponentRendered()
                .getGuiElementBehaviors().getOrDefault(activeGUIElement.getGuiElement(), null);
            if (guiElementBehavior != null)
                forEach.accept(guiElementBehavior, activeGUIElement);
        });
        forceUpdate();
    }

    public final <H extends ActiveGUIElement<?>> boolean editGUIElement(String id, Class<? extends H> type, Consumer<H> execution) {
        return edit(id, activeGUIElements, type, h -> {
            h.setVisible(true);
            execution.accept(h);
        });
    }

    public Inventory getVanillaInventory() {
        return inventory;
    }

    public <R> ActiveGUI addTemporaryData(String key, @Nullable R value) {
        if (value == null)
            tempData.remove(key);
        else
            tempData.put(key, value);
        return this;
    }

    @Nullable
    public <R> R getTemporaryDataOrDefault(String key, Class<? extends R> type, R defaultVal) {
        return type.cast(tempData.getOrDefault(key, defaultVal));
    }

    @EventHandler
    private void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().equals(inventory)))
            return;
        if (isUpdating) return;
        closeActiveGUI();
    }

    private void closeActiveGUI() {
        if (!isOpen) return;
        if (updateTask != null) {
            updateTask.cancel();
        }
        isOpen = false;
        if (getComponentRendered().onClose != null)
            getComponentRendered().onClose.accept(this);

        HandlerList.unregisterAll(this);

        FakeInventory.stopFakeInventoryOfPlayer(getPlayer());

        getPlayer().updateInventory();
    }

    @EventHandler
    private void onClick(InventoryClickEvent e) {
        if (!e.getView().getTopInventory().equals(this.inventory))
            return;

        if (isUpdating) {
            e.setCancelled(true);
            return;
        }

        ActiveGUIElement<?> activeGUIElement = guiElementsBySlot.getOrDefault(e.getRawSlot(), null);
        if ((getComponentRendered().isSlotBlocked(e.getRawSlot()) && this.inventory.equals(e.getClickedInventory())) || activeGUIElement != null) {
            e.setCancelled(true);
            activeGUIElement.onClick(e, e.getRawSlot() % 9, e.getRawSlot() / 9);
        }
        // Prevent inventory clicks if is using player slots
        if (getComponentRendered().isUsePlayerSlots() && Objects.equals(e.getClickedInventory(), e.getView()
                                                                                                  .getBottomInventory()))
            e.setCancelled(true);

        if (e.isShiftClick() && e.getView().getBottomInventory().equals(e.getClickedInventory())) {
            if (getComponentRendered().isUsePlayerSlots() || System.currentTimeMillis() - lastShift < SHIFT_COOLDOWN_MILLIS) {
                e.setCancelled(true);
                return;
            }
            lastShift = System.currentTimeMillis();

            shiftItemToInventory(e.getView()
                                  .getBottomInventory(), this.inventory, e.getSlot(), getComponentRendered().getBlockedSlots());
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler
    private void onClick(InventoryDragEvent e) {
        if (!(e.getInventory().equals(inventory)))
            return;
        if (isUpdating) {
            e.setCancelled(true);
            return;
        }
        ;
        var rawSlotUsed = e.getRawSlots().stream().anyMatch(getComponentRendered()::isSlotBlocked);
        if (rawSlotUsed)
            e.setCancelled(true);
    }

    public void placeGuiElementInSlot(int slotIndex, @Nullable ActiveGUIElement<?> activeGUIElement) {
        if (activeGUIElement != null && !this.equals(activeGUIElement.getActiveGUI()))
            throw new IllegalArgumentException("Trying to add gui element that does not belong to this gui.");
        if (activeGUIElement != null)
            guiElementsBySlot.put(slotIndex, activeGUIElement);
        else
            guiElementsBySlot.remove(slotIndex);
    }

    public @Nullable ActiveGUIElement<?> getGUIElementAtIndex(int slotIndex) {
        return guiElementsBySlot.getOrDefault(slotIndex, null);
    }

    @Override
    protected void doUpdate() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(MCCreativeLabExtension.getInstance(), this::doUpdate);
            return;
        }

        Component rendering = render();
        Player player = getPlayer();

        player.getOpenInventory().getCursor();
        var itemAtCursor = player.getOpenInventory().getCursor().clone();

        isUpdating = true;
        try {
            if (this.inventory != null) {
                if (!isOpen) return;
                else if (MCCreativeLabExtension.isServerSoftware()) {
                    player.openInventory(this.inventory, rendering);
                    return;
                }
            }

            ItemStack[] oldContent;
            if (this.inventory != null) {
                if (!isOpen)
                    return;
                oldContent = this.inventory.getContents();
            } else oldContent = null;

            if (getComponentRendered().getType() != null)
                this.inventory = Bukkit.createInventory(player, getComponentRendered().getType(), rendering);
            else
                this.inventory = Bukkit.createInventory(player, getComponentRendered().getChestSize() * 9, rendering);

            if (oldContent != null)
                this.inventory.setContents(oldContent);

            openUpdatedInventory(player, itemAtCursor);
        } finally {
            isUpdating = false;
        }
    }

    private void openUpdatedInventory(Player player, ItemStack itemAtCursor) {
        if (!isOpen && !FakeInventory.hasFakeInventory(player)) {
            isOpen = true;
            FakeInventory.setFakeInventoryOfPlayer(player);
        }

        var view = player.openInventory(this.inventory);

        if (itemAtCursor != null) {
            if (view != null && !itemAtCursor.getType().isAir() && !getComponentRendered().isUsePlayerSlots()) {
                player.getInventory().removeItem(itemAtCursor);
                view.setCursor(itemAtCursor);
                player.updateInventory();
            }
        }
    }

    public void shiftItemToInventory(Inventory sourceInventory, Inventory targetInventory, int sourceSlot, Set<Integer> blockedSlots) {
        int targetSlot = 0;

        ItemStack itemStack = sourceInventory.getItem(sourceSlot);

        // Überprüfe, ob das Item im Quell-Slot vorhanden ist
        if (itemStack == null) {
            return; // Beende die Funktion, wenn kein Item im Quell-Slot ist
        }

        // Überprüfe, ob alle Slots blockiert sind
        boolean allSlotsBlocked = true;
        for (int i = 0; i < targetInventory.getSize(); i++) {
            if (!blockedSlots.contains(i)) {
                allSlotsBlocked = false;
                break;
            }
        }

        if (allSlotsBlocked)
            return; // Beende die Funktion, wenn alle Slots blockiert sind

        // Iteriere über alle Slots im Ziel-Inventar
        for (int i = 0; i < targetInventory.getSize(); i++) {
            // Überprüfe, ob der aktuelle Slot blockiert ist
            if (blockedSlots.contains(i))
                continue; // Überspringe den aktuellen Slot und gehe zum nächsten

            ItemStack currentSlotItem = targetInventory.getItem(i);

            // Überprüfe, ob der aktuelle Slot leer ist
            if (currentSlotItem == null) {
                targetInventory.setItem(i, itemStack);
                sourceInventory.setItem(sourceSlot, null); // Entferne das Item aus dem Quell-Inventar
                return; // Das Item wurde verschoben, beende die Funktion
            }

            // Überprüfe, ob das Item im aktuellen Slot vom gleichen Typ und stapelbar ist
            if (currentSlotItem.isSimilar(itemStack) && currentSlotItem.getAmount() < currentSlotItem.getMaxStackSize()) {
                int spaceLeft = currentSlotItem.getMaxStackSize() - currentSlotItem.getAmount();
                int amountToMove = Math.min(spaceLeft, itemStack.getAmount());

                currentSlotItem.setAmount(currentSlotItem.getAmount() + amountToMove);
                itemStack.setAmount(itemStack.getAmount() - amountToMove);

                if (itemStack.getAmount() == 0) {
                    sourceInventory.setItem(sourceSlot, null); // Entferne das Item aus dem Quell-Inventar
                    return; // Das gesamte Item wurde verschoben, beende die Funktion
                }
            }

            targetSlot++;
        }

        // Falls alle Slots blockiert waren oder kein passender Slot gefunden wurde,
        // wird das Item nicht verschoben
    }
}
