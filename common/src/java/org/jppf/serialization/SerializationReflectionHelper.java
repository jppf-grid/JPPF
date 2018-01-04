/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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
import java.util.concurrent.ConcurrentHashMap;

import org.jppf.JPPFException;
import org.jppf.utils.Pair;
import org.jppf.utils.collections.ConcurrentSoftReferenceValuesMap;
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
   * The modifiers for the declared non persistent fields of a class.
   */
  private static final int NON_PERSISTENT_MODIFIERS = Modifier.TRANSIENT | Modifier.STATIC;
  /**
   * COnstant for empty field array.
   */
  private static final FieldDescriptor[] NO_FIELDS = new FieldDescriptor[0];
  /**
   * A cache of readObject() methods for deserialization.
   */
  private static final Map<Class<?>, Object> READ_OBJECT_MAP = new ConcurrentSoftReferenceValuesMap<>();
  /**
   * A cache of writeObject() methods for serialization.
   */
  private static final Map<Class<?>, Object> WRITE_OBJECT_MAP = new ConcurrentSoftReferenceValuesMap<>();
  /**
   * A cache of non transient fields.
   */
  private static final Map<Class<?>, FieldDescriptor[]> FIELDS_MAP = new ConcurrentSoftReferenceValuesMap<>();
  /**
   * A cache classes to their string signature.
   */
  private static final Map<Class<?>, String> SIGNATURE_MAP = new ConcurrentSoftReferenceValuesMap<>();
  /**
   *
   */
  private static final Object NO_MEMBER = new Object();
  /**
   *
   */
  private static final Set<Class<?>> TRANSIENT_EXCEPTION_CLASSES = initTransientExceptionClasses();
  /**
   * Map of classes to their assigned {@link SerializationHandler}, if any.
   */
  private static final Map<Class<?>, SerializationHandler> handlerMap = new HashMap<>();
  static {
    handlerMap.put(ConcurrentHashMap.class, new ConcurrentHashMapHandler());
    handlerMap.put(Vector.class, new VectorHandler());
  }
  /**
   * The field "private final char{] value" in String.
   */
  private static Field STRING_VALUE_FIELD;
  static {
    try {
      STRING_VALUE_FIELD = String.class.getDeclaredField("value");
      STRING_VALUE_FIELD.setAccessible(true);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Get all declared non-transient and non-static fields of the given class.
   * @param clazz the class object from which to extract the fields.
   * @return an array of {@link FieldDescriptor} objects.
   * @throws Exception if any error occurs.
   */
  public static FieldDescriptor[] getPersistentDeclaredFields(final Class<?> clazz) throws Exception {
    FieldDescriptor[] result = FIELDS_MAP.get(clazz);
    if (result == null) {
      final Field[] allFields = clazz.getDeclaredFields();
      if (allFields.length <= 0) result = NO_FIELDS;
      else {
        int count = 0;
        final int mods = TRANSIENT_EXCEPTION_CLASSES.contains(clazz) ? Modifier.STATIC : NON_PERSISTENT_MODIFIERS;
        final Field[] fields = new Field[allFields.length];
        for (final Field f : allFields) {
          if ((f.getModifiers() & mods) == 0) fields[count++] = f;
        }
        if (count == 0) result = NO_FIELDS;
        else {
          Field[] tmp = null;
          if (count == allFields.length) tmp = allFields;
          else {
            tmp = new Field[count];
            System.arraycopy(fields, 0, tmp, 0, count);
          }
          Arrays.sort(tmp, new Comparator<Field>() {
            @Override
            public int compare(final Field o1, final Field o2) {
              return o1.getName().compareTo(o2.getName());
            }
          });
          result = new FieldDescriptor[count];
          for (int i=0; i<count; i++) {
            final Field f = tmp[i];
            f.setAccessible(true);
            result[i] = new FieldDescriptor(f);
          }
        }
      }
      FIELDS_MAP.put(clazz, result);
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
    String sig = SIGNATURE_MAP.get(clazz);
    if (sig != null) return sig;
    final StringBuilder sb = new StringBuilder(32);
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
    sig = sb.toString();
    SIGNATURE_MAP.put(clazz, sig);
    return sig;
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
    while (signature.charAt(pos) == '[') pos++;
    final Class<?> componentType = getNonArrayTypeFromSignature(signature.substring(pos), cl);
    final int[] dimensions = new int[pos];
    Arrays.fill(dimensions, 0);
    final Class<?> c = Array.newInstance(componentType, dimensions).getClass();
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
      case 'B': return Byte.TYPE;
      case 'S': return Short.TYPE;
      case 'I': return Integer.TYPE;
      case 'J': return Long.TYPE;
      case 'F': return Float.TYPE;
      case 'D': return Double.TYPE;
      case 'C': return Character.TYPE;
      case 'Z': return Boolean.TYPE;
      case 'L':
        final String s = signature.substring(1);
        return "void".equals(s) ? Void.TYPE : cl.loadClass(s);
    }
    throw new JPPFException("Could not load type with signature '" + signature + '\'');
  }

  /**
   * Get the {@code readObject()} or {@code writeObject()} method with the signature specified in {@link Serializable}.
   * @param clazz the class for which to get the method.
   * @param isRead whether we are looking for a {@code readObject()} or {@code writeObject()} method.
   * @return the desired method, or {@code null} if the mthod could not be found.
   * @throws Exception if any error occurs.
   */
  static Method getReadOrWriteObjectMethod(final Class<?> clazz, final boolean isRead) throws Exception {
    final Map<Class<?>, Object> map = isRead ? READ_OBJECT_MAP : WRITE_OBJECT_MAP;
    Object method = map.get(clazz);
    if (method == NO_MEMBER) return null;
    else if (method != null) return (Method) method;
    final String methodName = isRead ? "readObject" : "writeObject";
    final Class<?> paramType = isRead ? ObjectInputStream.class : ObjectOutputStream.class;
    // iterating over getDeclaredMethods() is much faster than calling getDeclaredMethod(String, Class<?>...)
    for (final Method m: clazz.getDeclaredMethods()) {
      if (methodName.equals(m.getName())) {
        final Class<?>[] paramTypes = m.getParameterTypes();
        if ((paramTypes.length == 1) && (paramTypes[0] == paramType)) {
          final int n = m.getModifiers();
          if (!Modifier.isStatic(n) && Modifier.isPrivate(n)) {
            method = m;
            break;
          }
        }
      }
    }
    map.put(clazz, method == null ? NO_MEMBER : method);
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
      rf = initializeRF();
      if (rf != null) rfMethod = initializeRFMethod();
    } catch (@SuppressWarnings("unused") final Throwable t) {
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
      final Method m = rfClass.getDeclaredMethod("getReflectionFactory");
      return m.invoke(null);
    } catch (@SuppressWarnings("unused") final Throwable t) {
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
      final Method m = rfClass.getDeclaredMethod("newConstructorForSerialization", Class.class, Constructor.class);
      return m;
    } catch (@SuppressWarnings("unused") final Throwable t) {
    }
    return null;
  }

  /**
   * A cache of constructors used for deserialization.
   */
  private static final Map<Class<?>, Constructor<?>> CONSTRUCTOR_MAP = new ConcurrentSoftReferenceValuesMap<>();

  /**
   * Create an object without calling any of its class constructors if the JVM supports it,
   * or failing that, attempt to call the constructor with the smallest possible number of arguments.
   * @param clazz the object's class.
   * @return the newly created object.
   * @throws Exception if any error occurs.
   */
  public static Object create(final Class<?> clazz) throws Exception {
    return (rfMethod == null) ? createFromConstructor(clazz) : create(clazz, Object.class);
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
      Constructor<?> constructor = CONSTRUCTOR_MAP.get(clazz);
      if (constructor == null) {
        //==> ReflectionFactory rf = ReflectionFactory.getReflectionFactory();
        final Constructor<?> superConstructor = parent.getDeclaredConstructor();
        //==> constructor = rf.newConstructorForSerialization(clazz, superConstructor);
        constructor = (Constructor<?>) rfMethod.invoke(rf, clazz, superConstructor);
        CONSTRUCTOR_MAP.put(clazz, constructor);
      }
      return clazz.cast(constructor.newInstance());
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new IllegalStateException("Cannot create object", e);
    }
  }

  /**
   * A cache of constructors used for deserialization.
   */
  //private static final Map<Class<?>, ConstructorWithParameters> DEFAULT_CONSTRUCTOR_MAP = new ConcurrentHashMap<>();
  private static final Map<Class<?>, ConstructorWithParameters> DEFAULT_CONSTRUCTOR_MAP = new ConcurrentSoftReferenceValuesMap<>();

  /**
   * Instantiate an object from one of its class' existing constructor.
   * The constructors of the class are looked up, until we can find one whose invocation with default parameter values doesn't fail.
   * @param clazz the class for the object to create.
   * @return an instance of the specified class.
   * @throws Exception if any error occurs.
   */
  static Object createFromConstructor(final Class<?> clazz) throws Exception {
    ConstructorWithParameters cwp = DEFAULT_CONSTRUCTOR_MAP.get(clazz);
    if (cwp == null) {
      final Constructor<?>[] constructors = clazz.getDeclaredConstructors();
      Arrays.sort(constructors, new ConstructorComparator());
      for (final Constructor<?> c : constructors) {
        final Class<?>[] paramTypes = c.getParameterTypes();
        final Object[] params = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) params[i] = defaultValue(paramTypes[i]);
        if (!c.isAccessible()) c.setAccessible(true);
        try {
          final Object result = c.newInstance(params);
          cwp = new ConstructorWithParameters(c, params);
          DEFAULT_CONSTRUCTOR_MAP.put(clazz, cwp);
          return result;
        } catch (final Throwable t) {
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
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(final Constructor<?> c1, final Constructor<?> c2) {
      final int n1 = c1.getParameterTypes().length;
      final int n2 = c2.getParameterTypes().length;
      return n1 < n2 ? -1 : (n1 > n2 ? 1 : 0);
    }
  }

  /**
   * A class associating a constructor with a set of default values for its parameters.
   */
  private static class ConstructorWithParameters extends Pair<Constructor<?>, Object[]> {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

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
    else if ((c == Boolean.TYPE) || (c == Boolean.class)) return Boolean.FALSE;
    return null;
  }

  /**
   * Create a string from the specified array of characters.
   * @param chars the string's value.
   * @return a new string with the specified characters.
   * @throws Exception if any error occurs.
   */
  public static String createString(final char[] chars) throws Exception {
    final String s = new String();
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

  /**
   *
   * @return .
   */
  private static Set<Class<?>> initTransientExceptionClasses() {
    final Set<Class<?>> result = new HashSet<>();
    try {
      result.add(ConcurrentHashMap.class);
    } catch (final Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  /**
   * Get the serialization handler defined for the specified class, if any.
   * @param clazz the class to lookup.
   * @return a {@link SerializationHandler} instance, or null if none is defined for the class.
   */
  static SerializationHandler getSerializationHandler(final Class<?> clazz) {
    return handlerMap.get(clazz);
  }
}
