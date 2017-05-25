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

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;


/**
 * This class provides a set of utility methods for manipulating {@link Throwable} objects.
 * @author Laurent Cohen
 */
public final class ExceptionUtils {
  /**
   * Instantiation of this class is not permitted.
   */
  private ExceptionUtils() {
  }

  /**
   * Get a throwable's stack trace.
   * @param t the throwable to get the stack trace from.
   * @return the stack trace as a string.
   */
  public static String getStackTrace(final Throwable t) {
    if (t == null) return "null";
    StringBuilder result = null;
    try (StringWriter writer = new StringWriter(); PrintWriter pw = new PrintWriter(writer)) {
      t.printStackTrace(pw);
      String s = writer.toString();
      result = new StringBuilder(s);
      int n = result.length();
      if (s.endsWith("\r\n")) result.setLength(n-2);
      else {
        char c = result.charAt(n-1);
        if ((c == '\n') || (c == '\r')) result.setLength(n-1);
      }
    } catch(@SuppressWarnings("unused") Exception e) {
      result = new StringBuilder(getStackTrace2(t));
    }
    return result.toString();
  }

  /**
   * Get a throwable's stack trace.
   * @param t the throwable to get the stack trace from.
   * @return the stack trace as a string.
   */
  private static String getStackTrace2(final Throwable t) {
    Throwable ct = t;
    Set<Throwable> set = new HashSet<>();
    StringBuilder sb = new StringBuilder();
    while (ct != null) {
      set.add(ct);
      sb.append(getMessage(ct));
      for (StackTraceElement elt: ct.getStackTrace()) sb.append("\n  at ").append(elt);
      ct = ct.getCause();
      if (set.contains(ct)) break;
      if (ct != null) sb.append("\nCaused by: ");
    }
    return sb.toString();
  }

  /**
   * Get the call stack for the current thread.
   * @return the call stack as a string.
   */
  public static String getCallStack() {
    Throwable t = new Throwable();
    StringBuilder sb = new StringBuilder();
    StackTraceElement[] st = t.getStackTrace();
    for (int i=1; i<st.length; i++) {
      if (i > 1) sb.append("\n");
      sb.append("  at ").append(st[i]);
    }
    return sb.toString();
  }

  /**
   * Get the message of the specified <code>Throwable</code> along with its class name.
   * @param t the <code>Throwable</code> object from which to get the message.
   * @return a formatted message from the <code>Throwable</code>.
   */
  public static String getMessage(final Throwable t) {
    return (t == null) ? "null" : t.getClass().getName() + ": " + t.getMessage();
  }

  /**
   * Converts a generic Throwable into an Exception.
   * If <code>throwable</code> is already an instance of {@link Exception}, it is returned as is.
   * @param throwable the Throwable to convert.
   * @return an (possibly new) {@link Exception} wrapping the {@link Throwable} as its cause.
   * @since 4.0
   */
  public static Exception toException(final Throwable throwable) {
    if (throwable instanceof Exception) return (Exception) throwable;
    return new Exception(throwable);
  }

  /**
   * Converts a generic Throwable into an {@link Exception}.
   * @param throwable the Throwable to convert.
   * @param message the message of the created exception.
   * @return a new {@link Exception} wrapping the {@link Throwable} as its cause.
   * @since 4.0
   */
  public static Exception toException(final String message, final Throwable throwable) {
    return new Exception(message, throwable);
  }

  /**
   * Converts a generic Throwable into a {@link RuntimeException}.
   * If <code>throwable</code> is already an instance of {@link RuntimeException}, it is returned as is.
   * @param throwable the Throwable to convert.
   * @return a (possibly new) {@link RuntimeException} wrapping the {@link Throwable} as its cause.
   * @since 4.0
   */
  public static RuntimeException toRuntimeException(final Throwable throwable) {
    if (throwable instanceof RuntimeException) return (RuntimeException) throwable;
    return new RuntimeException(throwable);
  }

  /**
   * Converts a generic Throwable into a {@link RuntimeException}.
   * @param throwable the Throwable to convert.
   * @param message the message of the xcreated exception.
   * @return a new {@link RuntimeException} wrapping the {@link Throwable} as its cause.
   * @since 4.0
   */
  public static RuntimeException toRuntimeException(final String message, final Throwable throwable) {
    return new RuntimeException(message, throwable);
  }

  /**
   * Converts a generic {@link Throwable} into an Exception of the specified class.
   * If <code>throwable</code> is already an instance of the specified exception class, it is returned as is.
   * @param <E> the type of exception to return.
   * @param throwable the Throwable to convert.
   * @param clazz the class of the exception to convert to.
   * @return a (possibly new) exception of the specified class wrapping the {@link Throwable} as its cause,
   * or <code>null</code> if an exception of this class cannot be constructed. 
   * @since 4.0
   */
  @SuppressWarnings("unchecked")
  public static <E extends Exception> E toException(final Throwable throwable, final Class<E> clazz) {
    if (clazz == null) return null;
    try {
      if ((throwable != null) && clazz.isAssignableFrom(throwable.getClass())) return (E) throwable;
      Constructor<E> constructor = clazz.getConstructor(Throwable.class);
      return constructor.newInstance(throwable);
    } catch (@SuppressWarnings("unused") Exception e) {
    }
    return null;
  }

  /**
   * Converts a generic {@link Throwable} into an Exception of the specified class.
   * @param <E> the type of exception to return.
   * @param message the message of the xcreated exception.
   * @param throwable the Throwable to convert.
   * @param clazz the class of the exception to convert to.
   * @return a new exception of the specified class wrapping the {@link Throwable} as its cause,
   * or <code>null</code> if an exception of this class cannot be constructed. 
   * @since 4.0
   */
  public static <E extends Exception> E toException(final String message, final Throwable throwable, final Class<E> clazz) {
    if (clazz == null) return null;
    try {
      Constructor<E> constructor = clazz.getConstructor(String.class, Throwable.class);
      return constructor.newInstance(message, throwable);
    } catch (@SuppressWarnings("unused") Exception e) {
    }
    return null;
  }
}
