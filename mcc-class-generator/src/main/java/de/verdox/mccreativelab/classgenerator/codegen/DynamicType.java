package de.verdox.mccreativelab.classgenerator.codegen;

import de.verdox.mccreativelab.classgenerator.NMSMapper;
import de.verdox.mccreativelab.classgenerator.codegen.expressions.CodeExpression;
import de.verdox.mccreativelab.classgenerator.codegen.type.ClassDescription;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.*;

public class DynamicType {

    private Set<ClassDescription> importedClasses = new HashSet<>();
    private List<DynamicType> genericTypes = new LinkedList<>();
    private ClassDescription classDescription;
    @Nullable
    private Class<?> arrayComponentType;
    @Nullable
    private Class<?> rawType;
    private List<DynamicType> upperBounds = new LinkedList<>();
    private List<DynamicType> lowerBounds = new LinkedList<>();

    public static DynamicType of(Type type, boolean trySwap) {
        if (trySwap && NMSMapper.isSwapped(type) && !isPrimitiveType(type))
            return NMSMapper.getSwap(type);
        return new DynamicType(type, trySwap);
    }

    public static DynamicType of(Type type) {
        return DynamicType.of(type, true);
    }

    public CodeExpression getDefaultValueAsString() {
        return codeLineBuilder -> {
            if (getClassDescription().isPrimitiveType()) {
                if (boolean.class.equals(rawType) || Boolean.class.equals(rawType)) {
                    codeLineBuilder.append("false");
                } else if (
                    byte.class.equals(rawType) || Byte.class.equals(rawType) ||
                        char.class.equals(rawType) || Character.class.equals(rawType) ||
                        short.class.equals(rawType) || Short.class.equals(rawType) ||
                        int.class.equals(rawType) || Integer.class.equals(rawType) ||
                        long.class.equals(rawType) || Long.class.equals(rawType) ||
                        float.class.equals(rawType) || Float.class.equals(rawType) ||
                        double.class.equals(rawType) || Double.class.equals(rawType)) {
                    codeLineBuilder.append("0");
                }
            } else if (rawType != null && List.class.isAssignableFrom(rawType)) {
                if (rawType.getPackageName().contains("it.unimi.dsi.fastutil.")) {
                    codeLineBuilder.append(DynamicType.of(rawType, false) + ".of()");
                } else {
                    codeLineBuilder.append("List.of()");
                }
            } else if (rawType != null && Set.class.isAssignableFrom(rawType)) {
                codeLineBuilder.append("Set.of()");
            } else if (rawType != null && Map.class.isAssignableFrom(rawType)) {
                codeLineBuilder.append("Map.of()");
            } else {
                codeLineBuilder.append("null");
            }
        };
    }

    private DynamicType(Type type, boolean trySwap) {
        Objects.requireNonNull(type);
        switch (type) {
            case ClassDescription description -> this.classDescription = description;
            case ParameterizedType parameterizedType -> {
                for (Type actualTypeArgument : parameterizedType.getActualTypeArguments()) {
                    DynamicType genericType = DynamicType.of(actualTypeArgument, trySwap);
                    importedClasses.addAll(genericType.getImportedClasses());
                    genericTypes.add(genericType);
                }
                this.classDescription = new ClassDescription((Class<?>) parameterizedType.getRawType());
                this.rawType = (Class<?>) parameterizedType.getRawType();
            }
            case Class<?> clazz -> {
                if (clazz.isArray()) {
                    arrayComponentType = clazz.getComponentType();
                    if (!isPrimitiveType(clazz.getComponentType()))
                        addImport(new ClassDescription(clazz.getComponentType()));
                }
                this.classDescription = new ClassDescription(clazz);
                this.rawType = clazz;
            }
            case WildcardType wildcardType -> {
                for (Type bound : wildcardType.getLowerBounds()) {
                    DynamicType lowerBound = DynamicType.of(bound, trySwap);
                    lowerBounds.add(lowerBound);
                    if (this.rawType == null) {
                        this.rawType = lowerBound.rawType;
                        this.classDescription = lowerBound.getClassDescription();
                    }
                }

                for (Type bound : wildcardType.getUpperBounds()) {
                    DynamicType upperBound = DynamicType.of(bound, trySwap);
                    upperBounds.add(upperBound);
                    if (this.rawType == null) {
                        this.rawType = upperBound.rawType;
                        this.classDescription = upperBound.getClassDescription();
                    }
                }
            }
            case null, default ->
                throw new RuntimeException("Dynamic Types are not supported for " + type + " (" + type.getClass() + ", " + type + ")");
        }

        // Is inner class of a top level class
        if (this.rawType != null && this.rawType.getDeclaringClass() != null)
            importedClasses.addAll(DynamicType.of(this.rawType.getDeclaringClass(), false).getImportedClasses());
        else if (this.classDescription.getDeclaringParentClass() != null)
            importedClasses.addAll(DynamicType.of(this.classDescription.getDeclaringParentClass(), false).getImportedClasses());
            // Else we import the top level class
        else if (!isPrimitiveType(type))
            addImport(this.classDescription);
    }

    // Only used for immutable change methods
    private DynamicType() {

    }

