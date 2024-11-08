package de.verdox.mccreativelab.classgenerator;

import de.verdox.mccreativelab.classgenerator.codegen.ClassBuilder;
import de.verdox.mccreativelab.classgenerator.codegen.DynamicType;
import de.verdox.mccreativelab.classgenerator.conversion.*;
import de.verdox.mccreativelab.classgenerator.wrapper.WrapperInterfaceGenerator;
import de.verdox.mccreativelab.wrapper.inventory.MCCMenuType;
import de.verdox.mccreativelab.wrapper.item.components.MCCItemComponent;
import net.kyori.adventure.key.Key;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.PotDecorations;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.function.TriConsumer;
import org.apache.commons.lang3.function.TriFunction;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;
import java.util.function.*;
import java.util.logging.Logger;

public class ClassGenerator {

    //TODO: ItemKeys -> Create class that contains all Item keys
    //TODO: BlockItemKeys -> Create a class that contains all BlockItems

    public static final Logger LOGGER = Logger.getLogger(ClassGenerator.class.getName());
    public static final File GENERATION_DIR = new File("../../mcc-wrapper/generated/");
    ;


    public static void run() {
        LOGGER.info("Running class generator");
        ConverterGenerator converterGenerator = new ConverterGenerator("de.verdox.mccreativelab.impl.vanilla", "NMSConverter");
        converterGenerator.withPredefinedGenerator(new HolderKeyConversion());
        converterGenerator.withPredefinedGenerator(new MCCWrapperConverter());
        converterGenerator.withPredefinedGenerator(new OptionalConverter());
        converterGenerator.withPredefinedGenerator(new ListConverter());
        converterGenerator.withPredefinedGenerator(new EnumConverter());

        try {
            generateMCCItemComponentWrapper(converterGenerator);
            generateMenuTypesClass(converterGenerator);
            converterGenerator.buildConverterInterface(new File("../../mcc-wrapper/generated"));
            converterGenerator.buildWrapperAdapters(new File("../../mcc-wrapper/generated"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void generateMCCItemComponentWrapper(ConverterGenerator converterGenerator) throws IOException {
        File parentDir = new File("../../mcc-wrapper/generated/");
        try {
            FileUtils.deleteDirectory(parentDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final List<Class<?>> excludedTypes = List.of(
            AdventureModePredicate.class,
            Unit.class,
            WritableBookContent.class,
            WrittenBookContent.class,
            DebugStickState.class,
            BannerPatternLayers.class,
            BundleContents.class,
            ItemLore.class,
            Fireworks.class,
            PotDecorations.class,
            CustomData.class,
            ItemEnchantments.class,
            ChargedProjectiles.class,
            ItemContainerContents.class,
            Function.class,
            BiFunction.class,
            TriFunction.class,
            Consumer.class,
            BiConsumer.class,
            TriConsumer.class,
            Predicate.class,
            BiPredicate.class
        );
        final List<String> excludedPackages = List.of("java.");

        Class<?> dataComponentClass = DataComponents.class;
        LOGGER.info("Generating wrapper classes for DataComponents in " + dataComponentClass + " to " + parentDir.getAbsolutePath());


        WrapperInterfaceGenerator generator = new WrapperInterfaceGenerator(new File("../../mcc-wrapper/generated"), "MCC", "", excludedTypes, excludedPackages);
        generator.generateWrapper(AttributeModifier.class, "de.verdox.mccreativelab.wrapper.item.components", "de.verdox.mccreativelab.impl.vanilla.wrapper.item", converterGenerator, null);
        generator.generateWrapper(EquipmentSlotGroup.class, "de.verdox.mccreativelab.wrapper.item.components", "de.verdox.mccreativelab.impl.vanilla.wrapper.item", converterGenerator, null);

        ClassBuilder classBuilder = new ClassBuilder();
        classBuilder.withPackage("de.verdox.mccreativelab.wrapper.item.components");
        classBuilder.withHeader("public", ClassBuilder.ClassHeader.CLASS, "MCCDataComponentType", "");
        classBuilder.withClassGeneric("T", null);

        // Alle Felder der Klasse durchgehen
        for (Field field : dataComponentClass.getDeclaredFields()) {
            // Überprüfen, ob das Feld vom Typ DataComponentType ist

            if (!DataComponentType.class.isAssignableFrom(field.getType())) {
                System.out.println(field.getType() + " is not DataComponentType");
                continue;
            }
            Type genericType = field.getGenericType();
            if (!(genericType instanceof ParameterizedType parameterizedType)) {
                System.out.println(field.getType() + " is not parameterizedType");
                continue;
            }
            Type[] typeArguments = parameterizedType.getActualTypeArguments();

            DynamicType fieldType = DynamicType.of(classBuilder.getClassDescription());

            if (typeArguments.length > 0) {
                if (typeArguments[0] instanceof Class<?> componentType) {
                    if (!NMSMapper.isSwapped(componentType) && !excludedTypes.contains(componentType)) {
                        WrapperInterfaceGenerator wrapperInterfaceGenerator = new WrapperInterfaceGenerator(new File("../../mcc-wrapper/generated"), "MCC", "", excludedTypes, excludedPackages);

                        wrapperInterfaceGenerator.generateWrapper(componentType, "de.verdox.mccreativelab.wrapper.item.components", "de.verdox.mccreativelab.impl.vanilla.wrapper.item.components", converterGenerator, DynamicType.of(MCCItemComponent.class));
                        DynamicType generic = DynamicType.of(field.getGenericType()).getGenericTypes().get(0);
                        fieldType = fieldType.withAddedGeneric(generic);
                        classBuilder.withField("public static final", fieldType, field.getName(), "new " + DynamicType.of(classBuilder.getClassDescription()) + "<>()");

                        DynamicType apiGenericComponentType = fieldType;
                        DynamicType nmsGenericComponentType = DynamicType.of(field.getGenericType(), false);

                        converterGenerator.requireGeneratorFunction(apiGenericComponentType, nmsGenericComponentType);

                        converterGenerator.withPredefinedGenerator((converterGenerator1, from, to, assignedMethodName, fromParameterName, outputParameterName, methodBuilder, classBuilder1) -> {
                            if (from.equals(apiGenericComponentType) && to.equals(nmsGenericComponentType)) {
                                methodBuilder.appendAndNewLine("var " + outputParameterName + " = " + DataComponents.class.getSimpleName() + "." + field.getName() + ";");
                                classBuilder1.includeImport(DynamicType.of(DataComponents.class, false));
                                return true;
                            } else if (from.equals(nmsGenericComponentType) && to.equals(apiGenericComponentType)) {
                                methodBuilder.appendAndNewLine("var " + outputParameterName + " = " + classBuilder.getClassDescription().getTypeName() + "." + field.getName() + ";");
                                classBuilder1.includeImport(DynamicType.of(classBuilder.getClassDescription(), false));
                                return true;
                            }
                            return false;
                        });
                    }
                }
            }
        }
        classBuilder.writeClassFile(parentDir);
    }

    private static void generateMenuTypesClass(ConverterGenerator converterGenerator) throws IOException {

        ClassBuilder classBuilder = new ClassBuilder();
        classBuilder.withPackage("de.verdox.mccreativelab.wrapper.inventory");
        classBuilder.withHeader("public", ClassBuilder.ClassHeader.INTERFACE, "MCCMenuTypes", "");

        for (Field field : MenuType.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()))
                continue;
            if (!Modifier.isFinal(field.getModifiers()))
                continue;
            if (!Modifier.isPublic(field.getModifiers()))
                continue;
            if (!MenuType.class.isAssignableFrom(field.getType()))
                continue;
            String fieldName = field.getName();
            String keyValue = fieldName.toLowerCase(Locale.ROOT);

            int containerSize = 0;
            if(keyValue.contains("generic")){
                String[] twoInts = keyValue.replace("generic_", "").split("x");
                containerSize = Integer.parseInt(twoInts[0]) * Integer.parseInt(twoInts[1]);
            }
            else {
                for (InventoryType value : InventoryType.values()) {
                    if(value.getMenuType() == null)
                        continue;
                    if (value.getMenuType().key().equals(Key.key(Key.MINECRAFT_NAMESPACE, keyValue)))
                        containerSize = value.getDefaultSize();
                }
            }

            classBuilder.withField("public static final", DynamicType.of(MCCMenuType.class), fieldName, "new MCCMenuType(Key.key(Key.MINECRAFT_NAMESPACE, \"" + keyValue + "\"), " + containerSize + ")");
        }
        classBuilder.includeImport(DynamicType.of(Key.class));
        classBuilder.writeClassFile(GENERATION_DIR);
    }
}