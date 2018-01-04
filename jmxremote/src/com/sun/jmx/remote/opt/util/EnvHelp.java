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
/*
 * @(#)EnvHelp.java	1.5
 *
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * The contents of this file are subject to the terms of either the GNU General
 * Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL")(collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of the
 * License at http://opendmk.dev.java.net/legal_notices/licenses.txt or in the
 * LEGAL_NOTICES folder that accompanied this code. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file found at
 *     http://opendmk.dev.java.net/legal_notices/licenses.txt
 * or in the LEGAL_NOTICES folder that accompanied this code.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.
 *
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 *
 *       "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding
 *
 *       "[Contributor] elects to include this software in this distribution
 *        under the [CDDL or GPL Version 2] license."
 *
 * If you don't indicate a single choice of license, a recipient has the option
 * to distribute your version of this file under either the CDDL or the GPL
 * Version 2, or to extend the choice of license to its licensees as provided
 * above. However, if you add GPL Version 2 code and therefore, elected the
 * GPL Version 2 license, then the option applies only if the new code is made
 * subject to such option by the copyright holder.
 *
 */

package com.sun.jmx.remote.opt.util;

import java.io.*;
import java.security.AccessController;
import java.util.*;

import javax.management.*;
import javax.management.remote.*;

/**
 *
 */
public class EnvHelp {
  /**
   * Name of the attribute that specifies a default class loader object. The value associated with this attribute is a ClassLoader object
   */
  private static final String DEFAULT_CLASS_LOADER = JMXConnectorFactory.DEFAULT_CLASS_LOADER;
  /**
   * Name of the attribute that specifies a default class loader ObjectName. The value associated with this attribute is an ObjectName object
   */
  private static final String DEFAULT_CLASS_LOADER_NAME = JMXConnectorServerFactory.DEFAULT_CLASS_LOADER_NAME;
  /**
   *
   */
  private static final ClassLogger logger = new ClassLogger("javax.management.remote.misc", "EnvHelp");
  /**
   * Name of the attribute that specifies the size of a notification buffer for a connector server. The default value is 1000.
   */
  public static final String BUFFER_SIZE_PROPERTY = "jmx.remote.x.notification.buffer.size";
  /**
   * Name of the attribute that specifies the maximum number of notifications that a client will fetch from its server.
   * The value associated with this attribute should be an <code>Integer</code> object. The default value is 1000.
   */
  public static final String MAX_FETCH_NOTIFS = "jmx.remote.x.notification.fetch.max";
  /**
   * Name of the attribute that specifies the timeout for a client to fetch notifications from its server.
   * The value associated with this attribute should be a <code>Long</code> object. The default value is 60000 milleseconds.
   */
  public static final String FETCH_TIMEOUT = "jmx.remote.x.notification.fetch.timeout";
  /**
   *
   */
  public static final String DEFAULT_ORB = "java.naming.corba.orb";
  /**
   * Name of the attribute that specifies the timeout to keep a server side connection after answering last client request. The default value is 120000 milliseconds.
   */
  public static final String SERVER_CONNECTION_TIMEOUT = "jmx.remote.x.server.connection.timeout";
  /**
   * Default list of attributes not to show. This list is copied directly from the spec, plus {@code java.naming.security.*}.
   * Most of the attributes here would have been eliminated from the map anyway because they are typically not serializable.
   * But just in case they are, we list them here to conform to the spec.
   * @see #HIDDEN_ATTRIBUTES
   */
  public static final String DEFAULT_HIDDEN_ATTRIBUTES = "java.naming.security.* " + "jmx.remote.authenticator " + "jmx.remote.context " + "jmx.remote.default.class.loader "
      + "jmx.remote.message.connection.server " + "jmx.remote.object.wrapping " + "jmx.remote.rmi.client.socket.factory " + "jmx.remote.rmi.server.socket.factory "
      + "jmx.remote.sasl.callback.handler " + "jmx.remote.tls.socket.factory " + "jmx.remote.x.access.file " + "jmx.remote.x.password.file ";
  /**
   *
   */
  private static final SortedSet<String> defaultHiddenStrings = new TreeSet<>();
  /**
   *
   */
  private static final SortedSet<String> defaultHiddenPrefixes = new TreeSet<>();
  /**
   * The value of this attribute, if present, is a string specifying what other attributes should not appear in JMXConnectorServer.getAttributes().
   * It is a space-separated list of attribute patterns, where each pattern is either an attribute name, or an attribute prefix followed by a "*"
   * character. The "*" has no special significance anywhere except at the end of a pattern. By default, this list is added to the list defined by
   * {@link #DEFAULT_HIDDEN_ATTRIBUTES} (which uses the same format). If the value of this attribute begins with an "=", then the remainder of the
   * string defines the complete list of attribute patterns.
   */
  public static final String HIDDEN_ATTRIBUTES = "jmx.remote.x.hidden.attributes";
  /**
   * Name of the attribute that specifies the period in millisecond for a client to check its connection. The default value is 60000 milliseconds.
   */
  public static final String CLIENT_CONNECTION_CHECK_PERIOD = "jmx.remote.x.client.connection.check.period";

