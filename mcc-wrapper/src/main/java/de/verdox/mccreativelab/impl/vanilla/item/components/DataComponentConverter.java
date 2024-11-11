package de.verdox.mccreativelab.impl.vanilla.item.components;

import de.verdox.mccreativelab.conversion.converter.MCCConverter;
import de.verdox.mccreativelab.impl.vanilla.platform.NMSHandle;
import net.minecraft.core.component.DataComponentType;

public class DataComponentConverter implements MCCConverter<DataComponentType, NMSDataComponentType> {
    @Override
    public ConversionResult<NMSDataComponentType> wrap(DataComponentType platform) {

/*
        if(platform.equals(DataComponents.CUSTOM_MODEL_DATA))
            return new NMSDataComponentType(DataComponents.CUSTOM_MODEL_DATA ,MCCCustomModelData.class, () -> new MCCCustomModelDataImpl());
*/

        return null;
    }

    @Override
    public ConversionResult<DataComponentType> unwrap(NMSDataComponentType api) {
        NMSHandle<DataComponentType<?>> handle = api;
        return done(handle.getHandle());
    }

    @Override
    public Class<NMSDataComponentType> apiImplementationClass() {
        return NMSDataComponentType.class;
    }

    @Override
    public Class<DataComponentType> nativeMinecraftType() {
        return DataComponentType.class;
    }
}