package de.verdox.mccreativelab;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import de.verdox.mccreativelab.event.MCCreativeLabReloadEvent;
import de.verdox.mccreativelab.generator.resourcepack.ResourcePackMapper;
import de.verdox.mccreativelab.generator.resourcepack.types.gui.GUIUtil;
import de.verdox.mccreativelab.generator.resourcepack.types.hud.renderer.HudRenderer;
import de.verdox.mccreativelab.generator.resourcepack.types.hud.renderer.HudRendererImpl;
import de.verdox.mccreativelab.serialization.MCCSerializer;
import de.verdox.mccreativelab.util.ComponentUtil;
import de.verdox.mccreativelab.util.player.inventory.PlayerInventoryCacheStrategy;
import de.verdox.mccreativelab.util.player.inventory.PlayerInventoryCachedData;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

public class ExtensionFeatures implements Listener {
    private static final HudRendererImpl hudRenderer = new HudRendererImpl();
    public void onLoad(){
        MCCSerializer.init();
    }

    public void onEnable(){
        hudRenderer.start();
        ResourcePackMapper resourcePackMapper = MCCreativeLabExtension.getCustomResourcePack().getResourcePackMapper();
        MCCreativeLabExtension.registerRegistryLookupCommand("hud", resourcePackMapper.getHudsRegistry(), hudRenderer::getOrStartActiveHud);
        MCCreativeLabExtension.registerRegistryLookupCommand("gui", resourcePackMapper.getGuiRegistry(), (player, customGUIBuilder) -> customGUIBuilder.createMenuForPlayer(player));
        MCCreativeLabExtension.registerRegistryLookupCommand("menu", resourcePackMapper.getMenuRegistry(), (player, customMenu) -> customMenu.createMenuForPlayer(player));
        ComponentUtil.install();
        GUIUtil.install();

        Bukkit.getPluginManager().registerEvents(new PlayerInventoryCachedData.Listener(), MCCreativeLabExtension.getInstance());
        PlayerInventoryCachedData.register(PlayerInventoryCacheStrategy.CachedAmounts.class, PlayerInventoryCacheStrategy.CachedAmounts::new);
        PlayerInventoryCachedData.register(PlayerInventoryCacheStrategy.CachedSlots.class, PlayerInventoryCacheStrategy.CachedSlots::new);
    }

    public void onDisable(){
        this.hudRenderer.interrupt();
        try {
            this.hudRenderer.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void onServerLoad(ServerLoadEvent.LoadType loadType){
        if(loadType.equals(ServerLoadEvent.LoadType.STARTUP)){
            if (MCCreativeLabExtension.getInstance().createResourcePack())
                MCCreativeLabExtension.getInstance().getResourcePackFileHoster()
                                      .sendDefaultResourcePackToPlayers(Bukkit.getOnlinePlayers());
            Bukkit.getLogger().info("MCCreativeLabExtension started");
        }
        else {
            MCCreativeLabExtension.getInstance().buildPackAndZipFiles(true);
            MCCreativeLabExtension.getInstance().getResourcePackFileHoster().sendDefaultResourcePackToPlayers(Bukkit.getOnlinePlayers());
            Bukkit.getLogger().info("MCCreativeLabExtension reloaded");
        }
    }

    @EventHandler
    public void onServerTick(ServerTickEndEvent e) {
        hudRenderer.addTickToRenderQueue(Bukkit.getOnlinePlayers());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void buildPackOnServerLoad(ServerLoadEvent e) {
        onServerLoad(e.getType());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPluginReload(MCCreativeLabReloadEvent ignored) {
        onServerLoad(ServerLoadEvent.LoadType.RELOAD);
    }

    public HudRenderer getHudRenderer() {
        return hudRenderer;
    }
}