  /**
   * Get the Connector Server default class loader.
   * <p>Returns:
   * <ul>
   * <li>The ClassLoader object found in <var>env</var> for <tt>jmx.remote.default.class.loader</tt>, if any.</li>
   * <li>The ClassLoader pointed to by the ObjectName found in <var>env</var> for <tt>jmx.remote.default.class.loader.name</tt>, and registered in <var>mbs</var> if any.</li>
   * <li>The current thread's context classloader otherwise.</li>
   * </ul>
   * @param env Environment attributes.
   * @param mbs The MBeanServer for which the connector server provides remote access.
   * @return the connector server's default class loader.
   * @exception IllegalArgumentException if one of the following is true:
   * <ul>
   * <li>both <tt>jmx.remote.default.class.loader</tt> and <tt>jmx.remote.default.class.loader.name</tt> are specified,</li>
   * <li>or <tt>jmx.remote.default.class.loader</tt> is not an instance of {@link ClassLoader},</li>
   * <li>or <tt>jmx.remote.default.class.loader.name</tt> is not an instance of {@link ObjectName},</li>
   * <li>or <tt> jmx.remote.default.class.loader.name</tt> is specified but <var>mbs</var> is null.</li>
   * </ul>
   * @exception InstanceNotFoundException if <tt>jmx.remote.default.class.loader.name</tt> is specified and the ClassLoader MBean is not found in <var>mbs</var>.
   */
  public static ClassLoader resolveServerClassLoader(final Map<String, ?> env, final MBeanServer mbs) throws InstanceNotFoundException {
    if (env == null) return Thread.currentThread().getContextClassLoader();
    final Object loader = env.get(DEFAULT_CLASS_LOADER);
    final Object name = env.get(DEFAULT_CLASS_LOADER_NAME);
    if (loader != null && name != null) {
      final String msg = "Only one of " + DEFAULT_CLASS_LOADER + " or " + DEFAULT_CLASS_LOADER_NAME + " should be specified.";
      throw new IllegalArgumentException(msg);
    }
    if (loader == null && name == null) return Thread.currentThread().getContextClassLoader();
    if (loader != null) {
      if (loader instanceof ClassLoader) return (ClassLoader) loader;
      else {
        final String msg = "ClassLoader object is not an instance of " + ClassLoader.class.getName() + " : " + loader.getClass().getName();
        throw new IllegalArgumentException(msg);
      }
    }
    final ObjectName on;
    if (name instanceof ObjectName) on = (ObjectName) name;
    else {
      final String msg = "ClassLoader name is not an instance of " + ObjectName.class.getName() + " : " + name.getClass().getName();
      throw new IllegalArgumentException(msg);
    }
    if (mbs == null) throw new IllegalArgumentException("Null MBeanServer object");
    return mbs.getClassLoader(on);
  }

  /**
   * Get the Connector Client default class loader.
   * Returns:
   * <ul>
   * <li>The ClassLoader object found in <var>env</var> for <tt>jmx.remote.default.class.loader</tt>, if any.</li>
   * <li>The <tt>Thread.currentThread().getContextClassLoader()</tt> otherwise.</li>
   * </ul>
   * <p>Usually a Connector Client will call
   * <pre>ClassLoader dcl = EnvHelp.resolveClientClassLoader(env);</pre>
   * in its <tt>connect(Map env)</tt> method.
   * @param env Environment attributes.
   * @return The connector client default class loader.
   * @exception IllegalArgumentException if <tt>jmx.remote.default.class.loader</tt> is specified and is not an instance of {@link ClassLoader}.
   */
  public static ClassLoader resolveClientClassLoader(final Map<String, ?> env) {
    if (env == null) return Thread.currentThread().getContextClassLoader();
    final Object loader = env.get(DEFAULT_CLASS_LOADER);
    if (loader == null) return Thread.currentThread().getContextClassLoader();
    if (loader instanceof ClassLoader) return (ClassLoader) loader;
    else {
      final String msg = "ClassLoader object is not an instance of " + ClassLoader.class.getName() + " : " + loader.getClass().getName();
      throw new IllegalArgumentException(msg);
    }
  }

