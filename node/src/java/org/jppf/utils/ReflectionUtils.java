/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
package org.jppf.utils;

import java.lang.reflect.*;
import java.util.*;

/**
 * Collection of static utility methods for dealing with reflection-based APIs.
 * @author Laurent Cohen
 */
public class ReflectionUtils
{
  /**
   * Generates a string that displays all return values for all getters of an object.
   * This method uses a new line character a field separator.
   * @param o the object whose getter return values are to be dumped into a string.
   * @param names the names of the attributes to dump. If null or empty, then all attributes that have a getter, except "class", are dumped.
   * This parameter is used as a filter on the attributes to dump and defines the order in which the fields are dumped.
   * @return a string with the classname, hashcode, and the value of
   * each attribute that has a corresponding getter.
   */
  public static String dumpObject(final Object o, final String...names)
  {
    return dumpObject(o, ", ", true, false, names);
  }

  /**
   * Generates a string that displays all return values for all getters of an object.
   * @param o the object whose getter return values are to be dumped into a string.
   * @param separator separator between field values, like a comma or a new line.
   * @param displaySimpleClassName <code>true</code> to use the object's class simple name, <code>false</code> for the fully qualified name.
   * @param displayHashCode whether to append '@hashcode_value' to the name of the class.
   * @param names the names of the attributes to dump. If null or empty, then all attributes that have a getter, except "class", are dumped.
   * This parameter is used as a filter on the attributes to dump and defines the order in which the fields are dumped.
   * @return a string with the classname, hashcode, and the value of  each attribute that has a corresponding getter.
   */
  public static String dumpObject(final Object o, final String separator, final boolean displaySimpleClassName, final boolean displayHashCode, final String...names)
  {
    Set<String> fieldNames = new LinkedHashSet<String>();
    if (names != null) Collections.addAll(fieldNames, names);
    if (o == null) return "null";
    Class<?> clazz = o.getClass();
    StringBuilder sb = new StringBuilder();
    sb.append(displaySimpleClassName ? clazz.getSimpleName() : clazz.getName());
    if (displayHashCode) sb.append('@').append(Integer.toHexString(o.hashCode()));
    sb.append('[');
    Method[] methods = clazz.getMethods();
    // we want the attributes in ascending alphabetical order
    Map<String, Object> attrMap = fieldNames.isEmpty() ? new TreeMap<String, Object>() : new HashMap<String, Object>(names.length);
    for (Method method : methods)
    {
      if (isGetter(method) && !"getClass".equals(method.getName()))
      {
        String attrName = null;
        attrName = method.getName().substring(method.getName().startsWith("get") ? 3 : 2);
        attrName = attrName.substring(0, 1).toLowerCase() + attrName.substring(1);
        if (fieldNames.isEmpty() || fieldNames.contains(attrName))
        {
          Object value = null;
          try
          {
            value = method.invoke(o, (Object[]) null);
            if (value == null) value = "null";
          }
          catch (Exception e)
          {
            value = "*Error: " + ExceptionUtils.getMessage(e) + '*';
          }
          attrMap.put(attrName, value);
        }
      }
    }
    int count = 0;
    Set<String> set = fieldNames.isEmpty() ? attrMap.keySet() : fieldNames;
    for (String attr: set)
    {
      Object value = attrMap.get(attr);
      if (value != null)
      {
        if (count++ > 0) sb.append(separator);
        sb.append(attr).append('=').append(value);
      }
    }
    sb.append(']');
    return sb.toString();
  }

  /**
   * Determines whether a method is a getter (accessor), according to Sun's naming conventions.
   * @param meth the method to analyse.
   * @return true if the method is a getter, false otherwise.
   */
  public static boolean isGetter(final Method meth)
  {
    Class type = meth.getReturnType();
    if (Void.TYPE.equals(type)) return false;
    if (!StringUtils.startsWithOneOf(meth.getName(), false, "get", "is")) return false;
    if (meth.getName().startsWith("is"))
    {
      if (!Boolean.class.equals(type) && !Boolean.TYPE.equals(type)) return false;
    }
    Class[] paramTypes = meth.getParameterTypes();
    return !((paramTypes != null) && (paramTypes.length > 0));
  }

  /**
   * Determines whether a method is a setter (mutator), according to Sun's naming conventions.
   * @param meth the method to analyse.
   * @return true if the method is a setter, false otherwise.
   */
  public static boolean isSetter(final Method meth)
  {
    Class type = meth.getReturnType();
    if (!Void.TYPE.equals(type)) return false;
    if (!meth.getName().startsWith("set")) return false;
    Class[] paramTypes = meth.getParameterTypes();
    return !((paramTypes == null) || (paramTypes.length != 1));
  }

  /**
   * Get a getter with a given name from a class.
   * @param clazz the class enclosing the getter.
   * @param name the name of the getter to look for.
   * @return a <code>Method</code> object, or null if the class has no getter with the specified name.
   */
  public static Method getGetter(final Class clazz, final String name)
  {
    Method[] methods = clazz.getMethods();
    Method getter = null;
    for (Method method : methods) {
      if (isGetter(method) && name.equals(method.getName())) {
        getter = method;
        break;
      }
    }
    return getter;
  }

  /**
   * Get a setter with a given name from a class.
   * @param clazz the class enclosing the setter.
   * @param name the name of the setter to look for.
   * @return a <code>Method</code> object, or null if the class has no setter with the specified name.
   */
  public static Method getSetter(final Class clazz, final String name)
  {
    Method[] methods = clazz.getMethods();
    Method setter = null;
    for (Method method : methods) {
      if (isSetter(method) && name.equals(method.getName())) {
        setter = method;
        break;
      }
    }
    return setter;
  }

