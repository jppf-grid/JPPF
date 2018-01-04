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

package org.jppf.utils;

import java.lang.reflect.*;
import java.util.Arrays;

import org.jppf.JPPFException;
import org.jppf.utils.configuration.JPPFProperty;
import org.slf4j.*;

/**
 * Collection of utility methods to facilitate the use of reflection.
 * @author Laurent Cohen
 * @exclude
 */
public final class ReflectionHelper {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ReflectionHelper.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Invoke a method using reflection.
   * @param clazz the class on which to invoke the method.
   * @param instance the object on which to invoke the method, may be null if the method is static.
   * @param methodName the name of the method to invoke.
   * @param paramTypes the types of the method's parameters, may be null if no parameters.
   * @param values the values of the method's parameters, may be null if no parameters.
   * @return the result of the method's invocation, or null if the method's return type is void,
   * or a <code>JPPFException</code> if the invocation failed.
   */
  public static Object invokeMethod(final Class<?> clazz, final Object instance, final String methodName, final Class<?>[] paramTypes, final Object...values) {
    try {
      final Method m = clazz.getMethod(methodName, paramTypes);
      return m.invoke(instance, values);
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
      return new JPPFException(e);
    }
  }

  /**
   * Invoke a method using reflection, without having to specify the parameters types.
   * In this case, we assume the first method found with the specified name, and whose
   * number of parameter is the same as the number of values, is the one we use.
   * @param clazz the class on which to invoke the method.
   * @param instance the object on which to invoke the method, may be null if the method is static.
   * @param methodName the name of the method to invoke.
   * @param values the values of the method's parameters, may be null if no parameters.
   * @return the result of the method's invocation, or null if the method's return type is void,
   * or a <code>JPPFException</code> if the invocation failed.
   */
  public static Object invokeMethod(final Class<?> clazz, final Object instance, final String methodName, final Object...values) {
    try {
      final int nbArgs = (values == null) ? 0 : values.length;
      final Method m = findMethod(clazz, methodName, nbArgs);
      return m.invoke(instance, values);
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
      return new JPPFException(e);
    }
  }

  /**
   * Convenience method to invoke a method with no parameter.
   * @param clazz the class on which to invoke the method.
   * @param instance the object on which to invoke the method, may be null if the method is static.
   * @param methodName the name of the method to invoke.
   * @return the result of the method's invocation, or null if the method's return type is void,
   * or a <code>JPPFException</code> if the invocation failed.
   */
  public static Object invokeMethod(final Class<?> clazz, final Object instance, final String methodName) {
    return invokeMethod(clazz, instance, methodName, null, (Object[]) null);
  }

  /**
   * Find a method without having to specify the parameters types.
   * In this case, we assume the first method found with the specified name, and whose
   * number of parameter is the same as the number of values, is the one we use.
   * @param clazz the class on which to invoke the method.
   * @param methodName the name of the method to find.
   * @param nbArgs the of parameters in the method, may be zero if no parameters.
   * @return the method with the specified name and number of parameters.
   * @throws Exception if any erorr occurs.
   */
  public static Method findMethod(final Class<?> clazz, final String methodName, final int nbArgs) throws Exception {
    final Method[] methods = clazz.getMethods();
    for (Method m: methods) {
      if (m.getName().equals(methodName) && (m.getParameterTypes().length == nbArgs)) return m;
    }
    throw new NoSuchMethodException("class : " + clazz.getName() + ", method: " + methodName);
  }

  /**
   * Find a method without having to specify the parameters types.
   * In this case, we assume the first method found with the specified name and zero arugments is the one we use.
   * @param clazz the class on which to invoke the method.
   * @param methodName the name of the method to find.
   * @return the method with the specified name and no parameters.
   * @throws Exception if any erorr occurs.
   */
  public static Method findMethod(final Class<?> clazz, final String methodName) throws Exception {
    return findMethod(clazz, methodName, 0);
  }

  /**
   * Find a method without having to specify the parameters types.
   * In this case, we assume the first method found with the specified name is the one we use.
   * @param clazz the class on which to invoke the method.
   * @param methodName the name of the method to find.
   * @return the method with the specified name and number of parameters.
   * @throws Exception if any erorr occurs.
   */
  public static Method findMethodAnyArgs(final Class<?> clazz, final String methodName) throws Exception {
    final Method[] methods = clazz.getMethods();
    for (Method m: methods) {
      if (m.getName().equals(methodName)) return m;
    }
    throw new NoSuchMethodException("class : " + clazz.getName() + ", method: " + methodName);
  }

  /**
   * Invoke a default constructor using reflection.
   * @param className the name of the class to instantiate.
   * @return an instance of the class whose name is specified, or a <code>JPPFException</code> if the instantiation failed.
   */
  public static Object newInstance(final String className) {
    try {
      final Class<?> c = getCurrentClassLoader().loadClass(className);
      return c.newInstance();
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
      return new JPPFException(e);
    }
  }