  /**
   * Init the cause field of a Throwable object. The cause field is set only if <var>t</var> has an {@link Throwable#initCause(Throwable)} method (JDK Version >= 1.4)
   * @param t Throwable on which the cause must be set.
   * @param cause The cause to set on <var>t</var>.
   * @return <var>t</var> with or without the cause field set.
   */
  public static Throwable initCause(final Throwable t, final Throwable cause) {
    // Make a best effort to set the cause, but if we don't succeed, too bad, you don't get that useful debugging information.
    // We jump through hoops here so that we can work on platforms prior to J2SE 1.4 where the Throwable.initCause method was introduced.
    // If we change the public interface of JMRuntimeException in a future version we can add getCause() so we don't need to do this.
    try {
      final java.lang.reflect.Method initCause = t.getClass().getMethod("initCause", new Class[] { Throwable.class });
      initCause.invoke(t, new Object[] { cause });
    } catch (@SuppressWarnings("unused") final Exception e) {
      // OK. too bad, no debugging info
    }
    return t;
  }

  /**
   * Returns the cause field of a Throwable object. The cause field can be got only if <var>t</var> has an {@link Throwable#getCause()} method (JDK Version >= 1.4)
   * @param t Throwable on which the cause must be set.
   * @return the cause if getCause() succeeded and the got value is not null, otherwise return the <var>t</var>.
   */
  public static Throwable getCause(final Throwable t) {
    Throwable ret = t;
    try {
      final java.lang.reflect.Method getCause = t.getClass().getMethod("getCause", (Class[]) null);
      ret = (Throwable) getCause.invoke(t, (Object[]) null);
    } catch (@SuppressWarnings("unused") final Exception e) {
      // OK. it must be older than 1.4.
    }
    return (ret != null) ? ret : t;
  }

  /**
   * Returns the size of a notification buffer for a connector server. The default value is 1000.
   * @param env Environment attributes.
   * @return the size of the notification buffer.
   */
  public static int getNotifBufferSize(final Map<String, ?> env) {
    int defaultQueueSize = 1000; // default value

    // keep it for the compability for the fix:
    // 6174229: Environment parameter should be notification.buffer.size instead of buffer.size
    final String oldP = "jmx.remote.x.buffer.size";
    // the default value re-specified in the system
    try {
      GetPropertyAction act = new GetPropertyAction(BUFFER_SIZE_PROPERTY);
      String s = AccessController.doPrivileged(act);
      if (s != null) {
        defaultQueueSize = Integer.parseInt(s);
      } else { // try the old one
        act = new GetPropertyAction(oldP);
        s = AccessController.doPrivileged(act);
        if (s != null) {
          defaultQueueSize = Integer.parseInt(s);
        }
      }
    } catch (final RuntimeException e) {
      logger.warning("getNotifBufferSize", "Can't use System property " + BUFFER_SIZE_PROPERTY + ": " + e);
      logger.debug("getNotifBufferSize", e);
    }
    int queueSize = defaultQueueSize;
    try {
      if (env.containsKey(BUFFER_SIZE_PROPERTY)) {
        queueSize = (int) EnvHelp.getIntegerAttribute(env, BUFFER_SIZE_PROPERTY, defaultQueueSize, 0, Integer.MAX_VALUE);
      } else { // try the old one
        queueSize = (int) EnvHelp.getIntegerAttribute(env, oldP, defaultQueueSize, 0, Integer.MAX_VALUE);
      }
    } catch (final RuntimeException e) {
      logger.warning("getNotifBufferSize", "Can't determine queuesize (using default): " + e);
      logger.debug("getNotifBufferSize", e);
    }
    return queueSize;
  }

  /**
   * Returns the maximum notification number which a client will fetch every time.
   * @param env Environment attributes.
   * @return the max number of notifications as a long.
   */
  public static int getMaxFetchNotifNumber(final Map<String, ?> env) {
    return (int) getIntegerAttribute(env, MAX_FETCH_NOTIFS, 1000, 1, Integer.MAX_VALUE);
  }

