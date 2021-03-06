/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Steven Willems
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package be.idevelop.fiber;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class SerializerConfig {

    private static final Map<Class, Class> PRIMITIVE_WRAPPER_MAP = new HashMap<Class, Class>(8);

    static {
        PRIMITIVE_WRAPPER_MAP.put(int.class, Integer.class);
        PRIMITIVE_WRAPPER_MAP.put(boolean.class, Boolean.class);
        PRIMITIVE_WRAPPER_MAP.put(byte.class, Byte.class);
        PRIMITIVE_WRAPPER_MAP.put(char.class, Character.class);
        PRIMITIVE_WRAPPER_MAP.put(double.class, Double.class);
        PRIMITIVE_WRAPPER_MAP.put(short.class, Short.class);
        PRIMITIVE_WRAPPER_MAP.put(float.class, Float.class);
        PRIMITIVE_WRAPPER_MAP.put(long.class, Long.class);
    }

    private static final Serializer NULL_SERIALIZER = new NullSerializer();

    private static final Serializer ARRAY_SERIALIZER = new ArraySerializer();

    private final Serializer referenceSerializer = new ReferenceSerializer(this);

    private short nextId = -1;

    private Serializer[] serializers;

    private Map<Class, Serializer> serializerClassMap;

    private Set<Class> registeredClasses;

    private ByteBufferPool byteBufferPool;

    private ObjectCreator objectCreator = new ObjectCreator();

    SerializerConfig() {
        this.byteBufferPool = new ByteBufferPool();

        this.serializers = new Serializer[Short.MAX_VALUE];
        this.serializerClassMap = new HashMap<Class, Serializer>();
        this.registeredClasses = new HashSet<Class>();

        registerSpecialClassesAndSerializers();
        registerPrimitiveSerializers();
        registerJdkCommonClassSerializers();
        registerCollectionSerializers();
        registerMapSerializers();
    }

    private void registerSpecialClassesAndSerializers() {
        register(Object.class);
        registerSpecialSerializer(NULL_SERIALIZER);
        registerSpecialSerializer(referenceSerializer);
        registerSpecialSerializer(ARRAY_SERIALIZER);
        register(new ClassSerializer());
    }

    private void registerPrimitiveSerializers() {
        register(new IntegerSerializer());
        register(new BooleanSerializer());
        register(new ByteSerializer());
        register(new CharSerializer());
        register(new FloatSerializer());
        register(new DoubleSerializer());
        register(new ShortSerializer());
        register(new LongSerializer());
        register(new StringSerializer());
    }

    private void registerJdkCommonClassSerializers() {
        register(new BigIntegerSerializer());
        register(new BigDecimalSerializer());
        register(new DateSerializer());
        register(new CurrencySerializer());
        register(new StringBufferSerializer());
        register(new StringBuilderSerializer());
        register(new UUIDSerializer());
    }

    @SuppressWarnings("unchecked")
    private void registerCollectionSerializers() {
        register(new AbstractListSerializer());
        register(new CollectionSerializer(ArrayList.class));
        register(new TreeSetSerializer());
        register(new CollectionSerializer(HashSet.class));
    }

    @SuppressWarnings("unchecked")
    private void registerMapSerializers() {
        register(new MapSerializer(HashMap.class));
        register(new MapSerializer(LinkedHashMap.class));
        register(new MapSerializer(TreeMap.class));
    }

    @SuppressWarnings("unchecked")
    public void register(Class clazz) {
        if (!clazz.isInterface() && !registeredClasses.contains(clazz)) {
            if (Collection.class.isAssignableFrom(clazz)) {
                registeredClasses.add(clazz);
                register(new CollectionSerializer(clazz));
            } else if (clazz.isArray()) {
                register(clazz.getComponentType());
            } else if (Map.class.isAssignableFrom(clazz)) {
                registeredClasses.add(clazz);
                register(new MapSerializer(clazz));
            } else if (clazz.isEnum()) {
                registeredClasses.add(clazz);
                register(new EnumSerializer(clazz));
            } else if (!clazz.isPrimitive() && !serializerClassMap.containsKey(clazz)) {
                registeredClasses.add(clazz);
                register(new ObjectSerializer(clazz, this));
            }
        }
    }

    private void registerSpecialSerializer(Serializer serializer) {
        serializer.setId(++nextId);
        this.serializers[nextId] = serializer;
    }

    public void register(Serializer serializer) {
        Class serializedClass = convertPrimitiveClassIfNeeded(serializer.getSerializedClass());
        if (!this.serializerClassMap.containsKey(serializedClass)) {
            serializer.setId(++nextId);
            this.serializers[nextId] = serializer;
            if (serializedClass != null) {
                this.serializerClassMap.put(serializedClass, serializer);
                if (serializer instanceof GenericObjectSerializer) {
                    ((GenericObjectSerializer) serializer).registerObjectCreator(objectCreator);
                    this.objectCreator.registerClass(serializer.getSerializedClass(), serializer.getId());
                }
            }
        }
    }

    private Class convertPrimitiveClassIfNeeded(Class clazz) {
        return clazz != null && clazz.isPrimitive() ? PRIMITIVE_WRAPPER_MAP.get(clazz) : clazz;
    }

    @SuppressWarnings("unchecked")
    public <T> Serializer<? super T> getSerializer(T o) {
        if (o == null) {
            return NULL_SERIALIZER;
        } else {
            return getSerializerForClass((Class<T>) o.getClass());
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Serializer<? super T> getSerializerForClass(Class<T> clazz) {
        if (clazz.isArray()) {
            return ARRAY_SERIALIZER;
        } else if (serializerClassMap.containsKey(clazz)) {
            return serializerClassMap.get(clazz);
        } else if (!clazz.isEnum() && clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
            return getSerializerForClass(clazz.getSuperclass());
        }
        throw new IllegalArgumentException("Type not supported " + clazz.getName());
    }

    public Serializer getSerializer(short id) {
        return serializers[id];
    }

    short getClassId(Class clazz) {
        if (clazz.isPrimitive()) {
            return this.serializerClassMap.get(PRIMITIVE_WRAPPER_MAP.get(clazz)).getId();
        } else {
            return getSerializerForClass(clazz).getId();
        }
    }

    Class getClassForId(short classId) {
        return this.serializers[classId].getSerializedClass();
    }

    public ByteBufferPool getByteBufferPool() {
        return byteBufferPool;
    }

    Serializer getReferenceSerializer() {
        return referenceSerializer;
    }

    IntType getIntTypeForClassId() {
        return nextId <= Byte.MAX_VALUE ? IntType.BYTE : IntType.SHORT;
    }

    private static final class NullSerializer extends Serializer {

        @SuppressWarnings("unchecked")
        private NullSerializer() {
            super(null);
        }

        @Override
        public Object read(Input input) {
            return null;
        }

        @Override
        public void write(Object object, Output output) {
        }

        @Override
        public boolean isImmutable() {
            return true;
        }
    }
}
