/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.serialization;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.jppf.JPPFException;
import org.jppf.utils.Pair;
import org.jppf.utils.collections.SoftReferenceValuesMap;
import org.slf4j.*;

/**
 * This helper class provides utility methods to facilitate the JPPF-sepcific serialization and deserilaization.
 * @author Laurent Cohen
 * @exclude
 */
public final class SerializationReflectionHelper {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SerializationReflectionHelper.class);
  /**
   * Default byte value.
   */
  private static final Byte DEFAULT_BYTE = Byte.valueOf((byte) 0);
  /**
   * Default short value.
   */
  private static final Short DEFAULT_SHORT = Short.valueOf((short) 0);
  /**
   * Default int value.
   */
  private static final Integer DEFAULT_INT = Integer.valueOf(0);
  /**
   * Default long value.
   */
  private static final Long DEFAULT_LONG = Long.valueOf(0L);
  /**
   * Default float value.
   */
  private static final Float DEFAULT_FLOAT = Float.valueOf(0f);
  /**
   * Default double value.
   */
  private static final Double DEFAULT_DOUBLE = Double.valueOf(0d);
  /**
   * Default char value.
   */
  private static final Character DEFAULT_CHAR = Character.valueOf((char) 0);
  /**
   * Default boolean value.
   */
  private static final Boolean DEFAULT_BOOLEAN = Boolean.FALSE;
  /**
   * Default object reference value.
   */
  private static final Object DEFAULT_REF = null;
  /**
   * COnstant for empty field array.
   */
  private static final Field[] NO_FIELDS = new Field[0];
  /**
   * A cache of readObject() methods for deserialization.
   */
  private static final Map<Class<?>, Object> READ_OBJECT_MAP = new SoftReferenceValuesMap<>();
  /**
   * A cache of writeObject() methods for serialization.
   */
  private static final Map<Class<?>, Object> WRITE_OBJECT_MAP = new SoftReferenceValuesMap<>();
  /**
   * A cache of non transient fields.
   */
  private static final Map<Class<?>, Field[]> FIELDS_MAP = new SoftReferenceValuesMap<>();
  /**
   * 
   */
  private static final Object NO_MEMBER = new Object();
  /**
   * The field "private final char{] value" in String.
   */
  private static Field STRING_VALUE_FIELD;
  static {
    try {
      STRING_VALUE_FIELD = String.class.getDeclaredField("value");
      STRING_VALUE_FIELD.setAccessible(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Get all declared non-transient and non-static fields of the given class.
   * @param clazz the class object from which to extract the fields.
   * @return an array of {@link Field} objects.
   * @throws Exception if any error occurs.
   */
  public static Field[] getNonTransientDeclaredFields(final Class<?> clazz) throws Exception {
    Field[] result = null;
    synchronized(FIELDS_MAP) {
      result = FIELDS_MAP.get(clazz);
    }
    if (result == null) {
      Field[] allFields = clazz.getDeclaredFields();
      if (allFields.length <= 0) result = NO_FIELDS;
      else {
        Field[] fields = new Field[allFields.length];
        int count = 0;
        for (Field f : allFields) {
          int mod = f.getModifiers();
          if (!Modifier.isTransient(mod) && !Modifier.isStatic(mod)) fields[count++] = f;
        }
        if (count == 0) result = NO_FIELDS;
        else if (count == allFields.length) result = allFields;
        else {
          result = new Field[count];
          System.arraycopy(fields, 0, result, 0, count);
        }
      }
      synchronized(FIELDS_MAP) {
        FIELDS_MAP.put(clazz, result);
      }
    }
    return result;
  }

  /**
   * Get a unique string representation for the specified type.
   * @param clazz the type from which to get the signature.
   * @return a string representing the type.
   * @throws Exception if any error occurs.
   */
  public static String getSignatureFromType(final Class<?> clazz) throws Exception {
    StringBuilder sb = new StringBuilder();
    Class<?> tmp = clazz;
    while (tmp.isArray()) {
      sb.append('[');
      tmp = tmp.getComponentType();
    }
    if (tmp == Byte.TYPE) sb.append('B');
    else if (tmp == Short.TYPE) sb.append('S');
    else if (tmp == Integer.TYPE) sb.append('I');
    else if (tmp == Long.TYPE) sb.append('J');
    else if (tmp == Float.TYPE) sb.append('F');
    else if (tmp == Double.TYPE) sb.append('D');
    else if (tmp == Boolean.TYPE) sb.append('Z');
    else if (tmp == Character.TYPE) sb.append('C');
    else sb.append('L').append(tmp.getName());
    return sb.toString();
  }

  /**
   * Lookup or load the non-array class based on the specified signature.
   * @param signature the class signature.
   * @param cl the class loader used to load the class.
   * @return a {@link Class} object.
   * @throws Exception if any error occurs.
   */
  public static Class<?> getTypeFromSignature(final String signature, final ClassLoader cl) throws Exception {
    if (signature.charAt(0) != '[') return getNonArrayTypeFromSignature(signature, cl);
    int pos = 0;
    List<Class<?>> types = new ArrayList<>();
    while (signature.charAt(pos) == '[') pos++;
    Class<?> componentType = getNonArrayTypeFromSignature(signature.substring(pos), cl);
    int[] dimensions = new int[pos];
    Arrays.fill(dimensions, 0);
    Class<?> c = Array.newInstance(componentType, dimensions).getClass();
    return c;
  }

  /**
   * Lookup or load the non-array class based on the specified signature.
   * @param signature the class signature.
   * @param cl the class loader used to load the class.
   * @return a {@link Class} object.
   * @throws Exception if any error occurs.
   */
  public static Class<?> getNonArrayTypeFromSignature(final String signature, final ClassLoader cl) throws Exception {
    switch (signature.charAt(0)) {
      case 'B':
        return Byte.TYPE;
      case 'S':
        return Short.TYPE;
      case 'I':
        return Integer.TYPE;
      case 'J':
        return Long.TYPE;
      case 'F':
        return Float.TYPE;
      case 'D':
        return Double.TYPE;
      case 'C':
        return Character.TYPE;
      case 'Z':
        return Boolean.TYPE;
      case 'L':
        String s = signature.substring(1);
        if ("void".equals(s)) return Void.TYPE;
        return cl.loadClass(s);
    }
    throw new JPPFException("Could not load type with signature '" + signature + '\'');
  }

  /**
   * Get the writeObject() method with the signature specified in {@link Serializable}.
   * @param clazz the class for which to get the method.
   * @return the desired method, or {@code null} if th emthod could not be found.
   * @throws Exception if any error occurs.
   */
  public static Method getWriteObjectMethod(final Class<?> clazz) throws Exception {
    return getReadOrWriteObjectMethod(clazz, false);
  }

  /**
   * Get the readObject() method with the signature specified in {@link Serializable}.
   * @param clazz the class for which to get the method.
   * @return the desired method, or {@code null} if th emthod could not be found.
   * @throws Exception if any error occurs.
   */
  public static Method getReadObjectMethod(final Class<?> clazz) throws Exception {
    return getReadOrWriteObjectMethod(clazz, true);
  }

  /**
   * Get the {@code readObject()} or {@code writeObject()} method with the signature specified in {@link Serializable}.
   * @param clazz the class for which to get the method.
   * @param isRead whether we are looking for a {@code readObject()} or {@code writeObject()} method.
   * @return the desired method, or {@code null} if the mthod could not be found.
   * @throws Exception if any error occurs.
   */
  private static Method getReadOrWriteObjectMethod(final Class<?> clazz, final boolean isRead) throws Exception {
    Object method = null;
    Map<Class<?>, Object> map = isRead ? READ_OBJECT_MAP : WRITE_OBJECT_MAP;
    synchronized(map) {
      method = map.get(clazz);
    }
    if (method == NO_MEMBER) return null;
    else if (method != null) return (Method) method;
    String methodName = isRead ? "readObject" : "writeObject";
    Class<?> paramType = isRead ? ObjectInputStream.class : ObjectOutputStream.class;
    // iterating over getDeclaredMethods() is much faster than calling getDeclaredMethod(String, Class<?>...)
    for (Method m: clazz.getDeclaredMethods()) {
      if (methodName.equals(m.getName())) {
        Class<?>[] paramTypes = m.getParameterTypes();
        if ((paramTypes.length == 1) && (paramTypes[0] == paramType)) {
          int n = m.getModifiers();
          if (!Modifier.isStatic(n) && Modifier.isPrivate(n)) {
            method = m;
            break;
          }
        }
      }
    }
    synchronized(map) {
      map.put(clazz, method == null ? NO_MEMBER : method);
    }
    return (Method) method;
  }

  /**
   * Class object for the reflection factory.
   */
  private static Class<?> rfClass = null;
  /**
   * A reflection factory instance, used to create instances of deserialized classes without calling a constructor.
   * If it cannot be created (for example on non Sun-based JVMs), then we lookup the constructors of the class until we
   * find one that doesn't throw an exception upon invocation. Default values are used if the constructor has parameters.
   */
  private static Object rf = null;
  /**
   * The method to invoke on the reflection factory to create a new instance of a deserialized object.
   */
  private static Method rfMethod = null;

  static {
    try {
      rfClass = Class.forName("sun.reflect.ReflectionFactory");
    } catch (Throwable t) {
    }
    if (rfClass != null) {
      rf = initializeRF();
      if (rf != null) rfMethod = initializeRFMethod();
    }
  }

  /**
   * Initialize the reflection factory. The reflection factory is an internal JDK class in a sun.* package.
   * It may not be available on JVM implementations that are not based on Sun's. We use purely reflective calls
   * to create it, to avoid static dependencies on internal APIs.
   * @return a <code>sun.reflect.ReflectionFactory.ReflectionFactory</code> if it can be created, null otherwise.
   */
  private static Object initializeRF() {
    try {
      Method m = rfClass.getDeclaredMethod("getReflectionFactory");
      Object o = m.invoke(null);
      return o;
    } catch (Throwable t) {
    }
    return null;
  }

  /**
   * Initialize the reflection factory. The reflection factory is an internal JDK class in a sun.* package.
   * It may not be available on JVM implementations that are not based on Sun's. We use purely reflective calls
   * to create it, to avoid static dependencies on internal APIs.
   * @return a <code>sun.reflect.ReflectionFactory.ReflectionFactory</code> if it can be created, null otherwise.
   */
  private static Method initializeRFMethod() {
    try {
      Method m = rfClass.getDeclaredMethod("newConstructorForSerialization", Class.class, Constructor.class);
      return m;
    } catch (Throwable t) {
    }
    return null;
  }

  /**
   * A cache of constructors used for deserialization.
   */
  private static final Map<Class<?>, Constructor> CONSTRUCTOR_MAP = new SoftReferenceValuesMap<>();

  /**
   * Create an object without calling any of its class constructors if the JVM supports it,
   * or failing that, attempt to call the constructor with the smallest possible number of arguments.
   * @param clazz the object's class.
   * @return the newly created object.
   * @throws Exception if any error occurs.
   */
  public static Object create(final Class<?> clazz) throws Exception {
    //return createFromConstructor(clazz);
    if (rfMethod == null) return createFromConstructor(clazz);
    return create(clazz, Object.class);
  }

  /**
   * Create an object without calling any of its class constructors,
   * and calling the superclass no-arg constructor.
   * @param clazz the object's class.
   * @param parent the object's super class.
   * @return the newly created object.
   * @throws Exception if any error occurs.
   */
  static Object create(final Class<?> clazz, final Class<?> parent) throws Exception {
    try {
      Constructor constructor;
      synchronized (CONSTRUCTOR_MAP) {
        constructor = CONSTRUCTOR_MAP.get(clazz);
      }
      if (constructor == null) {
        //ReflectionFactory rf = ReflectionFactory.getReflectionFactory();
        Constructor superConstructor = parent.getDeclaredConstructor();
        //constructor = rf.newConstructorForSerialization(clazz, superConstructor);
        constructor = (Constructor) rfMethod.invoke(rf, clazz, superConstructor);
        synchronized (CONSTRUCTOR_MAP) {
          CONSTRUCTOR_MAP.put(clazz, constructor);
        }
      }
      return clazz.cast(constructor.newInstance());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Cannot create object", e);
    }
  }

  /**
   * A cache of constructors used for deserialization.
   */
  private static final Map<Class<?>, ConstructorWithParameters> DEFAULT_CONSTRUCTOR_MAP = new SoftReferenceValuesMap<>();

  /**
   * Instantiate an object from one of its class' existing constructor.
   * The constructors of the class are looked up, until we can find one whose invocation with default parameter values doesn't fail.
   * @param clazz the class for the object to create.
   * @return an instance of the specified class.
   * @throws Exception if any error occurs.
   */
  static Object createFromConstructor(final Class<?> clazz) throws Exception {
    ConstructorWithParameters cwp = null;
    synchronized (DEFAULT_CONSTRUCTOR_MAP) {
      cwp = DEFAULT_CONSTRUCTOR_MAP.get(clazz);
    }
    if (cwp == null) {
      Constructor<?>[] constructors = clazz.getDeclaredConstructors();
      Arrays.sort(constructors, new ConstructorComparator());
      for (Constructor<?> c : constructors) {
        Class<?>[] paramTypes = c.getParameterTypes();
        Object[] params = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) params[i] = defaultValue(paramTypes[i]);
        if (!c.isAccessible()) c.setAccessible(true);
        try {
          Object result = c.newInstance(params);
          cwp = new ConstructorWithParameters(c, params);
          synchronized (DEFAULT_CONSTRUCTOR_MAP) {
            DEFAULT_CONSTRUCTOR_MAP.put(clazz, cwp);
          }
          return result;
        } catch (Throwable t) {
          log.info(t.getMessage(), t);
        }
      }
    } else return cwp.first().newInstance(cwp.second());
    throw new InstantiationException("Could not create an instance of " + clazz);
  }

  /**
   * Compares two constructors based on their number of parameters.
   */
  private static class ConstructorComparator implements Comparator<Constructor<?>>, Serializable {
    @Override
    public int compare(final Constructor<?> c1, final Constructor<?> c2) {
      int n1 = c1.getParameterTypes().length;
      int n2 = c2.getParameterTypes().length;
      return n1 < n2 ? -1 : (n1 > n2 ? 1 : 0);
    }
  }

  /**
   * A class associating a constructor with a set of default values for its parameters.
   */
  private static class ConstructorWithParameters extends Pair<Constructor<?>, Object[]> {
    /**
     * Initialize this object with the specified constructors and parameters.
     * @param constructor the constructor.
     * @param params the array of parameters, may be null or empty.
     */
    ConstructorWithParameters(final Constructor<?> constructor, final Object... params) {
      super(constructor, params);
    }
  }

  /**
   * Get a default value for the specified type.
   * @param c the type for which to get a value.
   * @return a valid default value for the type.
   */
  private static Object defaultValue(final Class<?> c) {
    if ((c == Byte.TYPE) || (c == Byte.class)) return DEFAULT_BYTE;
    else if ((c == Short.TYPE) || (c == Short.class)) return DEFAULT_SHORT;
    else if ((c == Integer.TYPE) || (c == Integer.class)) return DEFAULT_INT;
    else if ((c == Long.TYPE) || (c == Long.class)) return DEFAULT_LONG;
    else if ((c == Float.TYPE) || (c == Float.class)) return DEFAULT_FLOAT;
    else if ((c == Double.TYPE) || (c == Double.class)) return DEFAULT_DOUBLE;
    else if ((c == Character.TYPE) || (c == Character.class)) return DEFAULT_CHAR;
    else if ((c == Boolean.TYPE) || (c == Boolean.class)) return DEFAULT_BOOLEAN;
    return DEFAULT_REF;
  }

  /**
   * Create a string from the specified array of characters.
   * @param chars the string's value.
   * @return a new string with the specified characters.
   * @throws Exception if any error occurs.
   */
  public static String createString(final char[] chars) throws Exception {
    String s = new String();
    STRING_VALUE_FIELD.set(s, chars);
    return s;
  }

  /**
   * Get the chars in a string without copying the array.
   * @param s the string to get the chars from.
   * @return an array of chars.
   * @throws Exception if any error occurs.
   */
  public static char[] getStringValue(final String s) throws Exception {
    return (char[]) STRING_VALUE_FIELD.get(s);
  }
}
