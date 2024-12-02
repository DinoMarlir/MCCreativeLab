package de.verdox.mccreativelab.world.block;

import com.google.gson.JsonObject;
import de.verdox.mccreativelab.behaviour.BlockBehaviour;
import de.verdox.mccreativelab.impl.mcclab.block.MCCCustomBlockSoundGroup;
import de.verdox.mccreativelab.world.block.display.FakeBlockDisplay;
import de.verdox.mccreativelab.world.block.entity.FakeBlockEntity;
import de.verdox.mccreativelab.world.block.entity.FakeBlockEntityStorage;
import de.verdox.mccreativelab.world.block.entity.FakeBlockEntityType;
import de.verdox.mccreativelab.world.block.event.FakeBlockDropExperienceEvent;
import de.verdox.mccreativelab.world.block.event.FakeBlockDropItemsEvent;
import de.verdox.mccreativelab.world.block.util.FakeBlockUtil;
import de.verdox.mccreativelab.world.block.replaced.ReplacedCrop;
import de.verdox.mccreativelab.generator.Asset;
import de.verdox.mccreativelab.generator.resourcepack.AssetBasedResourcePackResource;
import de.verdox.mccreativelab.generator.resourcepack.CustomResourcePack;
import de.verdox.mccreativelab.generator.resourcepack.ResourcePackAssetTypes;
import de.verdox.mccreativelab.generator.resourcepack.ResourcePackResource;
import de.verdox.mccreativelab.generator.resourcepack.types.ItemTextureData;
import de.verdox.mccreativelab.generator.resourcepack.types.ModelFile;
import de.verdox.mccreativelab.generator.resourcepack.types.sound.SoundData;
import de.verdox.mccreativelab.wrapper.block.MCCBlockSoundGroup;
import de.verdox.mccreativelab.wrapper.typed.MCCBlocks;
import net.kyori.adventure.sound.Sound;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public abstract class FakeBlock implements Keyed, BlockBehaviour {
    private final FakeBlockState[] fakeBlockStates;
    private final Map<FakeBlockState, Integer> blockStateToIdMapping = new HashMap<>();
    private final NamespacedKey key;

    protected FakeBlock(NamespacedKey namespacedKey, List<FakeBlockState> fakeBlockStates) {
        this.key = namespacedKey;
        this.fakeBlockStates = fakeBlockStates.toArray(FakeBlockState[]::new);
        for (int i = 0; i < fakeBlockStates.size(); i++) {
            blockStateToIdMapping.put(fakeBlockStates.get(i), i);
        }
    }

    public final FakeBlockState[] getFakeBlockStates() {
        return fakeBlockStates;
    }

    public FakeBlockState getDefaultBlockState() {
        return getBlockState(0);
    }

    public final int getBlockStateID(FakeBlockState fakeBlockState) {
        return blockStateToIdMapping.getOrDefault(fakeBlockState, -1);
    }

    @Nullable
    public final FakeBlockState getBlockState(int blockStateID) {
        if (blockStateID >= fakeBlockStates.length)
            return null;
        return fakeBlockStates[blockStateID];
    }

    //TODO: Not implemented yet -> We just block it for now
    public final PistonMoveReaction getPistonMoveReaction() {
        return PistonMoveReaction.BLOCK;
    }

    public boolean isPreferredTool(@NotNull FakeBlockState fakeBlockState, @NotNull Block block, @NotNull Player player, @Nullable ItemStack stack) {
        return true;
    }

    public float getDestroySpeed(@NotNull FakeBlockState fakeBlockState, @NotNull Player player, @Nullable ItemStack stack) {
        return 1f;
    }

    public List<ItemStack> drawLoot(Block block, FakeBlockState fakeBlockState, @Nullable Entity causeOfItemDrop, @Nullable ItemStack toolUsed, boolean ignoreTool) {
        return new ArrayList<>();
    }

    protected int getExperienceToDrop(Block block, FakeBlockState fakeBlockState, @Nullable Entity causeOfExperienceDrop, @Nullable ItemStack toolUsed, boolean ignoreTool) {
        return 0;
    }

    /**
     * Returns whether this {@link FakeBlock} has a {@link de.verdox.mccreativelab.world.block.entity.FakeBlockEntity}
     *
     * @return true if it has a block entity
     */
    public final boolean hasBlockEntity() {
        return getFakeBlockEntityType() != null;
    }

    /**
     * Returns the block entity type if hasBlockEntity returns true.
     * If hasBlockEntity is false this method will never be called.
     *
     * @return The FakeBlockEntityType
     */
    @NotNull
    public FakeBlockEntityType<?> getFakeBlockEntityType() {
        return null;
    }

    public void remove(Location location, boolean withEffects) {
        remove(location, withEffects, false, null);
    }

    public void remove(Location location, boolean withEffects, boolean dropLoot, @Nullable Entity causeOfBreak) {
        remove(location, withEffects, dropLoot, causeOfBreak, null, true);
    }

    public void remove(Location location, boolean withEffects, boolean dropLoot, @Nullable Entity causeOfBreak, @Nullable ItemStack tool, boolean ignoreTool) {
        remove(location, withEffects, dropLoot, true, causeOfBreak, tool, ignoreTool);
    }

    public void remove(Location location, boolean withEffects, boolean dropLoot, boolean dropExperience, @Nullable Entity causeOfBreak, @Nullable ItemStack tool, boolean ignoreTool) {
        FakeBlock.FakeBlockState fakeBlockState = FakeBlockStorage.getFakeBlockState(location, false);
        if (fakeBlockState == null)
            return;
        if (withEffects)
            FakeBlockUtil.simulateBlockBreakWithParticlesAndSound(fakeBlockState, location.getBlock());
        if (dropLoot)
            dropBlockLoot(location, fakeBlockState, causeOfBreak, tool, ignoreTool);
        if (dropExperience)
            dropBlockExperience(location, fakeBlockState, causeOfBreak, tool, ignoreTool);
        removeBlockEntity(location);
        FakeBlockStorage.setFakeBlockState(location, null, false);
    }

    private void removeBlockEntity(Location location) {
        if (hasBlockEntity()) {
            FakeBlockEntity fakeBlockEntity = FakeBlockEntityStorage.getFakeBlockEntityAt(location.getBlock());
            if (fakeBlockEntity != null)
                fakeBlockEntity.getMarkerEntity().remove();
        }
    }

    public void dropBlockLoot(Location location, FakeBlockState fakeBlockState, @Nullable Entity causeOfBreak, @Nullable ItemStack tool, boolean ignoreTool) {
        List<ItemStack> itemsToDrop = new ArrayList<>(fakeBlockState.getFakeBlock().drawLoot(location.getBlock(), fakeBlockState, causeOfBreak, tool, ignoreTool));
        if (itemsToDrop.isEmpty())
            return;
        FakeBlockDropItemsEvent fakeBlockDropItemsEvent = new FakeBlockDropItemsEvent(location.getBlock(), fakeBlockState, itemsToDrop, causeOfBreak, tool, ignoreTool);
        if (!fakeBlockDropItemsEvent.callEvent())
            return;
        for (ItemStack stack : fakeBlockDropItemsEvent.getItems())
            location.getBlock().getWorld().dropItemNaturally(location, stack.clone());
    }

    public void dropBlockExperience(Location location, FakeBlockState fakeBlockState, @Nullable Entity causeOfBreak, @Nullable ItemStack tool, boolean ignoreTool) {
        int experience = fakeBlockState.getFakeBlock().getExperienceToDrop(location.getBlock(), fakeBlockState, causeOfBreak, tool, ignoreTool);
        if (experience == 0)
            return;
        FakeBlockDropExperienceEvent fakeBlockDropItemsEvent = new FakeBlockDropExperienceEvent(location.getBlock(), fakeBlockState, experience, causeOfBreak, tool, ignoreTool);
        if (!fakeBlockDropItemsEvent.callEvent())
            return;
        location.getBlock().getWorld().spawnEntity(location.getBlock().getLocation(), EntityType.EXPERIENCE_ORB, CreatureSpawnEvent.SpawnReason.DEFAULT, entity -> {
            ExperienceOrb experienceOrb = (ExperienceOrb) entity;
            experienceOrb.setExperience(experience);
        });
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "FakeBlock{" +
            "fakeBlockStates=" + Arrays.toString(fakeBlockStates) +
            ", blockStateToIdMapping=" + blockStateToIdMapping +
            ", key=" + key +
            '}';
    }

    public static class Builder<T extends FakeBlock> {
        final NamespacedKey namespacedKey;
        private final BiFunction<NamespacedKey, List<FakeBlockState>, T> fakeBlockConstructor;
        final List<FakeBlockState> blockStates = new LinkedList<>();

        @Deprecated
        public Builder(NamespacedKey namespacedKey, Class<? extends T> fakeBlockClass) {
            this.namespacedKey = namespacedKey;
            this.fakeBlockConstructor = (namespacedKey1, fakeBlockStates) -> {
                try {
                    var constructor = fakeBlockClass.getDeclaredConstructor(NamespacedKey.class, List.class);
                    constructor.setAccessible(true);
                    return constructor.newInstance(namespacedKey1, blockStates);
                } catch (NoSuchMethodException e) {
                    Bukkit.getLogger()
                        .warning("FakeBlock class " + fakeBlockClass.getSimpleName() + " does not implement base constructor(FakeBlockState[])");
                    throw new RuntimeException(e);
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            };
        }

        public Builder(NamespacedKey namespacedKey, BiFunction<NamespacedKey, List<FakeBlockState>, T> fakeBlockConstructor) {
            this.namespacedKey = namespacedKey;
            this.fakeBlockConstructor = fakeBlockConstructor;
        }

        public Builder<T> withBlockState(Consumer<FakeBlockState.Builder> builderConsumer) {
            FakeBlockState.Builder builder = new FakeBlockState.Builder(namespacedKey, new NamespacedKey(namespacedKey.namespace(), namespacedKey.getKey() + "_state_" + blockStates.size()));
            builderConsumer.accept(builder);
            blockStates.add(builder.build());
            return this;
        }

        T buildBlock() {
            if (blockStates.isEmpty())
                throw new IllegalStateException(namespacedKey.asString() + " must provide at least one fake block state");

            return this.fakeBlockConstructor.apply(namespacedKey, blockStates);
        }
    }

    public static class FakeBlockState {
        private FakeBlock fakeBlock;
        private final FakeBlockProperties properties;
        private final FakeBlockDisplay fakeBlockDisplay;
        @Nullable
        private final FakeBlockSoundGroup fakeBlockSoundGroup;
        @Nullable
        private final BlockData vanillaBlockSound;
        private final Set<Class<? extends BlockEvent>> blockedEventsByDefault;

        FakeBlockState(FakeBlockProperties properties, FakeBlockDisplay fakeBlockDisplay, @Nullable FakeBlockSoundGroup fakeBlockSoundGroup, @Nullable BlockData vanillaBlockSound, Set<Class<? extends BlockEvent>> blockedEventsByDefault) {
            this.properties = properties;
            this.fakeBlockDisplay = fakeBlockDisplay;
            this.fakeBlockSoundGroup = fakeBlockSoundGroup;
            this.vanillaBlockSound = vanillaBlockSound;
            this.blockedEventsByDefault = blockedEventsByDefault;
        }

        public FakeBlockDisplay getFakeBlockDisplay() {
            return fakeBlockDisplay;
        }

        @Nullable
        public FakeBlockSoundGroup getFakeBlockSoundGroup() {
            return fakeBlockSoundGroup;
        }

        public FakeBlockProperties getProperties() {
            return properties;
        }

        public @Nullable BlockData getVanillaBlockSound() {
            return vanillaBlockSound;
        }

        public FakeBlock getFakeBlock() {
            return fakeBlock;
        }

        public Set<Class<? extends BlockEvent>> getBlockedEventsByDefault() {
            return blockedEventsByDefault;
        }

        FakeBlockState linkFakeBlock(FakeBlock fakeBlock) {
            this.fakeBlock = fakeBlock;
            return this;
        }

        public static class Builder {
            private final NamespacedKey parentBlockKey;
            private final NamespacedKey blockStateKey;
            private FakeBlockProperties fakeBlockProperties = new FakeBlockProperties();
            private FakeBlockDisplay fakeBlockDisplay;
            private FakeBlockSoundGroup fakeBlockSoundGroup;
            private BlockData vanillaBlockSound;
            private final Set<Class<? extends BlockEvent>> blockedEventsByDefault = new HashSet<>();

            Builder(NamespacedKey parentBlockKey, NamespacedKey blockStateKey) {
                Objects.requireNonNull(parentBlockKey);
                Objects.requireNonNull(blockStateKey);
                this.parentBlockKey = parentBlockKey;
                this.blockStateKey = blockStateKey;
            }

            public Builder withBlockProperties(Consumer<FakeBlockProperties> fakeBlockPropertiesConsumer) {
                this.fakeBlockProperties = new FakeBlockProperties();
                fakeBlockPropertiesConsumer.accept(this.fakeBlockProperties);
                return this;
            }

            /**
             * This is needed if you alter the fertilizeAction because spigot will call the event afterward and reset the block states.
             * Here we prevent the event entirely.
             *
             * @return The builder
             */
            public Builder preventFertilizeEvent() {
                this.blockedEventsByDefault.add(BlockFertilizeEvent.class);
                return this;
            }

            public Builder withSoundGroup(SoundData hitSound, SoundData stepSound, SoundData breakSound, SoundData placeSound, SoundData fallSound) {
                this.fakeBlockSoundGroup = new FakeBlockSoundGroup(parentBlockKey, hitSound, stepSound, breakSound, placeSound, fallSound);
                return this;
            }

            public Builder withVanillaBlockSound(BlockData blockSound) {
                this.vanillaBlockSound = blockSound;
                return this;
            }

            public Builder withBlockDisplay(FakeBlockDisplay.Builder<?> builder) {
                this.fakeBlockDisplay = builder.build(blockStateKey);
                return this;
            }

            FakeBlockState build() {
                return new FakeBlockState(fakeBlockProperties, fakeBlockDisplay, fakeBlockSoundGroup, vanillaBlockSound, Set.copyOf(blockedEventsByDefault));
            }
        }

        @Override
        public String toString() {
            return "FakeBlockState{" +
                ", fakeBlock=" + fakeBlock.getKey() +
                ", properties=" + properties +
                ", fakeBlockDisplay=" + fakeBlockDisplay +
                ", fakeBlockSoundGroup=" + fakeBlockSoundGroup +
                '}';
        }
    }

    public static class FakeBlockSoundGroup extends ResourcePackResource {
        private final SoundData hitSound;
        private final SoundData stepSound;
        private final SoundData breakSound;
        private final SoundData placeSound;
        private final SoundData fallSound;

        public FakeBlockSoundGroup(@NotNull NamespacedKey namespacedKey, @Nullable SoundData hitSound, @Nullable SoundData stepSound, @Nullable SoundData breakSound, @Nullable SoundData placeSound, @Nullable SoundData fallSound) {
            super(namespacedKey);
            this.hitSound = hitSound;
            this.stepSound = stepSound;
            this.breakSound = breakSound;
            this.placeSound = placeSound;
            this.fallSound = fallSound;
        }

        public FakeBlockSoundGroup(@Nullable SoundData hitSound, @Nullable SoundData stepSound, @Nullable SoundData breakSound, @Nullable SoundData placeSound, @Nullable SoundData fallSound) {
            this(new NamespacedKey("mcc", "fake_block_sound_group_no_parent_"+ ThreadLocalRandom.current().nextInt(10000)), hitSound, stepSound, breakSound, placeSound, fallSound);
        }

        @Override
        public void onRegister(CustomResourcePack customPack) {
            customPack.registerNullable(hitSound);
            customPack.registerNullable(stepSound);
            customPack.registerNullable(breakSound);
            customPack.registerNullable(placeSound);
            customPack.registerNullable(fallSound);
        }

        public MCCBlockSoundGroup asMCCBlockSoundGroup() {
            Sound.Source source = Sound.Source.BLOCK;
            float volume = 1;
            float pitch = 1;
            MCCBlockSoundGroup backup = MCCBlocks.STONE.get().getSoundGroup();
            return new MCCCustomBlockSoundGroup(
                hitSound != null ? hitSound.asSound(source, volume, pitch) : backup.hitSound(),
                stepSound != null ? stepSound.asSound(source, volume, pitch): backup.stepSound(),
                breakSound != null ? breakSound.asSound(source, volume, pitch): backup.breakSound(),
                placeSound != null ? placeSound.asSound(source, volume, pitch): backup.placeSound(),
                fallSound != null ? fallSound.asSound(source, volume, pitch): backup.fallSound()
            );
        }

        @Override
        public void installResourceToPack(CustomResourcePack customPack) throws IOException {

        }

        @Override
        public String toString() {
            return "FakeBlockSoundGroup{" +
                "hitSound=" + hitSound +
                ", stepSound=" + stepSound +
                ", breakSound=" + breakSound +
                ", placeSound=" + placeSound +
                ", fallSound=" + fallSound +
                '}';
        }

        public SoundData getHitSound() {
            return hitSound;
        }

        public SoundData getStepSound() {
            return stepSound;
        }

        public SoundData getBreakSound() {
            return breakSound;
        }

        public SoundData getPlaceSound() {
            return placeSound;
        }

        public SoundData getFallSound() {
            return fallSound;
        }
    }

    public static class FakeBlockHitbox {
        private static final Set<FakeBlockHitbox> fakeBlockHitBoxes = new HashSet<>();
        public static final FakeBlockHitbox SOLID_BLOCK = new FakeBlockHitbox(Bukkit.createBlockData(Material.ANCIENT_DEBRIS, blockData -> {
        }));
        public static final FakeBlockHitbox TRANSPARENT_BLOCK = new FakeBlockHitbox(Bukkit.createBlockData(Material.PURPLE_STAINED_GLASS, blockData -> {
        }));
        private final BlockData blockData;
        private boolean used;

        public FakeBlockHitbox(BlockData blockData) {
            this.blockData = blockData;
            fakeBlockHitBoxes.add(this);
        }

        public BlockData getBlockData() {
            return blockData;
        }

        public void setUsed() {
            this.used = true;
        }

        protected void makeInvisible(CustomResourcePack customResourcePack, Asset<CustomResourcePack> emptyBlockModelAsset, Asset<CustomResourcePack> emptyBlockStatesFile) throws IOException {
            emptyBlockStatesFile.installAsset(customResourcePack, new NamespacedKey("minecraft", getBlockData()
                .getMaterial().name().toLowerCase(Locale.ROOT)), ResourcePackAssetTypes.BLOCK_STATES, "json");
        }

        public static void makeHitBoxesInvisible(CustomResourcePack customResourcePack) throws IOException {
            Bukkit.getLogger().info("Installing invisible hitboxes");

            Asset<CustomResourcePack> emptyBlockModel = new Asset<>(() -> CustomResourcePack.class.getResourceAsStream("/empty_block/models/empty.json"));
            Asset<CustomResourcePack> emptyBlockStatesFile = new Asset<>(() -> CustomResourcePack.class.getResourceAsStream("/empty_block/blockstates/empty_blockstates.json"));
            Asset<CustomResourcePack> emptyBlockTexture = new Asset<>(() -> CustomResourcePack.class.getResourceAsStream("/empty_block/textures/empty.png"));

            customResourcePack.register(new AssetBasedResourcePackResource(new NamespacedKey("minecraft", "block/empty"), emptyBlockModel, ResourcePackAssetTypes.MODELS, "json"));
            customResourcePack.register(new AssetBasedResourcePackResource(new NamespacedKey("minecraft", "block/empty"), emptyBlockTexture, ResourcePackAssetTypes.TEXTURES, "png"));

            for (FakeBlockHitbox fakeBlockHitBox : fakeBlockHitBoxes) {
                if (fakeBlockHitBox.used)
                    fakeBlockHitBox.makeInvisible(customResourcePack, emptyBlockModel, emptyBlockStatesFile);
            }
        }

        public static void makeModelEmpty(CustomResourcePack customResourcePack, NamespacedKey modelKey) {
            JsonObject jsonToWriteToFile = new JsonObject();
            ItemTextureData.ModelType modelType = ReplacedCrop.createFakeCropModel(new NamespacedKey("minecraft", "block/empty"));
            modelType.modelCreator().accept(null, jsonToWriteToFile);
            customResourcePack.register(new ModelFile(modelKey, modelType));
        }

        public static FakeBlockHitbox createFakeBlockHitbox(BlockData blockData) {
            return new FakeBlockHitbox(blockData);
        }

        @Override
        public String toString() {
            return "FakeBlockHitbox{" +
                "blockData=" + blockData +
                '}';
        }
    }

    public static class FakeBlockProperties {
        private int lightEmission = 0;
        private float explosionResistance = 0.0F;
        private float hardness = 1.0F;
        private boolean requiresCorrectToolForDrops = false;
        private boolean isRandomlyTicking = false;
        private float speedFactor = 1.0F;
        private float jumpFactor = 1.0F;
        private boolean immutable;

        FakeBlockProperties() {

        }

        public FakeBlockProperties fromVanillaBlockData(BlockData blockData) {
            withLightEmission(blockData.getLightEmission());
            withExplosionResistance(blockData.getMaterial().getBlastResistance());
            withBlockHardness(blockData.getMaterial().getHardness());
            requiresCorrectToolForDrops(blockData.requiresCorrectToolForDrops());
            isRandomlyTicking(blockData.isRandomlyTicked());
            return this;
        }

        public FakeBlockProperties withLightEmission(int lightEmission) {
            checkImmutability();
            this.lightEmission = lightEmission;
            return this;
        }

        public FakeBlockProperties withExplosionResistance(float explosionResistance) {
            checkImmutability();
            this.explosionResistance = explosionResistance;
            return this;
        }

        public FakeBlockProperties withBlockHardness(float hardness) {
            checkImmutability();
            this.hardness = hardness;
            return this;
        }

        public FakeBlockProperties requiresCorrectToolForDrops(boolean requiresCorrectToolForDrops) {
            checkImmutability();
            this.requiresCorrectToolForDrops = requiresCorrectToolForDrops;
            return this;
        }

        public FakeBlockProperties isRandomlyTicking(boolean isRandomlyTicking) {
            checkImmutability();
            this.isRandomlyTicking = isRandomlyTicking;
            return this;
        }

        public FakeBlockProperties withSpeedFactor(float speedFactor) {
            checkImmutability();
            this.speedFactor = speedFactor;
            return this;
        }

        public FakeBlockProperties withJumpFactor(float jumpFactor) {
            checkImmutability();
            this.jumpFactor = jumpFactor;
            return this;
        }

        private void checkImmutability() {
            if (immutable)
                throw new IllegalStateException("Block properties can't be changed after building it");
        }

        void makeImmutable() {
            immutable = true;
        }

        public int getLightEmission() {
            return lightEmission;
        }

        public float getExplosionResistance() {
            return explosionResistance;
        }

        public float getHardness() {
            return hardness;
        }

        public boolean isRequiresCorrectToolForDrops() {
            return requiresCorrectToolForDrops;
        }

        public boolean isRandomlyTicking() {
            return isRandomlyTicking;
        }

        public float getSpeedFactor() {
            return speedFactor;
        }

        public float getJumpFactor() {
            return jumpFactor;
        }

        @Override
        public String toString() {
            return "FakeBlockProperties{" +
                "lightEmission=" + lightEmission +
                ", explosionResistance=" + explosionResistance +
                ", hardness=" + hardness +
                ", requiresCorrectToolForDrops=" + requiresCorrectToolForDrops +
                ", isRandomlyTicking=" + isRandomlyTicking +
                ", speedFactor=" + speedFactor +
                ", jumpFactor=" + jumpFactor +
                ", immutable=" + immutable +
                '}';
        }
    }
}