    public DynamicType withRawType(DynamicType other) {

        DynamicType dynamicType = new DynamicType();
        dynamicType.importedClasses = new HashSet<>(this.importedClasses);
        dynamicType.removeImport(getClassDescription());
        dynamicType.genericTypes = new LinkedList<>(this.genericTypes);
        dynamicType.arrayComponentType = this.arrayComponentType;

        dynamicType.upperBounds = new LinkedList<>(this.upperBounds);
        dynamicType.lowerBounds = new LinkedList<>(this.lowerBounds);

        dynamicType.rawType = other.rawType;
        dynamicType.classDescription = other.getClassDescription();
        dynamicType.addImport(dynamicType.classDescription);

        return dynamicType;
    }

    public DynamicType withSwappedGeneric(DynamicType oldGeneric, DynamicType newGeneric) {

        DynamicType dynamicType = new DynamicType();
        dynamicType.importedClasses = new HashSet<>(this.importedClasses);
        dynamicType.genericTypes = new LinkedList<>(this.genericTypes);
        dynamicType.arrayComponentType = this.arrayComponentType;

        dynamicType.genericTypes.remove(oldGeneric);
        dynamicType.genericTypes.add(newGeneric);

        dynamicType.upperBounds = new LinkedList<>(this.upperBounds);
        dynamicType.lowerBounds = new LinkedList<>(this.lowerBounds);

        dynamicType.removeImport(oldGeneric.classDescription);
        dynamicType.addImport(newGeneric.classDescription);

        dynamicType.rawType = this.rawType;
        dynamicType.classDescription = this.getClassDescription();
        return dynamicType;
    }

    public DynamicType withNoGenerics() {

        DynamicType dynamicType = new DynamicType();
        dynamicType.importedClasses = new HashSet<>(this.importedClasses);
        dynamicType.genericTypes = new LinkedList<>(this.genericTypes);
        dynamicType.arrayComponentType = this.arrayComponentType;

        dynamicType.upperBounds = new LinkedList<>(this.upperBounds);
        dynamicType.lowerBounds = new LinkedList<>(this.lowerBounds);

        dynamicType.rawType = this.rawType;
        dynamicType.classDescription = this.getClassDescription();
        return dynamicType;
    }

    public DynamicType withAddedGeneric(DynamicType newGeneric) {

        DynamicType dynamicType = new DynamicType();
        dynamicType.importedClasses = new HashSet<>(this.importedClasses);
        dynamicType.genericTypes = new LinkedList<>(this.genericTypes);
        dynamicType.arrayComponentType = this.arrayComponentType;

        dynamicType.genericTypes.add(newGeneric);

        dynamicType.upperBounds = new LinkedList<>(this.upperBounds);
        dynamicType.lowerBounds = new LinkedList<>(this.lowerBounds);

        dynamicType.addImport(newGeneric.classDescription);

        dynamicType.rawType = this.rawType;
        dynamicType.classDescription = this.getClassDescription();
        return dynamicType;
    }

    public ClassDescription getClassDescription() {
        return classDescription;
    }

    public String getTypeName() {
        return getClassDescription().getTypeName();
    }


    public String debugString() {
        return "DynamicType{" +
            "importedClasses=" + importedClasses +
            ", genericTypes=" + genericTypes +
            ", classDescription=" + classDescription +
            ", arrayComponentType=" + arrayComponentType +
            ", rawType=" + rawType +
            '}';
    }

    public String toString() {
        String typeName = classDescription.getTypeName();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(typeName);
        if (!genericTypes.isEmpty()) {
            stringBuilder.append("<");
            for (int i = 0; i < genericTypes.size(); i++) {
                DynamicType genericType = genericTypes.get(i);
                stringBuilder.append(genericType.toString());
                if (i < genericTypes.size() - 1)
                    stringBuilder.append(", ");
            }
            stringBuilder.append(">");
        }
        return stringBuilder.toString();
    }

    public String toStringWithPackage() {
        return classDescription.getPackageName() + "." + toString();
    }

    public Set<ClassDescription> getImportedClasses() {
        return Set.copyOf(importedClasses);
    }

    public boolean compareWithoutGenerics(DynamicType dynamicType) {
        return dynamicType.classDescription.equals(this.classDescription);
    }

    private void addImport(ClassDescription classDescription) {
        if (classDescription.getDeclaringParentClass() != null)
            addImport(classDescription.getDeclaringParentClass());
        else
            this.importedClasses.add(classDescription);
    }

    private void removeImport(ClassDescription classDescription) {
        if (classDescription.getDeclaringParentClass() != null)
            removeImport(classDescription.getDeclaringParentClass());
        else
            this.importedClasses.remove(classDescription);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DynamicType that = (DynamicType) o;
        return Objects.equals(importedClasses, that.importedClasses) && Objects.equals(genericTypes, that.genericTypes) && Objects.equals(arrayComponentType, that.arrayComponentType) && Objects.equals(rawType, that.rawType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(importedClasses, genericTypes, arrayComponentType, rawType);
    }

    public static boolean isPrimitiveType(Type type) {
        return type.equals(int.class) || type.equals(boolean.class) || type.equals(double.class) ||
            type.equals(long.class) || type.equals(short.class) || type.equals(byte.class) ||
            type.equals(char.class) || type.equals(float.class) || type.equals(void.class);
    }

    public @Nullable Class<?> getRawType() {
        return rawType;
    }

    public @Nullable Class<?> getArrayComponentType() {
        return arrayComponentType;
    }

    public List<DynamicType> getGenericTypes() {
        return genericTypes;
    }
}