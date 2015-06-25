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
package org.jppf.classloader;

import java.beans.Introspector;
import java.lang.ref.Reference;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Implementation for class loader leak prevention.
 * @author Martin JANDA
 * @exclude
 */
public final class JPPFLeakPrevention {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JPPFLeakPrevention.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Name of the system thread group.
   */
  private static final String THREAD_GROUP_SYSTEM = "system";
  /**
   * Name of the RMI runtime thread group.
   */
  private static final String THREAD_GROUP_RMI_RUNTIME = "RMI Runtime";
  /**
   * Flag that enables preventing JDBC Driver, Introspector and ResourceBundle leaks.
   */
  private final boolean preventJVM;
  /**
   * Flag that enables prevents leaking thread by stopping it and shutting down thread pool executor service when necessary.
   */
  private final boolean preventThread;
  /**
   * Flag enables clearing leaked <code>TimerThreads</code> from scheduled TimerTasks.
   */
  private final boolean preventTimer;
  /**
   * Flag that enables clearing thread local and inheritable thread local values.
   */
  private final boolean preventThreadLocal;
  /**
   * Flag that enables preventing HTTP keep alive thread leaks.
   */
  private final boolean preventKeepAlive;
  /**
   * Flag that enables clearing static fields.
   */
  private final boolean preventStaticReferences;

  /**
   * Default constructor for leak prevention.
   * @param config The JPPF configuration properties.
   */
  public JPPFLeakPrevention(final TypedProperties config)
  {
    if (config == null) throw new IllegalArgumentException("config is null");

    this.preventJVM = config.getBoolean("jppf.classloader.clear.jvm", false);
    this.preventThread = config.getBoolean("jppf.classloader.clear.thread", false);
    this.preventTimer = config.getBoolean("jppf.classloader.clear.timer", false);
    this.preventThreadLocal = config.getBoolean("jppf.classloader.clear.thread.local", false);
    this.preventKeepAlive = config.getBoolean("jppf.classloader.clear.keep.alive", false);
    this.preventStaticReferences = config.getBoolean("jppf.classloader.clear.static", false);
  }

  /**
   * Clears all references as prevention for memory leaks.
   * @param classLoader a <code>ClassLoader</code> instance.
   */
  public void clearReferences(final ClassLoader classLoader)
  {
    if (classLoader == null) throw new IllegalArgumentException("classLoader is null");

    try {
      if (preventJVM) clearJDBCDrivers(classLoader);
    } catch (Throwable t) {
      if (debugEnabled) log.debug(t.getMessage(), t);
      else log.warn(ExceptionUtils.getMessage(t));
    }

    try {
      if (preventKeepAlive || preventTimer || preventThread) clearThreads(classLoader);
    } catch (Throwable t) {
      if (debugEnabled) log.debug(t.getMessage(), t);
      else log.warn(ExceptionUtils.getMessage(t));
    }

    try {
      if (preventThreadLocal) clearThreadLocal(classLoader);
    } catch (Throwable t) {
      if (debugEnabled) log.debug(t.getMessage(), t);
      else log.warn(ExceptionUtils.getMessage(t));
    }

    try {
      if (preventStaticReferences) clearStaticFields(classLoader);
    } catch (Throwable t) {
      if (debugEnabled) log.debug(t.getMessage(), t);
      else log.warn(ExceptionUtils.getMessage(t));
    }

    try {
      if (preventJVM) ResourceBundle.clearCache(classLoader);
    } catch (Throwable t) {
      if (debugEnabled) log.debug(t.getMessage(), t);
      else log.warn(ExceptionUtils.getMessage(t));
    }

    try {
      if (preventJVM) Introspector.flushCaches();
    } catch (Throwable t) {
      if (debugEnabled) log.debug(t.getMessage(), t);
      else log.warn(ExceptionUtils.getMessage(t));
    }
  }

