package de.verdox.mccreativelab.debug;

import de.verdox.mccreativelab.MCCreativeLabExtension;
import de.verdox.mccreativelab.world.block.FakeBlock;
import de.verdox.mccreativelab.world.block.FakeBlockStorage;
import de.verdox.mccreativelab.world.block.entity.FakeBlockEntityStorage;
import de.verdox.mccreativelab.world.block.replaced.ReplacedBlocks;
import de.verdox.mccreativelab.world.item.FakeItem;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FakeBlockCommand extends Command {

    public FakeBlockCommand() {
        super("fakeblock");
        setPermission("mccreativelab.command.fakeblock");
    }

/*    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if(!sender.hasPermission("mccreativelab.command.fakeblock"))
            return false;
        if(!(sender instanceof Player player))
            return false;
        if(args.length == 0)
            return false;
        if(args.length == 1){
            RayTraceResult rayTraceResult = player.rayTraceBlocks(5);
            if(rayTraceResult == null)
                return false;
            Block block = rayTraceResult.getHitBlock();
            if(block == null)
                return false;
            if(args[0].equalsIgnoreCase("set")){
                if(FakeBlockStorage.setFakeBlock(block.getLocation(), ReplacedBlocks.WHEAT,false))
                    player.sendMessage(Component.text("Set fake block"));
                else player.sendMessage(Component.text("Could not set fake block"));
                return true;
            }
            else if(args[0].equalsIgnoreCase("get")){
                FakeBlock.FakeBlockState fakeBlockState = FakeBlockStorage.getFakeBlockState(block.getLocation(), true);
                FakeBlock fakeBlock = FakeBlockStorage.getFakeBlock(block.getLocation(), true);
                if(fakeBlock == null){
                    player.sendMessage(Component.text("No fake block found"));
                    return true;
                }

                player.sendMessage(Component.text("Found fake block: "+ MCCreativeLabExtension.getFakeBlockRegistry().getKey(fakeBlock)+" | "+fakeBlock.getBlockStateID(fakeBlockState)));
                return true;
            }
            else if(args[0].equalsIgnoreCase("damage")){
                player.sendBlockDamage(block.getLocation(), 0.9f, 12355);
            }
        }
        return false;
    }*/

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!sender.hasPermission("mccreativelab.command.fakeblock"))
            return false;
        if (!(sender instanceof Player player))
            return false;

        RayTraceResult rayTraceResult = player.rayTraceBlocks(5);
        if (rayTraceResult == null)
            return false;
        Block block = rayTraceResult.getHitBlock();
        if (block == null)
            return false;

        if (args.length == 0) {
            player.sendMessage("");
            return false;
        }
        if (args[0].equalsIgnoreCase("get")) {
            if (args.length == 2) {
                String keyAsString = args[1];
                try {
                    NamespacedKey namespacedKey = NamespacedKey.fromString(keyAsString);
                    FakeBlock fakeBlock = MCCreativeLabExtension.getFakeBlockRegistry().get(namespacedKey);
                    FakeBlockStorage.setFakeBlock(block.getLocation(), fakeBlock, false);
                    FakeBlockEntityStorage.createFakeBlockEntity(fakeBlock.getDefaultBlockState(), block.getLocation());
                } catch (Exception e) {
                    sender.sendMessage("Please provide a valid custom item");
                    return false;
                }
            }
        } else if (args[0].equalsIgnoreCase("info")) {

            player.sendMessage(Component.text(FakeBlockStorage.getFakeBlockState(block.getLocation(), false) + " [" + block.getX() + " | " + block.getY() + " | " + block.getZ() + "]"));
        }
        return false;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (args.length <= 1)
            return List.of("get", "info");
        if (args.length == 2)
            return MCCreativeLabExtension.getFakeBlockRegistry().streamKeys().map(NamespacedKey::asString).filter(s -> s.contains(args[1])).toList();
        return List.of();
    }
}