  /**
   * Returns the timeout for a client to fetch notifications.
   * @param env Environment attributes.
   * @return the timeout as a long.
   */
  public static long getFetchTimeout(final Map<String, ?> env) {
    return getIntegerAttribute(env, FETCH_TIMEOUT, 60000L, 0, Long.MAX_VALUE);
  }

  /**
   * Get an integer-valued attribute with name <code>name</code> from <code>env</code>.
   * If <code>env</code> is null, or does not contain an entry for <code>name</code>, return <code>defaultValue</code>.
   * The value may be a Number, or it may be a String that is parsable as a long.
   * It must be at least <code>minValue</code> and at most<code>maxValue</code>.
   * @param env Environment attributes.
   * @param name the name of the attrbiute to get.
   * @param defaultValue the default value ot give the attribute if undefined.
   * @param minValue the min value of the attribute.
   * @param maxValue the max value of the attribute.
   * @return the attribute value as a long.
   * @throws IllegalArgumentException if <code>env</code> contains an entry for <code>name</code> but it does not meet the constraints above.
   */
  public static long getIntegerAttribute(final Map<String, ?> env, final String name, final long defaultValue, final long minValue, final long maxValue) {
    final Object o;
    if (env == null || (o = env.get(name)) == null) return defaultValue;
    final long result;
    if (o instanceof Number) result = ((Number) o).longValue();
    else if (o instanceof String) result = Long.parseLong((String) o);
    else throw new IllegalArgumentException("Attribute " + name + " value must be Integer or String: " + o);
    if (result < minValue) throw new IllegalArgumentException("Attribute " + name + " value must be at least " + minValue + ": " + result);
    if (result > maxValue) throw new IllegalArgumentException("Attribute " + name + " value must be at most " + maxValue + ": " + result);
    return result;
  }

  /**
   * Check that all attributes have a key that is a String. Could make further checks, e.g. appropriate types for attributes.
   * @param attributes Environment attributes.
   */
  public static void checkAttributes(final Map<?, ?> attributes) {
    for (Object key: attributes.keySet()) {
      if (!(key instanceof String)) {
        final String msg = "Attributes contain key that is not a string: " + key;
        throw new IllegalArgumentException(msg);
      }
    }
  }

  /**
   * Return a writable map containing only those attributes that are serializable, and that are not hidden by jmx.remote.x.hidden.attributes or the default list of hidden attributes.
   * @param attributes Environment attributes.
   * @return a map of serializable attributes.
   */
  public static Map<String, Object> filterAttributes(final Map<String, ?> attributes) {
    if (logger.traceOn()) logger.trace("filterAttributes", "starts");
    final SortedMap<String, Object> map = new TreeMap<>(attributes);
    purgeUnserializable(map.values());
    hideAttributes(map);
    return map;
  }

  /**
   * Remove from the given Collection any element that is not a serializable object.
   * @param objects the objects to purge.
   */
  private static void purgeUnserializable(final Collection<Object> objects) {
    logger.trace("purgeUnserializable", "starts");
    ObjectOutputStream oos = null;
    int i = 0;
    for (final Iterator<Object> it = objects.iterator(); it.hasNext(); i++) {
      final Object v = it.next();
      if (v == null || v instanceof String) {
        if (logger.traceOn()) logger.trace("purgeUnserializable", "Value trivially serializable: " + v);
        continue;
      }
      try {
        if (oos == null) oos = new ObjectOutputStream(new SinkOutputStream());
        oos.writeObject(v);
        if (logger.traceOn()) logger.trace("purgeUnserializable", "Value serializable: " + v);
      } catch (final IOException e) {
        if (logger.traceOn()) logger.trace("purgeUnserializable", "Value not serializable: " + v + ": " + e);
        it.remove();
        oos = null; // ObjectOutputStream invalid after exception
      }
    }
  }