  /**
   * Deregister all JDBC drivers.
   * @param classLoader a <code>ClassLoader</code> instance.
   */
  private static void clearJDBCDrivers(final ClassLoader classLoader)
  {
    if (classLoader == null) throw new IllegalArgumentException("classLoader is null");

    Enumeration<Driver> drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements())
    {
      Driver driver = drivers.nextElement();
      if (driver.getClass().getClassLoader() == classLoader)
      {
        try {
          DriverManager.deregisterDriver(driver);
        } catch (SQLException e)
        {
          if (debugEnabled) log.debug(e.getMessage(), e);
          else log.warn(ExceptionUtils.getMessage(e));
        }
      }
    }
  }

  /**
   * Clear references help by threads. That can be HTTP keep alive thread, timer thread and thread pool worker threads.
   * @param classLoader a <code>ClassLoader</code> instance.
   */
  @SuppressWarnings("deprecation")
  private void clearThreads(final ClassLoader classLoader)
  {
    if (classLoader == null) throw new IllegalArgumentException("classLoader is null");

    for (Thread thread : getThreads()) {
      if (thread != null) {
        ClassLoader ccl = thread.getContextClassLoader();
        if (ccl == classLoader) {
          if (thread == Thread.currentThread()) continue;
          ThreadGroup threadGroup = thread.getThreadGroup();
          if (threadGroup != null && (THREAD_GROUP_SYSTEM.equals(threadGroup.getName()) || THREAD_GROUP_RMI_RUNTIME.equals(threadGroup.getName()))) {
            if (preventKeepAlive && "Keep-Alive-Timer".equals(thread.getName())) {
              thread.setContextClassLoader(classLoader.getParent());
              log.warn("HTTP keep alive timer cleared");
            }
            continue;
          }
          if (!thread.isAlive()) continue;

          if (preventTimer && "java.util.TimerThread".equals(thread.getClass().getName())) {
            clearTimerThread(thread);
          }  else if (preventThread) {
            try {
              Class clazz = thread.getClass();
              while(!Thread.class.equals(clazz)) clazz = clazz.getSuperclass();
              Field fieldTarget = getDeclaredAccessibleField(clazz, "target");
              Object target = fieldTarget.get(thread);

              if (target != null && "java.util.concurrent.ThreadPoolExecutor.Worker".equals(target.getClass().getCanonicalName())) {
                Field fieldThis = getDeclaredAccessibleField(target.getClass(), "this$0");
                Object executor = fieldThis.get(target);
                if (executor instanceof ThreadPoolExecutor) ((ThreadPoolExecutor) executor).shutdownNow();
              }
            } catch (Exception e) {
              if (debugEnabled) log.debug(e.getMessage(), e);
              else log.warn(ExceptionUtils.getMessage(e));
            }
            thread.stop();
          }
        }
      }
    }
  }

  /**
   * Disable accepting new timer task and clear timer queue.
   * @param thread instance of <code>TimerThread</code> to be cleared.
   */
  private static void clearTimerThread(final Thread thread) {
    try {
      Field fieldNewTasks = getDeclaredAccessibleField(thread.getClass(), "newTasksMayBeScheduled");
      Field fieldQueue = getDeclaredAccessibleField(thread.getClass(), "queue");

      Object queue = fieldQueue.get(thread);
      Method methodClear = getDeclaredAccessibleMethod(queue.getClass(), "clear");

      synchronized (queue)
      {
        fieldNewTasks.setBoolean(thread, false);
        methodClear.invoke(queue);
        queue.notify();
      }

      log.warn("Timer thread " + thread.getName() + " leaked.");
    } catch (Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
    }
  }

  /**
   * Clear thread local and inheritable thread local maps in all threads.
   * @param classLoader a <code>ClassLoader</code> instance.
   */
  private void clearThreadLocal(final ClassLoader classLoader) {
    if (classLoader == null) throw new IllegalArgumentException("classLoader is null");

    try {
      Field fieldThreadLocals = getDeclaredAccessibleField(Thread.class, "threadLocals");
      Field fieldInheritableThreadLocals = getDeclaredAccessibleField(Thread.class, "inheritableThreadLocals");
      Field fieldTable = getDeclaredAccessibleField(Class.forName("java.lang.ThreadLocal$ThreadLocalMap"), "table");

      for (Thread thread : getThreads()) {
        if (thread != null) {
          clearThreadLocalMap(classLoader, fieldThreadLocals.get(thread), fieldTable); // clear thread locals
          clearThreadLocalMap(classLoader, fieldInheritableThreadLocals.get(thread), fieldTable); // clear inheritable thread locals
        }
      }
    } catch (Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
    }
  }

  /**
   * Clear thread locals for one thread.
   * @param classLoader a <code>ClassLoader</code> instance.
   * @param map holding <code>ThreadLocal</code> instances.
   * @param fieldTable table in ThreadLocal map that is holding <code>ThreadLocal</code> instances.
   * @throws Exception when error occurs.
   */
  private void clearThreadLocalMap(final ClassLoader classLoader, final Object map, final Field fieldTable) throws Exception {
    if (map == null) return;

    Method methodRemove = map.getClass().getDeclaredMethod("remove", ThreadLocal.class);
    methodRemove.setAccessible(true);
    Object[] table = (Object[]) fieldTable.get(map);
    if (table != null)
    {
      StringBuilder sb = new StringBuilder();
      boolean hasStaleEntries = false;
      for (Object item : table)
      {
        if (item == null) continue;

        Object key = ((Reference<?>) item).get();
        boolean remove = isLoadedByClassLoader(classLoader, key);

        Field fieldValue = getDeclaredAccessibleField(item.getClass(), "value");
        Object value = fieldValue.get(item);
        remove |= isLoadedByClassLoader(classLoader, value);

        if (remove)
        {
          sb.setLength(0); // reuse preallocated StringBuilder on next run
          sb.append(classLoader.toString());
          sb.append(", ");
          if (key != null)
          {
            sb.append(key.getClass().getName());
            sb.append(", ");
            try {
              sb.append(key);
            } catch (Exception e)
            {
              if (debugEnabled) log.debug("Clear thread local: key=" + sb + " - " + e.getMessage(), e);
              else log.warn("Clear thread local: key=" + sb + " - " + ExceptionUtils.getMessage(e));
              sb.append("???");
            }
          } else sb.append("???, <null>");

          sb.append(", ");
          if (value != null)
          {
            sb.append(value.getClass().getName());
            sb.append(", ");
            try {
              sb.append(value);
            } catch (Exception e)
            {
              if (debugEnabled) log.debug("Clear thread local: value=" + sb + " - " + e.getMessage(), e);
              else log.warn("Clear thread local: value=" + sb + " - " + ExceptionUtils.getMessage(e));
              sb.append("???");
            }
          } else sb.append("???, <null>");

          if (debugEnabled) log.debug("Clear thread local: " + sb);
          if (preventThreadLocal)
          {
            if (key == null) hasStaleEntries = true;
            else methodRemove.invoke(map, key);
          }
        }
      }
      if (hasStaleEntries)
      {
        Method methodExpungeStaleEntries = getDeclaredAccessibleMethod(map.getClass(), "expungeStaleEntries");
        methodExpungeStaleEntries.invoke(map);
      }
    }
  }

  /**
   * Clear references held by static fields.
   * @param classLoader a <code>ClassLoader</code> instance.
   */
  private static void clearStaticFields(final ClassLoader classLoader)
  {
    if (classLoader == null) throw new IllegalArgumentException("classLoader is null");

    // we need to initialize all loaded classes
    for (Class clazz : getLoadedClasses(classLoader))
    {
      try {
        for (Field field : clazz.getDeclaredFields()) {
          if (Modifier.isStatic(field.getModifiers())) {
            field.get(null);
            break;
          }
        }
      } catch (Throwable ignore) {
      }
    }

    for (Class clazz : getLoadedClasses(classLoader)) {
      try {
        for (Field field : clazz.getDeclaredFields()) {
          if (field.getType().isPrimitive() || field.getName().contains("$")) continue;

          int mods = field.getModifiers();
          if (Modifier.isStatic(mods)) {
            try {
              field.setAccessible(true);
              if (Modifier.isFinal(mods)) {
                if (!(field.getType().getName().startsWith("javax.") || field.getType().getName().startsWith("java."))) {
                  nullInstance(classLoader, field.get(null));
                }
              } else {
                field.set(null, null);
                if (debugEnabled) log.debug("Set " + clazz.getName() + '.' + field.getName() + " to null");
              }
            } catch (Throwable t) {
              if (debugEnabled) log.debug(t.getMessage(), t);
              else log.warn(ExceptionUtils.getMessage(t));
              if (debugEnabled) log.debug("Could not set " + clazz.getName() + '.' + field.getName() + " to null", t);
            }
          }
        }
      } catch (Throwable t) {
        if (debugEnabled) log.debug(t.getMessage(), t);
        else log.warn(ExceptionUtils.getMessage(t));
        if (debugEnabled) log.debug("Could not clean fields for class " + clazz.getName(), t);
      }
    }
  }

  /**
   * Set instance static final fields to <code>null</code>.
   * @param classLoader a <code>ClassLoader</code> instance.
   * @param instance and object instance to null it's static fields.
   */
  private static void nullInstance(final ClassLoader classLoader, final Object instance) {
    if (classLoader == null) throw new IllegalArgumentException("classLoader is null");
    if (instance == null) return;

    for (Field field : instance.getClass().getDeclaredFields()) {
      if (field.getType().isPrimitive() || field.getName().contains("$")) continue;

      try {
        int mods = field.getModifiers();
        if (!Modifier.isStatic(mods) || !Modifier.isFinal(mods)) {
          field.setAccessible(true);
          Object value = field.get(instance);
          if (value != null && isLoadedByClassLoader(classLoader, value.getClass())) {
            field.set(instance, null);
            if (debugEnabled) log.debug("Set " + instance.getClass().getName() + '.' + field.getName() + " to null");
          }
        }
      } catch (Throwable t) {
        if (debugEnabled) log.debug("Could not set " + instance.getClass().getName() + '.' + field.getName() + " to null", t);
      }
    }
  }

  /**
   * Get array of all threads in this VM.
   * @return array of all threads. May contain <code>null</code> values.
   */
  private static Thread[] getThreads()
  {
    ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();

    while (threadGroup.getParent() != null)
    {
      threadGroup = threadGroup.getParent();
    }

    // activeCount is an estimate only, so we play it safe and take 2x its value.
    Thread[] threads = new Thread[2 * threadGroup.activeCount()];
    threadGroup.enumerate(threads);
    return threads;
  }

  /**
   * Get accessible field declared in class.
   * @param clazz a <code>Class</code> instance that should declare the field.
   * @param name the field name to find.
   * @return a <code>Field</code> instance with accessible flag set to <code>true</code>.
   * @throws NoSuchFieldException if field doesn't exists.
   */
  private static Field getDeclaredAccessibleField(final Class clazz, final String name) throws NoSuchFieldException {
    if (clazz == null) throw new IllegalArgumentException("clazz is null");
    if (name == null || name.isEmpty()) throw new IllegalArgumentException("name is blank");

    Field member = clazz.getDeclaredField(name);
    member.setAccessible(true);
    return member;
  }

  /**
   * Get accessible method declared in class.
   * @param clazz a <code>Class</code> instance that should declare the method.
   * @param name the method name to find.
   * @return a <code>Method</code> instance with accessible flag set to <code>true</code>.
   * @throws NoSuchMethodException if method doesn't exists.
   */
  private static Method getDeclaredAccessibleMethod(final Class<?> clazz, final String name) throws NoSuchMethodException {
    if (clazz == null) throw new IllegalArgumentException("clazz is null");
    if (name == null || name.isEmpty()) throw new IllegalArgumentException("name is blank");

    Method member = clazz.getDeclaredMethod(name);
    member.setAccessible(true);
    return member;
  }
  /**
   * Check whether object was loaded by this or child loader.
   * @param classLoader a <code>ClassLoader</code> instance.
   * @param o object to test.
   * @return true if object is loaded by this or child class loader.
   */
  private static boolean isLoadedByClassLoader(final ClassLoader classLoader, final Object o)
  {
    return o != null && (classLoader.equals(o) || isLoadedByClassLoader(classLoader, o.getClass()));
  }

  /**
   * Check whether class was loaded by this or child loader.
   * @param classLoader a <code>ClassLoader</code> instance.
   * @param clazz class to test.
   * @return true if class is loaded by this or child class loader.
   */
  private static boolean isLoadedByClassLoader(final ClassLoader classLoader, final Class clazz)
  {
    if (classLoader == null) throw new IllegalArgumentException("classLoader is null");
    if (clazz == null) throw new IllegalArgumentException("clazz is null");

    ClassLoader loader = clazz.getClassLoader();
    while(loader != null)
    {
      if (classLoader == loader) return true;
      loader = loader.getParent();
    }
    return false;
  }

  /**
   * Get collection of loaded classes by using reflection.
   * @param classLoader a <code>ClassLoader</code> instance.
   * @return collection of loaded classes by class loader.
   */
  @SuppressWarnings("unchecked")
  private static Collection<Class> getLoadedClasses(final ClassLoader classLoader)
  {
    if (classLoader == null) throw new IllegalArgumentException("classLoader is null");

    Class<?> cls = classLoader.getClass();
    while(cls != null && !ClassLoader.class.equals(cls)) cls = cls.getSuperclass();
    try {
      if (cls != null) {
        Field field = getDeclaredAccessibleField(cls, "classes");
        return Collections.unmodifiableCollection(((Collection<Class>) field.get(classLoader)));
      }
    } catch (Throwable t) {
      if (debugEnabled) log.debug(t.getMessage(), t);
      else log.warn(ExceptionUtils.getMessage(t));
    }

    return Collections.emptyList();
  }
}
