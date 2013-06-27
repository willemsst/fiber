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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static be.idevelop.fiber.ReferenceResolver.REFERENCE_RESOLVER;

enum ObjectCreator {

    OBJECT_CREATOR;

    private Map<Class, Constructor> constructorMap = new HashMap<Class, Constructor>();

    public void registerClass(Class clazz) {
        if (!Modifier.isAbstract(clazz.getModifiers())) {
            constructorMap.put(clazz, getDefaultConstructor(clazz));
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T createNewInstance(Class<T> clazz, int referenceId) {
        if (constructorMap.containsKey(clazz)) {
            T newInstance = createNewInstance((Constructor<T>) constructorMap.get(clazz));
            REFERENCE_RESOLVER.add(referenceId, newInstance);
            return newInstance;
        } else {
            throw new IllegalArgumentException("No constructor registered for class " + clazz + ".");
        }
    }

    private <T> Constructor<T> getDefaultConstructor(Class<T> clazz) {
        Constructor<T> constructor;
        try {
            constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("No default constructor found for class " + clazz.getName() + ". Implement a custom serializer.");
        }
        return constructor;
    }

    private <T> T createNewInstance(Constructor<T> constructor) {
        T t;
        try {
            t = constructor.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalStateException("Could not invoke default constructor for class " + constructor.getDeclaringClass().getName(), e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not invoke default constructor for class " + constructor.getDeclaringClass().getName(), e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Could not invoke default constructor for class " + constructor.getDeclaringClass().getName(), e);
        }
        return t;
    }

}
