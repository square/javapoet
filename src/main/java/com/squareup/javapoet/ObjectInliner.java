package com.squareup.javapoet;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public class ObjectInliner {
    private static final String DEFAULT_NAME_PREFIX = "$$javapoet$";
    private static final ObjectInliner DEFAULT = new ObjectInliner();
    // We need to use object identity as a key in the map
    private final IdentityHashMap<Object, String> pojoNameMap = new IdentityHashMap<>();
    private final Set<Object> possibleCircularRecordReferenceSet = Collections
            .newSetFromMap(new IdentityHashMap<>());
    private final List<TypeInliner> typeInliners = new ArrayList<>();

    // You probably should not be using this class
    // if you did not construct the instance yourself.
    /**
     * This is the Set of all exact types we trust,
     * to prevent malicious classes with malicious setters
     * from being generated.
     */
    private final Set<Class<?>> trustedExactTypes = new HashSet<>();

    /**
     * This is the Set of all assignable types we trust,
     * which allows anything that can be assigned to them to
     * be generated. Be careful with these; there can be a
     * malicious subclass you do not know about.
     */
    private final Set<Class<?>> trustedAssignableTypes = new HashSet<>();

    private int inlineLevel = 0;
    private NameAllocator nameAllocator;
    private String suggestedNamePrefix;

    public ObjectInliner() {
        this.suggestedNamePrefix = DEFAULT_NAME_PREFIX;
    }

    public static ObjectInliner getDefault() {
        return DEFAULT;
    }

    public ObjectInliner useNamePrefix(String namePrefix) {
        suggestedNamePrefix = namePrefix;
        return this;
    }

    public ObjectInliner addTypeInliner(TypeInliner typeInliner) {
        typeInliners.add(typeInliner);
        return this;
    }

    /**
     * Trust everything assignable to the given type. Do not call this method
     * if users can create subclasses of that type, since this will allow
     * the user to execute arbitrary code in the generated class.
     */
    public ObjectInliner trustTypesAssignableTo(Class<?> assignableType) {
        trustedAssignableTypes.add(assignableType);
        return this;
    }

    /**
     * Trust the exact types reachable from the given type.
     * These are:
     * <ul>
     * <li>The type of its fields.
     * <li>The type of any fields inherited from a superclass.
     * <li>The type and its supertypes.
     * </ul>
     */
    public ObjectInliner trustExactTypes(Class<?> exactType) {
        if (trustedExactTypes.contains(exactType)) {
            return this;
        }
        trustedExactTypes.add(exactType);
        for (Field field : exactType.getDeclaredFields()) {
            trustExactTypes(field.getType());
        }
        if (exactType.getSuperclass() != null) {
            trustExactTypes(exactType.getSuperclass());
        }
        return this;
    }

    /**
     * Trust everything. Do not call this method if you are passing in
     * user-generated instances of arbitrary types, since this will allow
     * the user to execute arbitrary code in the generated class.
     */
    public ObjectInliner trustEverything() {
        return trustTypesAssignableTo(Object.class);
    }

    public static class Inlined {
        private final ObjectInliner inliner;
        private final Object value;

        public Inlined(ObjectInliner inliner, Object value) {
            this.inliner = inliner;
            this.value = value;
        }

        public ObjectInliner getInliner() {
            return inliner;
        }

        public Object getValue() {
            return value;
        }

        void emit(CodeWriter codeWriter) throws IOException  {
            inliner.emit(codeWriter, value);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object)
                return true;
            if (object == null || getClass() != object.getClass())
                return false;
            Inlined inlined = (Inlined) object;
            return Objects.equals(inliner, inlined.inliner)
                    && Objects.equals(value, inlined.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(inliner, value);
        }
    }

    public Inlined inlined(Object object) {
        return new Inlined(this, object);
    }

    void emit(CodeWriter builder, Object object) throws IOException {
        if (object == null) {
            builder.emit("null");
            return;
        }

        // Inline tracks how many nested inline calls there are.
        // The one at the top level is responsible for creating the
        // name allocator; inner inline calls must use the same allocator
        // to provide name clashes.
        if (inlineLevel == 0) {
            nameAllocator = new NameAllocator(new LinkedHashSet<>(),
                    pojoNameMap);
        }
        inlineLevel++;

        // Does a neat trick, so we can get a code block in a fragment
        // It defines an inline supplier and immediately calls it.
        try {
            builder.emit("(($T)(($T)(()->{", getSerializedType(object.getClass()), Supplier.class);
            builder.emit("\nreturn $L;\n})).get())", getInlinedPojo(builder, object));
        } finally {
            inlineLevel--;
            // There are no inner inline calls, so we can clear the name allocator
            // (since the names are scoped inside the supplier lambda block, which is now
            // closed).
            if (inlineLevel == 0) {
                pojoNameMap.clear();
                nameAllocator = null;
            }
        }
    }

    /**
     * Serializes a Pojo to code that uses its no-args constructor
     * and setters to create the object.
     *
     * @param pojo The object to be serialized.
     * @return A string that can be used in a {@link CodeBlock.Builder} to access the object
     */
    String getInlinedPojo(CodeWriter builder, Object pojo) throws IOException  {
        // First, check for primitives
        if (pojo == null) {
            return "null";
        }
        if (pojo instanceof Boolean) {
            return pojo.toString();
        }
        if (pojo instanceof Byte) {
            // Cast to byte
            return "((byte) " + pojo + ")";
        }
        if (pojo instanceof Character) {
            return "'\\u" + Integer.toHexString(((char) pojo) | 0x10000)
                    .substring(1) + "'";
        }
        if (pojo instanceof Short) {
            // Cast to short
            return "((short) " + pojo + ")";
        }
        if (pojo instanceof Integer) {
            return pojo.toString();
        }
        if (pojo instanceof Long) {
            // Add long suffix to number string
            return pojo + "L";
        }
        if (pojo instanceof Float) {
            // Add float suffix to number string
            return pojo + "f";
        }
        if (pojo instanceof Double) {
            // Add double suffix to number string
            return pojo + "d";
        }

        // Check for builtin classes
        if (pojo instanceof String) {
            return CodeBlock.builder().add("$S", pojo).build().toString();
        }
        if (pojo instanceof Class<?>) {
            Class<?> value = (Class<?>) pojo;
            if (!Modifier.isPublic(value.getModifiers())) {
                throw new IllegalArgumentException("Cannot serialize (" + value
                        + ") because it is not a public class.");
            }
            return value.getCanonicalName() + ".class";
        }
        if (pojo.getClass().isEnum()) {
            // Use field access to read the enum
            Class<?> enumClass = pojo.getClass();
            Enum<?> pojoEnum = (Enum<?>) pojo;
            if (!Modifier.isPublic(enumClass.getModifiers())) {
                // Use name() since toString() can be malicious
                throw new IllegalArgumentException(
                        "Cannot serialize (" + pojoEnum.name()
                                + ") because its type (" + enumClass
                                + ") is not a public class.");
            }

            return enumClass.getCanonicalName() + "." + pojoEnum.name();
        }

        // We need to use a custom in-liner, which will potentially
        // call methods on a user-supplied instances. Make sure we trust
        // the type before continuing
        if (!isTrustedType(pojo.getClass())) {
            throw new IllegalArgumentException("Cannot serialize instance of ("
                    + pojo.getClass() + ") because it is not an instance of a trusted type.");
        }

        // Check if any registered TypeInliner matches the class
        Class<?> type = pojo.getClass();
        for (TypeInliner typeInliner : typeInliners) {
            if (typeInliner.canInline(type)) {
                return typeInliner.inline(this, pojo);
            }
        }

        return getInlinedComplexPojo(builder, pojo);
    }

    private boolean isTrustedType(Class<?> query) {
        if (query.isArray()) {
            return query.getComponentType().isPrimitive()
                    || isTrustedType(query.getComponentType());
        }
        for (Class<?> trustedAssignableType : trustedAssignableTypes) {
            if (trustedAssignableType.isAssignableFrom(query)) {
                return true;
            }
        }
        for (Class<?> trustedExactType : trustedExactTypes) {
            if (trustedExactType.equals(query)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return a string that can be used in a {@link CodeBlock.Builder} to
     * access a complex object.
     *
     * @param pojo The object to be accessed
     * @return A string that can be used in a {@link CodeBlock.Builder} to
     * access the object.
     */
    private String getPojoValue(Object pojo) {
        return nameAllocator.get(pojo);
    }

    private static boolean isRecord(Object object) {
        Class<?> superClass = object.getClass().getSuperclass();
        return superClass != null && superClass.getName()
                .equals("java.lang.Record");
    }

    /**
     * Serializes collections and complex POJOs to code.
     */
    private String getInlinedComplexPojo(CodeWriter builder, Object pojo) throws IOException {
        if (possibleCircularRecordReferenceSet.contains(pojo)) {
            // Records do not have a no-args constructor, so we cannot safely
            // serialize self-references in records
            // as we cannot do a map lookup before the record is created.
            throw new IllegalArgumentException(
                    "Cannot serialize record of type (" + pojo.getClass()
                            + ") because it is a record containing a circular reference.");
        }

        // If we already serialized the object, we should just return
        // the code string
        if (pojoNameMap.containsKey(pojo)) {
            return getPojoValue(pojo);
        }
        if (isRecord(pojo)) {
            // Records must set all fields at initialization time,
            // so we delay the declaration of its variable
            return getInlinedRecord(builder, pojo);
        }
        // Object is not serialized yet
        // Create a new variable to store its value when setting its fields
        String newIdentifier = nameAllocator.newName(suggestedNamePrefix
                + getSerializedType(pojo.getClass()).getSimpleName(), pojo);

        // First, check if it is a collection type
        if (pojo.getClass().isArray()) {
            return getInlinedArray(builder, newIdentifier, pojo);
        }
        if (pojo instanceof List) {
            return getInlinedList(builder, newIdentifier, (List<?>) pojo);
        }
        if (pojo instanceof Set) {
            return getInlinedSet(builder, newIdentifier, (Set<?>) pojo);
        }
        if (pojo instanceof Map) {
            return getInlinedMap(builder, newIdentifier, (Map<?, ?>) pojo);
        }

        if (!Modifier.isPublic(pojo.getClass().getModifiers())) {
            throw new IllegalArgumentException("Cannot serialize type (" + pojo.getClass()
                    + ") because it is not public.");
        }
        builder.emit("\n$T $N;", pojo.getClass(), newIdentifier);
        try {
            Constructor<?> constructor = pojo.getClass().getConstructor();
            if (!Modifier.isPublic(constructor.getModifiers())) {
                throw new IllegalArgumentException("Cannot serialize type (" + pojo.getClass()
                        + ") because its no-args constructor is not public.");
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Cannot serialize type (" + pojo.getClass()
                    + ") because it does not have a public no-args constructor.");
        }
        builder.emit("\n$N = new $T();", newIdentifier, pojo.getClass());
        inlineFieldsOfPojo(builder, pojo.getClass(), newIdentifier, pojo);
        return getPojoValue(pojo);
    }

    private String getInlinedArray(CodeWriter builder, String newIdentifier,
            Object array) throws IOException {
        Class<?> componentType = array.getClass().getComponentType();
        if (!Modifier.isPublic(componentType.getModifiers())) {
            throw new IllegalArgumentException(
                    "Cannot serialize array of type (" + componentType
                            + ") because (" + componentType + ") is not public.");
        }
        builder.emit("\n$T $N;", array.getClass(), newIdentifier);

        // Get the length of the array
        int length = Array.getLength(array);

        // Create a new array from the component type with the given length
        builder.emit("\n$N = new $T[$L];", newIdentifier,
                componentType, Integer.toString(length));
        for (int i = 0; i < length; i++) {
            // Set the elements of the array
            builder.emit("\n$N[$L] = $L;",
                    newIdentifier,
                    Integer.toString(i),
                    getInlinedPojo(builder, Array.get(array, i)));
        }
        return getPojoValue(array);
    }

    private String getInlinedList(CodeWriter builder, String newIdentifier,
            List<?> list) throws IOException  {
        builder.emit("\n$T $N;", List.class, newIdentifier);

        // Create an ArrayList
        builder.emit("\n$N = new $T($L);", newIdentifier, ArrayList.class,
                Integer.toString(list.size()));
        for (Object item : list) {
            // Add each item of the list to the ArrayList
            builder.emit("\n$N.add($L);",
                    newIdentifier,
                    getInlinedPojo(builder, item));
        }
        return getPojoValue(list);
    }

    private String getInlinedSet(CodeWriter builder, String newIdentifier,
            Set<?> set) throws IOException  {
        builder.emit("\n$T $N;", Set.class, newIdentifier);

        // Create a new HashSet
        builder.emit("\n$N = new $T($L);", newIdentifier, LinkedHashSet.class,
                Integer.toString(set.size()));
        for (Object item : set) {
            // Add each item of the set to the HashSet
            builder.emit("\n$N.add($L);",
                    newIdentifier,
                    getInlinedPojo(builder, item));
        }
        return getPojoValue(set);
    }

    private String getInlinedMap(CodeWriter builder, String newIdentifier,
            Map<?, ?> map) throws IOException  {
        builder.emit("\n$T $N;", Map.class, newIdentifier);

        // Create a HashMap
        builder.emit("\n$N = new $T($L);", newIdentifier, LinkedHashMap.class,
                Integer.toString(map.size()));
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            // Put each entry of the map into the HashMap
            builder.emit("\n$N.put($L, $L);",
                    newIdentifier,
                    getInlinedPojo(builder, entry.getKey()),
                    getInlinedPojo(builder, entry.getValue()));
        }
        return getPojoValue(map);
    }

    // Workaround for Java 8
    private static final class RecordComponent {
        private final Class<?> type;
        private final String name;

        private RecordComponent(Class<?> type, String name) {
            this.type = type;
            this.name = name;
        }

        public Class<?> getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public Object getValue(Object record) {
            try {
                return record.getClass().getMethod(name).invoke(record);
            } catch (InvocationTargetException | IllegalAccessException
                    | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Workaround for Java 8
    private static RecordComponent[] getRecordComponents(Class<?> recordClass) {
        try {
            Object[] components = (Object[]) recordClass.
                    getMethod("getRecordComponents").invoke(recordClass);
            RecordComponent[] out = new RecordComponent[components.length];
            for (int i = 0; i < components.length; i++) {
                Object component = components[i];
                Class<?> componentClass = component.getClass();
                Class<?> type = (Class<?>) componentClass
                        .getMethod("getType").invoke(component);
                String name = (String) componentClass
                        .getMethod("getName").invoke(component);
                out[i] = new RecordComponent(type, name);
            }
            return out;
        } catch (InvocationTargetException | IllegalAccessException
                | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private String getInlinedRecord(CodeWriter builder, Object record)
            throws IOException  {
        possibleCircularRecordReferenceSet.add(record);
        Class<?> recordClass = record.getClass();
        if (!Modifier.isPublic(recordClass.getModifiers())) {
            throw new IllegalArgumentException(
                    "Cannot serialize record type (" + recordClass
                            + ") because it is not public.");
        }

        RecordComponent[] recordComponents = getRecordComponents(recordClass);
        String[] componentAccessors = new String[recordComponents.length];
        for (int i = 0; i < recordComponents.length; i++) {
            Object value;
            Class<?> serializedType = getSerializedType(recordComponents[i].getType());
            if (!recordComponents[i].getType().equals(serializedType)) {
                throw new IllegalArgumentException(
                        "Cannot serialize type (" + recordClass
                                + ") as its component (" + recordComponents[i].getName()
                                + ") uses an implementation of a collection ("
                                + recordComponents[i].getType()
                                + ") instead of the interface type ("
                                + serializedType + ").");
            }
            value = recordComponents[i].getValue(record);
            try {
                componentAccessors[i] = getInlinedPojo(builder, value);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Cannot serialize record type ("
                        + record.getClass() + ") because the type of its value ("
                        + value.getClass() + ") for its component ("
                        + recordComponents[i].getName() + ") is not serializable.", e);
            }
        }
        // All components serialized, so no circular references
        possibleCircularRecordReferenceSet.remove(record);
        StringBuilder constructorArgs = new StringBuilder();
        for (String componentAccessor : componentAccessors) {
            constructorArgs.append(componentAccessor).append(", ");
        }
        if (componentAccessors.length != 0) {
            constructorArgs.delete(constructorArgs.length() - 2,
                    constructorArgs.length());
        }
        String newIdentifier = nameAllocator.newName(suggestedNamePrefix
                + recordClass.getSimpleName(), record);
        builder.emit("\n$T $N = new $T($L);", recordClass, newIdentifier,
                recordClass, constructorArgs.toString());
        return getPojoValue(record);
    }

    static Class<?> getSerializedType(Class<?> query) {
        if (List.class.isAssignableFrom(query)) {
            return List.class;
        }
        if (Set.class.isAssignableFrom(query)) {
            return Set.class;
        }
        if (Map.class.isAssignableFrom(query)) {
            return Map.class;
        }
        return query;
    }

    private static Method getSetterMethod(Class<?> expectedArgumentType, Field field) {
        Class<?> declaringClass = field.getDeclaringClass();
        String fieldName = field.getName();

        String methodName = "set" + Character.toUpperCase(fieldName.charAt(0))
                + fieldName.substring(1);
        try {
            return declaringClass.getMethod(methodName, expectedArgumentType);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Sets the fields of pojo declared in pojoClass and all its superclasses.
     *
     * @param pojoClass A class assignable to pojo containing some of its fields.
     * @param identifier The name of the variable storing the serialized pojo.
     * @param pojo The object being serialized.
     */
    private void inlineFieldsOfPojo(CodeWriter builder, Class<?> pojoClass,
            String identifier, Object pojo) throws IOException  {
        if (pojoClass == Object.class) {
            // We are the top-level, no more fields to set
            return;
        }
        Field[] fields = pojoClass.getDeclaredFields();
        // Sort by name to guarantee a consistent ordering
        Arrays.sort(fields, Comparator.comparing(Field::getName));
        for (Field field : fields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                // We do not want to write static fields
                continue;
            }
            if (Modifier.isPublic(field.getModifiers())) {
                try {
                    builder.emit("\n$N.$N = $L;", identifier,
                            field.getName(),
                            getInlinedPojo(builder, field.get(pojo)));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }
            // Set the field accessible so we can read its value
            field.setAccessible(true);
            Class<?> serializedType = getSerializedType(field.getType());
            Method setterMethod = getSetterMethod(serializedType, field);
            // setterMethod guaranteed to be public
            if (setterMethod == null) {
                if (!field.getType().equals(serializedType)) {
                    throw new IllegalArgumentException(
                            "Cannot serialize type (" + pojoClass
                                    + ") as its field (" + field.getName()
                                    + ") uses an implementation of a collection ("
                                    + field.getType()
                                    + ") instead of the interface type ("
                                    + serializedType + ").");
                }
                throw new IllegalArgumentException(
                        "Cannot serialize type (" + pojoClass
                                + ") as it is missing a public setter method for field ("
                                + field.getName() + ") of type (" + field.getType() + ").");
            }
            Object value;
            try {
                value = field.get(pojo);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            try {
                // Convert the field value to code, and call the setter
                // corresponding to the field with the serialized field value.
                builder.emit("\n$N.$N($L);", identifier,
                        setterMethod.getName(),
                        getInlinedPojo(builder, value));
            } catch (IllegalArgumentException e) {
                // We trust pojo, but not necessary value
                throw new IllegalArgumentException("Cannot serialize an instance of type ("
                        + pojo.getClass() + ") because the type of its value ("
                        + value.getClass()
                        + ") for its field (" + field.getName()
                        + ") is not serializable.", e);
            }
        }
        try {
            inlineFieldsOfPojo(builder, pojoClass.getSuperclass(), identifier, pojo);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cannot serialize type ("
                    + pojoClass + ") because its superclass ("
                    + pojoClass.getSuperclass() + ") is not serializable.", e);
        }
    }
}