  /**
   * 
   * @param map Environment attributes.
   */
  private static void hideAttributes(final SortedMap<String, ?> map) {
    if (map.isEmpty()) return;
    final SortedSet<String> hiddenStrings;
    final SortedSet<String> hiddenPrefixes;
    String hide = (String) map.get(HIDDEN_ATTRIBUTES);
    if (hide != null) {
      if (hide.startsWith("=")) hide = hide.substring(1);
      else hide += " " + DEFAULT_HIDDEN_ATTRIBUTES;
      hiddenStrings = new TreeSet<>();
      hiddenPrefixes = new TreeSet<>();
      parseHiddenAttributes(hide, hiddenStrings, hiddenPrefixes);
    } else {
      hide = DEFAULT_HIDDEN_ATTRIBUTES;
      synchronized (defaultHiddenStrings) {
        if (defaultHiddenStrings.isEmpty()) parseHiddenAttributes(hide, defaultHiddenStrings, defaultHiddenPrefixes);
        hiddenStrings = defaultHiddenStrings;
        hiddenPrefixes = defaultHiddenPrefixes;
      }
    }
    // Construct a string that is greater than any key in the map.
    // Setting a string-to-match or a prefix-to-match to this string guarantees that we will never call next() on the corresponding iterator.
    final String sentinelKey = map.lastKey() + "X";
    final Iterator<String> keyIterator = map.keySet().iterator();
    final Iterator<String> stringIterator = hiddenStrings.iterator();
    final Iterator<String> prefixIterator = hiddenPrefixes.iterator();
    String nextString;
    if (stringIterator.hasNext()) nextString = stringIterator.next();
    else nextString = sentinelKey;
    String nextPrefix;
    if (prefixIterator.hasNext()) nextPrefix = prefixIterator.next();
    else nextPrefix = sentinelKey;
    // Read each key in sorted order and, if it matches a string or prefix, remove it.
    keys: while (keyIterator.hasNext()) {
      final String key = keyIterator.next();
      //Continue through string-match values until we find one that is either greater than the current key, or equal to it. In the latter case, remove the key.
      int cmp = +1;
      while ((cmp = nextString.compareTo(key)) < 0) {
        if (stringIterator.hasNext()) nextString = stringIterator.next();
        else nextString = sentinelKey;
      }
      if (cmp == 0) {
        keyIterator.remove();
        continue keys;
      }
      // Continue through the prefix values until we find one that is either greater than the current key, or a prefix of it. In the latter case, remove the key.
      while (nextPrefix.compareTo(key) <= 0) {
        if (key.startsWith(nextPrefix)) {
          keyIterator.remove();
          continue keys;
        }
        if (prefixIterator.hasNext()) nextPrefix = prefixIterator.next();
        else nextPrefix = sentinelKey;
      }
    }
  }

  /**
   * 
   * @param hide .
   * @param hiddenStrings .
   * @param hiddenPrefixes .
   */
  private static void parseHiddenAttributes(final String hide, final SortedSet<String> hiddenStrings, final SortedSet<String> hiddenPrefixes) {
    final StringTokenizer tok = new StringTokenizer(hide);
    while (tok.hasMoreTokens()) {
      final String s = tok.nextToken();
      if (s.endsWith("*")) hiddenPrefixes.add(s.substring(0, s.length() - 1));
      else hiddenStrings.add(s);
    }
  }

  /**
   * Returns the server side connection timeout.
   * @param env Environment attributes.
   * @return the connection timeout.
   */
  public static long getServerConnectionTimeout(final Map<String, ?> env) {
    return getIntegerAttribute(env, SERVER_CONNECTION_TIMEOUT, 120000L, 0, Long.MAX_VALUE);
  }

  /**
   * Returns the client connection check oeriod.
   * @param env Environment attributes.
   * @return the connection check period.
   */
  public static long getConnectionCheckPeriod(final Map<String, ?> env) {
    return getIntegerAttribute(env, CLIENT_CONNECTION_CHECK_PERIOD, 60000L, 0, Long.MAX_VALUE);
  }

  /**
   * Converts a map into a valid hash table, i.e. it removes all the {@code null} values from the map.
   * @param map Environment attributes.
   * @return a hash table with no null values.
   */
  public static Hashtable<String, Object> mapToHashtable(final Map<String, ?> map) {
    final Map<String, Object> m = new HashMap<>(map);
    if (m.containsKey(null)) m.remove(null);
    for (final Iterator<Object> i = m.values().iterator(); i.hasNext();)
      if (i.next() == null) i.remove();
    return new Hashtable<>(m);
  }

  /**
   * 
   */
  private static final class SinkOutputStream extends OutputStream {
    @Override
    public void write(final byte[] b, final int off, final int len) {
    }

    @Override
    public void write(final int b) {
    }
  }
}
