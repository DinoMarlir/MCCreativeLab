package de.verdox.mccreativelab.debug.vanilla;

import de.verdox.mccreativelab.behaviour.BehaviourResult;
import de.verdox.mccreativelab.random.VanillaRandomSource;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

public class VanillaSugarCaneRandomTickBehaviour extends VanillaCropRandomTickBehaviour {
    public VanillaSugarCaneRandomTickBehaviour(int minLightLevel) {
        super(minLightLevel);
    }

    @Override
    public BehaviourResult.@NotNull Void randomTick(@NotNull Block block, @NotNull VanillaRandomSource vanillaRandomSource) {
        if (!block.getRelative(0, 1, 0).getType().isAir())
            return voidResult();
        var heightCounter = 0;
        for(heightCounter = 1; block.getRelative(0, heightCounter,0).getType().equals(Material.SUGAR_CANE); heightCounter++){
            ;
        }
        var modifier = getAndValidateGrowth("Cane");
        var ageable = (Ageable) block.getBlockData();
        var age = ageable.getAge();
        var maxAge = ageable.getMaximumAge();

        if(heightCounter < 3){

            if(age >= 15 || (modifier != 100 && drawRandomNumber(vanillaRandomSource) < (modifier / (100f * 16)))){
                handleBlockGrowEvent(block.getRelative(0,1,0), Bukkit.createBlockData(Material.SUGAR_CANE));
                ageable.setAge(0);
                block.setBlockData(ageable);
            }
            else if(modifier == 100 || drawRandomNumber(vanillaRandomSource) < (modifier / (100.0f * 16))){
                ageable.setAge(Math.min(maxAge, ageable.getAge() + 1));
                block.setBlockData(ageable);
            }
        }
        return voidResult();
    }

    @Override
    public BehaviourResult.@NotNull Bool isBlockDataRandomlyTicking(@NotNull BlockData blockData) {
        return bool(true);
    }
}
