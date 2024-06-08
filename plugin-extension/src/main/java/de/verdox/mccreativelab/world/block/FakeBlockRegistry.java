package de.verdox.mccreativelab.world.block;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import de.verdox.mccreativelab.behaviour.BlockBehaviour;
import de.verdox.mccreativelab.world.block.behaviour.ReusedStateBehaviour;
import de.verdox.mccreativelab.world.block.display.ReusedBlockStateDisplay;
import de.verdox.mccreativelab.generator.resourcepack.CustomResourcePack;
import de.verdox.mccreativelab.generator.resourcepack.types.ItemTextureData;
import de.verdox.mccreativelab.registry.CustomRegistry;
import de.verdox.mccreativelab.util.storage.palette.IdMap;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class FakeBlockRegistry extends CustomRegistry<FakeBlock> {
    // The alternate fake block engine does not use retextured note block states
    // Instead it uses a combination of re textured empty blocks and fake block faces build with display entities.
    public static final boolean USE_ALTERNATE_FAKE_BLOCK_ENGINE = false;
    public static final IdMap<FakeBlock.FakeBlockState> FAKE_BLOCK_STATE_ID_MAP = new IdMap<>();

    public static BlockBehaviour SOLID_BLOCK_BEHAVIOUR = new BlockBehaviour() {
    };
    public static BlockBehaviour TRANSPARENT_BLOCK_BEHAVIOUR = new BlockBehaviour() {
    };
    private static final Map<BlockData, FakeBlock.FakeBlockState> reusedBlockStates = new HashMap<>();

    public static void setupFakeBlocks() {
        BlockBehaviour.BLOCK_BEHAVIOUR.setBehaviour(Material.NOTE_BLOCK, new ReusedStateBehaviour(Material.NOTE_BLOCK));
        /*if(USE_ALTERNATE_FAKE_BLOCK_ENGINE){
            fakeBlockDamage.init(MCCreativeLabExtension.getInstance().getCustomResourcePack());

            Material solidBlockMaterial = FakeBlock.FakeBlockHitbox.SOLID_BLOCK.getBlockData().getMaterial();
            Material transparentBlockMaterial = FakeBlock.FakeBlockHitbox.TRANSPARENT_BLOCK.getBlockData().getMaterial();

            NamespacedKey solidBlockKey = new NamespacedKey("replaced_blocks", "solid_block");
            NamespacedKey transparentBlockKey = new NamespacedKey("replaced_blocks", "transparent_block");

            BlockBehaviour.BLOCK_BEHAVIOUR.setBehaviour(solidBlockMaterial, new ReplacingFakeBlockBehaviour(solidBlockMaterial, solidBlockKey, () -> SOLID_BLOCK_BEHAVIOUR));
            BlockBehaviour.BLOCK_BEHAVIOUR.setBehaviour(transparentBlockMaterial, new ReplacingFakeBlockBehaviour(transparentBlockMaterial, transparentBlockKey, () -> TRANSPARENT_BLOCK_BEHAVIOUR));


            Asset<CustomResourcePack> ancient_debris_side_texture = new Asset<>("/replaced/blocks/ancient_debris_side.png");
            Asset<CustomResourcePack> ancient_debris_top_texture = new Asset<>("/replaced/blocks/ancient_debris_top.png");
            Asset<CustomResourcePack> purple_stained_glass_texture = new Asset<>("/replaced/blocks/purple_stained_glass.png");

            MCCreativeLabExtension
                .getFakeBlockRegistry()
                .register(new FakeBlock.Builder<>(solidBlockKey, FakeBlock.class)
                    .withBlockState(builder ->
                        builder
                            .withBlockProperties(fakeBlockProperties -> fakeBlockProperties.fromVanillaBlockData(solidBlockMaterial.createBlockData()))
                            .withBlockDisplay(builder1 ->
                                builder1.withTopAndBottomTexture(ancient_debris_top_texture)
                                        .withSideTexture(ancient_debris_side_texture)
                                        .withDestroyParticles(Bukkit.createBlockData(Material.NETHERRACK))
                            ))
                );

            MCCreativeLabExtension
                .getFakeBlockRegistry()
                .register(new FakeBlock.Builder<>(transparentBlockKey, FakeBlock.class)
                    .withBlockState(builder ->
                        builder
                            .withBlockProperties(fakeBlockProperties -> fakeBlockProperties.fromVanillaBlockData(transparentBlockMaterial.createBlockData()))
                            .withBlockDisplay(builder1 ->
                                builder1.withFullBlockTexture(purple_stained_glass_texture)
                                        .withDestroyParticles(Bukkit.createBlockData(Material.PURPLE_STAINED_GLASS_PANE)))
                    )
                );

            Wrappers.SoundGroup newAncientDebrisSoundGroup = Wrappers.of(Wrappers.of(Sound.BLOCK_STONE_HIT), Wrappers.of(Sound.BLOCK_STONE_STEP), Wrappers.of(Sound.BLOCK_STONE_BREAK), Wrappers.of(Sound.BLOCK_STONE_PLACE), Wrappers.of(Sound.BLOCK_STONE_FALL));
            MCCreativeLabExtension.getReplacedSoundGroups().replaceSoundGroup("block.ancient_debris", Material.ANCIENT_DEBRIS.createBlockData()
                                                                                                 .getSoundGroup(), newAncientDebrisSoundGroup);
        }*/
        //BlockBehaviour.BLOCK_BEHAVIOUR.setBehaviour(Material.NOTE_BLOCK, new ReusedStateBehaviour(Material.NOTE_BLOCK));
    }

    public static boolean hasTransparentTexture(Material material) {
        return Objects.equals(material, FakeBlock.FakeBlockHitbox.SOLID_BLOCK.getBlockData().getMaterial()) ||
            Objects.equals(material, FakeBlock.FakeBlockHitbox.TRANSPARENT_BLOCK
                .getBlockData().getMaterial());
    }

    public <T extends FakeBlock> T register(FakeBlock.Builder<T> fakeBlockBuilder) {
        T fakeBlock = fakeBlockBuilder.buildBlock();
        register(fakeBlockBuilder.namespacedKey, fakeBlock);
        for (FakeBlock.FakeBlockState fakeBlockState : fakeBlockBuilder.blockStates) {
            fakeBlockState.linkFakeBlock(fakeBlock);
            fakeBlockState.getProperties().makeImmutable();
            FAKE_BLOCK_STATE_ID_MAP.add(fakeBlockState);
            if (fakeBlockState.getFakeBlockDisplay() instanceof ReusedBlockStateDisplay reusedBlockStateDisplay)
                reusedBlockStates.put(reusedBlockStateDisplay.getHitBox().getBlockData(), fakeBlockState);
        }
        Bukkit.getLogger().info("Registering fake block " + fakeBlockBuilder.namespacedKey);
        return fakeBlock;
    }

    public boolean hasReusedAnyBlockStates(){
        return !reusedBlockStates.isEmpty();
    }

    @Nullable
    public static FakeBlock.FakeBlockState getFakeBlockStateFromBlockData(BlockData blockData) {
        if (reusedBlockStates.containsKey(blockData))
            return reusedBlockStates.get(blockData);
        return null;
    }

    public static Map<BlockData, FakeBlock.FakeBlockState> getReusedBlockStates() {
        return Map.copyOf(reusedBlockStates);
    }
}