  /**
   * Get a getter corresponding to a specified instance variable name from a class.
   * @param clazz the class enclosing the instance variable.
   * @param attrName the name of the instance variable to look for.
   * @return a <code>Method</code> object, or null if the class has no getter for the specified
   * instance variable name.
   */
  public static Method getGetterForAttribute(final Class clazz, final String attrName)
  {
    String basename =
        attrName.substring(0, 1).toUpperCase() + attrName.substring(1);
    Method method = getGetter(clazz, "get"+basename);
    if (method == null) method = getGetter(clazz, "is"+basename);
    return method;
  }

  /**
   * Get a setter corresponding to a specified instance variable name from a class.
   * @param clazz the class enclosing the instance variable.
   * @param attrName the name of the instance variable to look for.
   * @return a <code>Method</code> object, or null if the class has no setter for the specified
   * instance variable name.
   */
  public static Method getSetterForAttribute(final Class clazz, final String attrName)
  {
    String basename =
        attrName.substring(0, 1).toUpperCase() + attrName.substring(1);
    return getSetter(clazz, "set"+basename);
  }

  /**
   * Obtain all the getters or setters of a specified class.
   * @param clazz the class to get the methods from.
   * @param getters if true, indicates that the getters should be looked up, otherwise it should be the setters.
   * @return an array of <code>Method</code> instances.
   */
  public static Method[] getAllBeanMethods(final Class clazz, final boolean getters)
  {
    List<Method> methodList = new ArrayList<Method>();
    Method[] allMethods = clazz.getMethods();
    for (Method meth: allMethods)
    {
      if ((getters && isGetter(meth)) || (!getters && isSetter(meth)))
      {
        methodList.add(meth);
      }
    }
    return methodList.toArray(new Method[methodList.size()]);
  }

  /**
   * Perform a deep copy of an object.
   * This method uses reflection to perform the copy through public accessors and mutators (getters and setters).
   * @param o the object to copy.
   * @return an object whose state is a copy of that of the input object.
   */
  public static Object deepCopy(final Object o)
  {
    return null;
  }

  /**
   * Get the method with the specified name, in the specified declaring class, that matches
   * the number, order and types of the specified arguments.
   * @param clazz the class in which to look for the method.
   * @param name the name of the method to look for.
   * @param args the arguments of the method.
   * @return a matching <code>Method</code> instance, or null if no match could be found.
   */
  public static Method getMatchingMethod(final Class clazz, final String name, final Object[] args)
  {
    Class[] argTypes = createTypeArray(args);
    Method[] methods = clazz.getDeclaredMethods();
    for (Method m: methods)
    {
      if (!m.getName().equals(name)) continue;
      if (matchingTypes(argTypes, m.getParameterTypes())) return m;
    }
    return null;
  }

  /**
   * Get the constructor with in the specified declaring class, that matches
   * the number, order and types of the specified arguments.
   * @param clazz the class in which to look for the constructor.
   * @param args the arguments of the method.
   * @return a matching <code>Constructor</code> instance, or null if no match could be found.
   */
  public static Constructor getMatchingConstructor(final Class clazz, final Object[] args)
  {
    Class[] argTypes = createTypeArray(args);
    Constructor[] constructors = clazz.getDeclaredConstructors();
    for (Constructor c: constructors)
    {
      if (matchingTypes(argTypes, c.getParameterTypes())) return c;
    }
    return null;
  }

  /**
   * Determine whether a set of (possibly null) types loosely matches another set of types.
   * @param argTypes the types to match, null values are considered wildcards (matching any type).
   * @param types the set of types to match.
   * @return true if the methods match, false otherwise.
   */
  public static boolean matchingTypes(final Class<?>[] argTypes, final Class<?>[] types)
  {
    if (argTypes.length != types.length) return false;
    for (int i=0; i<types.length; i++)
    {
      if (argTypes[i] != null)
      {
        Class<?> c = types[i].isPrimitive() ? mapPrimitveType(types[i]) :  types[i];
        if (!c.isAssignableFrom(argTypes[i])) return false;
      }
    }
    return true;
  }

  /**
   * Map a primitive type to its corresponding wrapper type.
   * @param type a primitive type.
   * @return a <code>Class</code> instance.
   */
  public static Class mapPrimitveType(final Class type)
  {
    if (Boolean.TYPE.equals(type)) return Boolean.class;
    else if (Character.TYPE.equals(type)) return Character.class;
    else if (Byte.TYPE.equals(type)) return Byte.class;
    else if (Short.TYPE.equals(type)) return Short.class;
    else if (Integer.TYPE.equals(type)) return Integer.class;
    else if (Long.TYPE.equals(type)) return Long.class;
    else if (Float.TYPE.equals(type)) return Float.class;
    else if (Double.TYPE.equals(type)) return Double.class;
    return type;
  }

  /**
   * Generate an array of the types of the specified arguments.
   * If the array of arguments is null this method will return an empty array.
   * @param args the arguments to get the types from.
   * @return an array of <code>Class</code> instances.
   */
  public static Class[] createTypeArray(final Object[] args)
  {
    if ((args == null) || (args.length == 0)) return new Class[0];
    Class[] argTypes = new Class[args.length];
    for (int i=0; i<args.length; i++)
    {
      argTypes[i] = (args[i] != null) ? args[i].getClass() : null;
    }
    return argTypes;
  }

  /**
   * Get the name of the method that called this one.
   * @return the name of the invoking method as a string.
   */
  public static String getCurrentMethodName()
  {
    Exception e = new Exception();
    StackTraceElement[] elts = e.getStackTrace();
    if (elts.length < 2) return "method name not found";
    return elts[1].getMethodName();
  }
}