  /**
   * Invoke a default constructor using reflection.
   * @param clazz the class to instantiate.
   * @return an instance of the class whose name is specified, or a <code>JPPFException</code> if the instantiation failed.
   */
  public static Object newInstance(final Class<?> clazz) {
    try {
      return clazz.newInstance();
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
      return new JPPFException(e);
    }
  }

  /**
   * Invoke a constructor using reflection.
   * @param clazz the class on which to invoke the constructor.
   * @param paramTypes the types of the constructor's parameters, may be null if no parameters.
   * @param values the values of the constructor's parameters, may be null if no parameters.
   * @return the result of the constructor's invocation, or a <code>JPPFException</code> if the invocation failed.
   */
  public static Object invokeConstructor(final Class<?> clazz, final Class<?>[] paramTypes, final Object...values) {
    try {
      final Constructor<?> c = clazz.getConstructor(paramTypes);
      return c.newInstance(values);
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
      return new JPPFException(e);
    }
  }

  /**
   * Get the value of the field of a specified class.
   * @param clazz the class declaring the field.
   * @param instance  the class instance for which to get the field's value, may be null if the field is static.
   * @param fieldName the name of the field to get the value  of.
   * @return the value of the field, or a <code>JPPFException</code> if the invocation failed.
   */
  public static Object getField(final Class<?> clazz, final Object instance, final String fieldName) {
    try {
      final Field f = clazz.getField(fieldName);
      return f.get(instance);
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
      return new JPPFException(e);
    }
  }

  /**
   * Get the value of the specified static field for a specified class.
   * @param className the class declaring the field.
   * @param fieldName the name of the field to get the value  of.
   * @return the value of the field, or a <code>JPPFException</code> if the invocation failed.
   */
  public static Object getField(final String className, final String fieldName) {
    return getField(getClass0(className), null, fieldName);
  }

  /**
   * Get the value of the specified field for a specified class and specified instance.
   * @param className the class declaring the field.
   * @param instance  the class instance for which to get the field's value, may be null if the field is static.
   * @param fieldName the name of the field to get the value  of.
   * @return the value of the field, or a <code>JPPFException</code> if the invocation failed.
   */
  public static Object getField(final String className, final Object instance, final String fieldName) {
    return getField(getClass0(className), instance, fieldName);
  }

