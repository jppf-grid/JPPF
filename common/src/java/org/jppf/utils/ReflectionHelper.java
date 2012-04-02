/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import org.jppf.JPPFException;
import org.slf4j.*;

/**
 * Collection of utility methods to facilitate the use of reflection.
 * @author Laurent Cohen
 */
public final class ReflectionHelper
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ReflectionHelper.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private boolean debugEnabled = log.isDebugEnabled();

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
  public static Object invokeMethod(final Class<?> clazz, final Object instance, final String methodName, final Class<?>[] paramTypes, final Object...values)
  {
    try
    {
      Method m = clazz.getMethod(methodName, paramTypes);
      return m.invoke(instance, values);
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
      return new JPPFException(e);
    }
  }

  /**
   * Invoke a method using reflection, without having to specify the parameters types.
   * In this case, we assume the first method found with the specified name is the one we use.
   * @param clazz the class on which to invoke the method.
   * @param instance the object on which to invoke the method, may be null if the method is static.
   * @param methodName the name of the method to invoke.
   * @param values the values of the method's parameters, may be null if no parameters.
   * @return the result of the method's invocation, or null if the method's return type is void,
   * or a <code>JPPFException</code> if the invocation failed.
   */
  public static Object invokeMethod(final Class clazz, final Object instance, final String methodName, final Object...values)
  {
    try
    {
      Method[] methods = clazz.getMethods();
      for (Method m: methods)
      {
        if (m.getName().equals(methodName)) return m.invoke(instance, values);
      }
      throw new NoSuchMethodException("class : " + clazz + ", method: " + methodName);
    }
    catch(Exception e)
    {
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
  public static Object invokeMethod(final Class clazz, final Object instance, final String methodName)
  {
    return invokeMethod(clazz, instance, methodName, null, (Object[]) null);
  }

  /**
   * Invoke a default constructor using reflection.
   * @param className the name of the class to instantiate.
   * @return an instance of the class whose name is specified, or a <code>JPPFException</code> if the instantiation failed.
   */
  public static Object newInstance(final String className)
  {
    try
    {
      Class c = getCurrentClassLoader().loadClass(className);
      return c.newInstance();
    }
    catch(Exception e)
    {
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
  public static Object invokeConstructor(final Class<?> clazz, final Class<?>[] paramTypes, final Object...values)
  {
    try
    {
      Constructor<?> c = clazz.getConstructor(paramTypes);
      return c.newInstance(values);
    }
    catch(Exception e)
    {
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
  public static Object getField(final Class clazz, final Object instance, final String fieldName)
  {
    try
    {
      Field f = clazz.getField(fieldName);
      return f.get(instance);
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
      return new JPPFException(e);
    }
  }

  /**
   * Transform an array of class names into an array of <code>Class</code> objects.
   * @param classNames the names of the classes to find.
   * @return n array of <code>Class</code> objects, or null if one of the classes could not be found.
   */
  public static Class[] getClasses(final String...classNames)
  {
    try
    {
      if ((classNames == null) || (classNames.length <= 0)) return new Class[0];
      Class[] classes = new Class[classNames.length];
      //for (int i=0; i<classNames.length; i++) classes[i] = Class.forName(classNames[i]);
      ClassLoader cl = getCurrentClassLoader();
      for (int i=0; i<classNames.length; i++) classes[i] = cl.loadClass(classNames[i]);
      return classes;
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  /**
   * Transform a class name into a <code>Class</code> object.
   * @param className the name of the class to find.
   * @return n array of <code>Class</code> objects, or null if one of the classes could not be found.
   */
  public static Class getClass0(final String className)
  {
    try
    {
      return getCurrentClassLoader().loadClass(className);
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  /**
   * Returns the current thread's context class loader, or this class's class loader if it is null.
   * @return a <code>ClassLoader</code> instance.
   */
  public static ClassLoader getCurrentClassLoader()
  {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) cl = ReflectionHelper.class.getClassLoader();
    return cl;
  }
}