  /**
   * Transform an array of class names into an array of <code>Class</code> objects.
   * @param classNames the names of the classes to find.
   * @return n array of <code>Class</code> objects, or null if one of the classes could not be found.
   */
  public static Class<?>[] getClasses(final String...classNames) {
    try {
      if ((classNames == null) || (classNames.length <= 0)) return new Class[0];
      final Class<?>[] classes = new Class[classNames.length];
      final ClassLoader cl = getCurrentClassLoader();
      for (int i=0; i<classNames.length; i++) classes[i] = Class.forName(classNames[i], true, cl);
      return classes;
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  /**
   * Transform a class name into a <code>Class</code> object.
   * @param className the name of the class to find.
   * @return a <code>Class</code>, or null if the classe could not be found.
   */
  public static Class<?> getClass0(final String className) {
    try {
      return Class.forName(className, true, getCurrentClassLoader());
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  /**
   * Returns the current thread's context class loader, or this class's class loader if it is null.
   * @return a <code>ClassLoader</code> instance.
   */
  public static ClassLoader getCurrentClassLoader() {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) cl = ReflectionHelper.class.getClassLoader();
    return cl;
  }

  /**
   *
   * @param clazz the class on which to find the method.
   * @param methodName the name of the method to find.
   * @param params the concrete parameters.
   * A best effort is made to try and match the specified parameters to the formal parameters of an existing method.
   * @return a method that matches the name and parameters.
   * @throws Exception if any erorr occurs.
   */
  public static Method findMethodFromConcreteArgs(final Class<?> clazz, final String methodName, final Object...params) throws Exception {
    final Method[] methods = clazz.getMethods();
    final Object[] p = params == null ? new Object[0] : params;
    for (Method m: methods) {
      final Class<?>[] formalParams = m.getParameterTypes();
      if (m.getName().equals(methodName) && (formalParams.length == p.length)) {
        boolean mismatch = false;
        for (int i=0; i<formalParams.length; i++) {
          if ((p[i] != null) && !formalParams[i].isAssignableFrom(p[i].getClass())) {
            mismatch = true;
            break;
          }
        }
        if (!mismatch) return m;
      }
    }
    return null;
  }

  /**
   * Find the specified public method in the specified class or one of its implemented interfaces or in its super class hierarchy.
   * @param clazz the class for which to find the method.
   * @param methodName the name of the method to find.
   * @param paramTypes the types of the parameters of the method, if any.
   * @return the matching method, or {@code null} or null if no method matches.
   */
  public static Method findMethod(final Class<?> clazz, final String methodName, final Class<?>...paramTypes) {
    try {
      final Method[] methods = clazz.getMethods();
      for (Method m: methods) {
        final Class<?>[] types = m.getParameterTypes();
        if (!methodName.equals(m.getName()) || (paramTypes.length != types.length)) continue;
        boolean found = true;
        for (int i=0; i<paramTypes.length; i++) {
          if (paramTypes[i] != types[i]) {
            found = false;
            break;
          }
        }
        if (found) return m;
      }
    } catch (@SuppressWarnings("unused") final Exception e) {
    }
    return null;
  }

  /**
   * Find the specified public constructor in the specified class.
   * @param clazz the class for which to find the constructor.
   * @param paramTypes the types of the parameters of the constructor, if any.
   * @return the matching method, or {@code null} or null if no method matches.
   */
  public static Constructor<?> findConstructor(final Class<?> clazz, final Class<?>...paramTypes) {
    try {
      final Constructor<?>[] constructors = clazz.getConstructors();
      for (Constructor<?> c: constructors) {
        final Class<?>[] types = c.getParameterTypes();
        if (paramTypes.length != types.length) continue;
        boolean found = true;
        for (int i=0; i<paramTypes.length; i++) {
          if (paramTypes[i] != types[i]) {
            found = false;
            break;
          }
        }
        if (found) return c;
      }
    } catch (@SuppressWarnings("unused") final Exception e) {
    }
    return null;
  }

  /**
   * Instantiate an object that implements the specified interface, whose class name is given by the specified property,
   * by calling either its default constructor or a oonstrcutor that take a String[].
   * @param inf the interface that must be implemented by the class.
   * @param prop the property that provides the class name and optional parameters for its String[] constructor,
   * along with an optional default implementation of the interface. 
   * @param <T> the type of the interface.
   * @return an instance of the specified interface.
   * @throws Exception if any error occurs.
   */
  public static <T> T invokeDefaultOrStringArrayConstructor(final Class<T> inf, final JPPFProperty<String[]> prop) {
    return invokeDefaultOrStringArrayConstructor(inf, prop.getName(), JPPFConfiguration.get(prop));
  }

  /**
   * Instantiate an object that implements the specified interface, whose class name is given by the specified property,
   * by calling either its default constructor or a oonstrcutor that take a String[].
   * @param inf the interface that must be implemented by the class.
   * @param prop the name of the property that provides the class name and optional parameters for its String[] constructor, along with an optional default implementation of the interface. 
   * @param value the value of the property.
   * @param <T> the type of the interface.
   * @return an instance of the specified interface.
   * @throws Exception if any error occurs.
   */
  @SuppressWarnings("unchecked")
  public static <T> T invokeDefaultOrStringArrayConstructor(final Class<T> inf, final String prop, final String[] value) {
    final String propName = (prop == null) ? "<unknown_property>" : prop;
    if (debugEnabled) log.debug("found {} = {}", propName, value == null ? "null" : Arrays.asList(value));
    if ((value == null) || (value.length <= 0)) return null;
    final String className = value[0];
    final String[] params = new String[value.length - 1];
    if (value.length > 1) {
      for (int i=1; i<value.length; i++) params[i - 1] = value[i];
    }
    Class<?> clazz = null;
    try {
      clazz = Class.forName(className);
    } catch (final ClassNotFoundException e) {
      log.error(String.format("The implementation of %s, class %s, was not found. Error is: %s", inf.getName(), className, ExceptionUtils.getStackTrace(e)));
      return null;
    }
    if (!inf.isAssignableFrom(clazz)) {
      log.error(String.format("The configured class '%s' for property '%s' does not implement or extend %s.", clazz.getName(), propName, inf.getName()));
      return null;
    }
    Object result = null;
    final Constructor<?> stringArrayConstructor = findConstructor(clazz, String[].class);
    if (stringArrayConstructor != null) {
      try {
        result = stringArrayConstructor.newInstance(new Object[] {params});
      } catch (final Exception e) {
        log.error(String.format("Error invoking %s(String[]) with params=%s for property '%s': %s", clazz.getName(), Arrays.asList(params), propName, ExceptionUtils.getStackTrace(e)));
        return null;
      }
    } else {
      final Constructor<?> defaultConstructor = findConstructor(clazz);
      if (defaultConstructor != null) {
        try {
          result = defaultConstructor.newInstance();
        } catch (final Exception e) {
          log.error(String.format("Error invoking default constructor of configured class '%s' for property '%s': %s", clazz.getName(), propName, ExceptionUtils.getStackTrace(e)));
          return null;
        }
        if (params.length > 0) {
          final Method setParamsMethod = findMethod(clazz, "setParameters", String[].class);
          if (setParamsMethod != null) {
            try {
              setParamsMethod.invoke(result, new Object[] {params});
            } catch (final Exception e) {
              log.error(String.format("Error invoking %s.setParameters(%s) for property '%s': %s",
                clazz.getName(), Arrays.asList(params), propName, ExceptionUtils.getStackTrace(e)));
              return null;
            }
          }
        }
      }
    }
    return (T) result;
  }
}
