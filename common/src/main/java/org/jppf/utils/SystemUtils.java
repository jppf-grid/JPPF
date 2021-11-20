/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import static org.jppf.utils.PropertyType.*;

import java.io.File;
import java.net.InetAddress;
import java.security.*;
import java.util.*;

import javax.management.MBeanServer;

import org.slf4j.*;

/**
 * Collection of utility methods used as a convenience for retrieving
 * JVM-level or System-level information.
 * @author Laurent Cohen
 */
public final class SystemUtils {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(SystemUtils.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Singleton holding the unchanging system properties.
   */
  private static TypedProperties systemProps;
  /**
   * A map of the environment properties.
   */
  private static TypedProperties env;
  /**
   * A map of the network configuration.
   */
  private static TypedProperties network;
  /**
   * A map of the physical RAM information.
   */
  private static TypedProperties os;
  /**
   * Windows OS.
   */
  private static final int WINDOWS_OS = 1;
  /**
   * X11 based OS.
   */
  private static final int X11_OS = 2;
  /**
   * The MAC based OS.
   */
  private static final int MAC_OS = 3;
  /**
   * Unknown or unsupported OS.
   */
  private static final int UNKNOWN_OS = -1;
  /**
   * The type of this host's OS.
   */
  private static final int OS_TYPE = determineOSType();
  /**
   * This process id.
   */
  private static final int PID = determinePID();
  /**
   * The runtime for the current JVM.
   */
  private static final Runtime RUNTIME = Runtime.getRuntime();
  /**
   * The maximum heap memory the JVM will attempt yo use.
   */
  private static final long MAX_MEMORY = RUNTIME.maxMemory();

  /**
   * Instantiation of this class is not permitted.
   */
  private SystemUtils() {
  }

  /**
   * Return a set of properties guaranteed to always be part of those returned by
   * {@link java.lang.System#getProperties() System.getProperties()}.
   * @return the properties as a <code>TypedProperties</code> instance.
   */
  public static synchronized TypedProperties getSystemProperties() {
    if (systemProps == null) {
      final TypedProperties props = new TypedProperties();
      addOtherSystemProperties(props);
      systemProps = props;
    }
    return systemProps;
  }

  /**
   * Add system properties whose name is not known in advance.
   * @param props the TypedProperties instance to add the system properties to.
   */
  private static void addOtherSystemProperties(final TypedProperties props) {
    try {
      // run as privileged so we don't have to set write access on all properties in the security policy file.
      final TypedProperties sysProps = AccessController.doPrivileged((PrivilegedAction<TypedProperties>) () -> {
        synchronized(System.getProperties()) {
          return new TypedProperties(System.getProperties());
        }
      });
      final Enumeration<?> en = sysProps.propertyNames();
      while (en.hasMoreElements()) {
        final String name = (String) en.nextElement();
        try {
          if (!props.containsKey(name)) {
            final String value = sysProps.getProperty(name);
            if (value != null) props.setProperty(name, value);
          }
        } catch(final SecurityException e) {
          if (debugEnabled) log.debug(e.getMessage(), e);
          else log.info(e.getMessage());
        }
      }
    } catch(final SecurityException e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.info(e.getMessage());
    }
  }

  /**
   * Get information about the number of processors available to the JVM and the JVM memory usage.
   * @return a <code>TypedProperties</code> instance holding the requested information.
   */
  public static TypedProperties getRuntimeInformation() {
    final TypedProperties props = new TypedProperties();
    try {
      final Runtime rt = Runtime.getRuntime();
      props.setInt("availableProcessors", rt.availableProcessors());
      props.setLong("freeMemory", rt.freeMemory());
      props.setLong("totalMemory", rt.totalMemory());
      props.setLong("maxMemory", rt.maxMemory());
      final long usedMemory = rt.totalMemory() - rt.freeMemory();
      props.setLong("usedMemory", usedMemory);
      props.setLong("availableMemory", rt.maxMemory() - usedMemory);
      if (ManagementUtils.isManagementAvailable()) {
        final MBeanServer mbeanServer = ManagementUtils.getPlatformServer();
        final String mbeanName = "java.lang:type=Runtime";
        String s = String.valueOf(ManagementUtils.getAttribute(mbeanServer, mbeanName, "StartTime"));
        props.setProperty("startTime", s);
        s = String.valueOf(ManagementUtils.getAttribute(mbeanServer, mbeanName, "Uptime"));
        props.setProperty("uptime", s);
        final String[] inputArgs = (String[]) ManagementUtils.getAttribute(mbeanServer, mbeanName, "InputArguments");
        props.setProperty("inputArgs", StringUtils.arrayToString(", ", null, null, inputArgs));
      }
    } catch(final Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.info(e.getMessage());
    }
    return props;
  }

  /**
   * Get storage information about the file system roots available on the current host.
   * This method populates a {@link TypedProperties} object with root name, free space,
   * total space and usable space information for each root.
   * <p>An example root name would be &quot;C:\&quot; for a Windows system and &quot;/&quot; for a Unix system.
   * @return TypedProperties object with storage information.
   */
  public static synchronized TypedProperties getStorageInformation() {
    final TypedProperties props = new TypedProperties();
    final File[] roots = File.listRoots();
    props.setInt("host.roots.number", roots == null ? 0 : roots.length);
    if ((roots == null) || (roots.length <= 0)) return props;
    final StringBuilder sb = new StringBuilder();
    for (int i=0; i<roots.length; i++) {
      try {
        if (i > 0) sb.append(' ');
        final String s = roots[i].getCanonicalPath();
        sb.append(s);
        final String prefix = "root." + i;
        props.setProperty(prefix + ".name", s);
        props.setLong(prefix + ".space.total", roots[i].getTotalSpace());
        props.setLong(prefix + ".space.free", roots[i].getFreeSpace());
        props.setLong(prefix + ".space.usable", roots[i].getUsableSpace());
      } catch(final Exception e) {
        if (debugEnabled) log.debug(e.getMessage(), e);
        else log.warn(e.getMessage());
      }
    }
    props.setProperty("host.roots.names", sb.toString());
    return props;
  }

  /**
   * Get a map of the environment variables.
   * @return a mapping of environment variables to their value.
   */
  public static synchronized TypedProperties getEnvironment() {
    if (env == null) {
      env = new TypedProperties();
      try {
        final Map<String, String> props = System.getenv();
        for (Map.Entry<String, String> entry: props.entrySet()) {
          env.setProperty(entry.getKey(), entry.getValue());
        }
      } catch(final SecurityException e) {
        if (debugEnabled) log.debug(e.getMessage(), e);
        else log.info(e.getMessage());
      }
    }
    return env;
  }

  /**
   * Get a map of the environment variables.
   * @return a mapping of environment variables to their value.
   */
  public static synchronized TypedProperties getNetwork() {
    if (network == null) {
      network = new TypedProperties();
      try {
        network.setProperty("ipv4.addresses", formatAddresses(NetworkUtils.getIPV4Addresses()));
        network.setProperty("ipv6.addresses", formatAddresses(NetworkUtils.getIPV6Addresses()));
      } catch(final SecurityException e) {
        if (debugEnabled) log.debug(e.getMessage(), e);
        else log.info(e.getMessage());
      }
    }
    return network;
  }

  /**
   * Format a list of InetAddress.
   * @param addresses a List of <code>InetAddress</code> instances.
   * @return a string containing a space-separated list of <i>hostname</i>|<i>ip_address</i> pairs.
   */
  private static String formatAddresses(final List<? extends InetAddress> addresses) {
    final StringBuilder sb = new StringBuilder();
    for (final InetAddress addr: addresses) {
      final String name = addr.getHostName();
      final String ip = addr.getHostAddress();
      if (sb.length() > 0) sb.append(' ');
      sb.append(name).append('|').append(ip);
    }
    return sb.toString();
  }

  /**
   * Compute the maximum memory currently available for the Java heap.
   * @return the maximum number of free bytes in the heap.
   */
  public static long maxFreeHeap() {
    return MAX_MEMORY - (RUNTIME.totalMemory() - RUNTIME.freeMemory());
  }

  /**
   * Compute the used heap.
   * @return the heap memory used in bytes.
   */
  public static long heapUsage() {
    return RUNTIME.totalMemory() - RUNTIME.freeMemory();
  }

  /**
   * Compute the percentage of used heap as compared to the maximum heap size of the JVM.
   * @return the percentage of heap memory used.
   */
  public static double heapUsagePct() {
    return 100d * (RUNTIME.totalMemory() - RUNTIME.freeMemory()) / MAX_MEMORY;
  }

  /**
   * Determine the type of this host's operating system, based on the value
   * of the system property &quot;os.name&quot;.
   * @return an int value identifying the type of OS.
   */
  private static int determineOSType() {
    final String name = System.getProperty("os.name");
    if (StringUtils.startsWithOneOf(name, true, "Linux", "SunOS", "Solaris", "FreeBSD", "OpenBSD")) return X11_OS;
    else if (StringUtils.startsWithOneOf(name, true, "Mac", "Darwin")) return MAC_OS;
    else if (name.startsWith("Windows") && !name.startsWith("Windows CE")) return WINDOWS_OS;
    return UNKNOWN_OS;
  }

  /**
   * Determine whether the current OS is Windows.
   * @return true if the system is Windows, false otherwise.
   */
  public static boolean isWindows() {
    return OS_TYPE == WINDOWS_OS;
  }

  /**
   * Determine whether the current OS is X11-based.
   * @return true if the system is X11, false otherwise.
   */
  public static boolean isX11() {
    return OS_TYPE == X11_OS;
  }

  /**
   * Determine whether the current OS is Mac-based.
   * @return true if the system is Mac-based, false otherwise.
   */
  public static boolean isMac() {
    return OS_TYPE == MAC_OS;
  }

  /**
   * Determine the current process ID.
   * @return the pid as an int, or -1 if the pid could not be obtained.
   */
  private static int determinePID() {
    int pid = -1;
    // we expect the name to be in '<pid>@hostname' format - this is JVM dependent
    try {
      if (ManagementUtils.isManagementAvailable()) {
        final String name = String.valueOf(ManagementUtils.getAttribute(ManagementUtils.getPlatformServer(), "java.lang:type=Runtime", "Name"));
        final int idx = name.indexOf('@');
        if (idx >= 0) {
          final String sub = name.substring(0, idx);
          try {
            pid = Integer.valueOf(sub);
            if (debugEnabled) log.debug("process name=" + name + ", pid=" + pid);
          } catch (final Exception e) {
            final String msg = "could not parse '" + sub +"' into a valid integer pid : " + ExceptionUtils.getMessage(e);
            if (debugEnabled) log.debug(msg, e);
            else log.warn(msg);
          }
        }
      }
    } catch (final Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(e.getMessage());
    }
    return pid;
  }

  /**
   * @return the current process pid as an int, or -1 if the pid could not be obtained.
   */
  public synchronized static int getPID() {
    return PID;
  }

  /**
   * Prints the JPPF process id and uuid to {@code System.out}.
   * @param component the JPPF component type: driver, node or client.
   * @param uuid the component uuid.
   */
  public static void printPidAndUuid(final String component, final String uuid) {
    StringBuilder sb = new StringBuilder(component == null ? "<unknown component type>" : component);
    final int pid = getPID();
    if (pid >= 0) sb = sb.append(" process id: ").append(pid).append(',');
    sb.append(" uuid: ").append(uuid);
    System.out.println(sb.toString());
  }

  /**
   * Get the available physical ram information for the local machine.
   * @return the memory information eelements as a {@link TypedProperties} instance.
   */
  public static TypedProperties getOS() {
    if (os == null) {
      os = new TypedProperties();
      if (ManagementUtils.isManagementAvailable()) {
        try {
          capture("TotalPhysicalMemorySize", os, LONG);
          capture("FreePhysicalMemorySize", os, LONG);
          capture("TotalSwapSpaceSize", os, LONG);
          capture("FreeSwapSpaceSize", os, LONG);
          capture("CommittedVirtualMemorySize", os, LONG);
          //capture("ProcessCpuLoad", os, DOUBLE);
          capture("ProcessCpuTime", os, LONG);
          //capture("SystemCpuLoad", os, DOUBLE);
          capture("Name", os, STRING);
          capture("Version", os, STRING);
          capture("Arch", os, STRING);
          capture("AvailableProcessors", os, INT);
          //capture("SystemLoadAverage", os, DOUBLE);
        } catch(final Exception e) {
          if (debugEnabled) log.debug(e.getMessage(), e);
          else log.info(e.getMessage());
        }
      }
    }
    return os;
  }

  /**
   * Capture the specified attribute into the specified TypedProperties object.
   * @param name the attribute name.
   * @param props the properties holding the attribute and its value.
   * @param type the type of the attribute.
   */
  private static void capture(final String name, final TypedProperties props, final PropertyType type) {
    if (!ManagementUtils.isManagementAvailable()) return;
    Object value = null;
    try {
      value = ManagementUtils.getAttribute(ManagementUtils.getPlatformServer(), "java.lang:type=OperatingSystem", name);
    } catch(@SuppressWarnings("unused") final Exception ignore) {
      return;
    }
    switch(type) {
      case INT:
        props.setInt("os." + name, value != null ? (Integer) value : -1);
        break;
      case LONG:
        props.setLong("os." + name, value != null ? (Long) value : -1L);
        break;
      case DOUBLE:
        props.setDouble("os." + name, value != null ? (Double) value : -1d);
        break;
      case STRING:
        props.setString("os." + name, value != null ? (String) value : "");
        break;
    }
  }

  /**
   * @return the currently used heap memory.
   */
  public static long getUsedMemory() {
    return RUNTIME.totalMemory() - RUNTIME.freeMemory();
  }

  /**
   * @return the currently used heap memory.
   */
  public static double getPctUsedMemory() {
    return (double) (RUNTIME.totalMemory() - RUNTIME.freeMemory()) / (double) MAX_MEMORY;
  }

  /**
   * Determine whether the specified value is one of the possibles.
   * @param <T> the type of values to check.
   * @param value the value to compare.
   * @param possibles the possible values to compare with
   * @return {@code true} if value is one of the possibe values, {@code false} otherwise.
   */
  @SafeVarargs
  public static <T> boolean isOneOf(final T value, final T...possibles) {
    if ((possibles == null) || (possibles.length <= 0)) return false;
    for (T t: possibles) {
      if (value == t) return true;
      if ((value != null) && (t != null) && t.equals(value)) return true;
    }
    return false;
  }

  /**
   * Print the system properties to a string.
   * @return the system properties printed one per line.
   */
  public static String printSystemProperties() {
    final Enumeration<?> names = System.getProperties().propertyNames();
    final Map<String, String> props = new TreeMap<>();
    while (names.hasMoreElements()) {
      final String name = (String) names.nextElement();
      props.put(name, System.getProperty(name));
    }
    final StringBuilder sb = new StringBuilder("{\n");
    for (Map.Entry<String, String> entry: props.entrySet()) {
      sb.append("  ").append(entry.getKey()).append(" = ").append(entry.getValue()).append('\n');
    }
    sb.append('}');
    return sb.toString();
  }

  /**
   * Get a name for an object in the form simple_class_name@hex_identity_hashcode.
   * @param o the object for which to get a name.
   * @return the object's identity name.
   */
  public static String getSystemIdentityName(final Object o) {
    return o.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(o));
  }

  /**
   * Get a name for an object in the form class_fqn@hex_identity_hashcode.
   * @param o the object for which to get a name.
   * @return the object's identity name.
   */
  public static String getSystemIdentity(final Object o) {
    return o.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(o));
  }

  /**
   * Determine whether 2 objects are equal.
   * @param o1 the first object.
   * @param o2 the second object.
   * @return {@code true} if the 2 objects are equal, {@code false} otherwise.
   */
  public static boolean areEqual(final Object o1, final Object o2) {
    if (o1 == o2) return true;
    if ((o1 == null) || (o2 == null)) return false;
    return o1.equals(o2);
  }

  /**
   * Get a pretty formatted calss path from the system properties.
   * @return a class path with one path per line.
   */
  public static String prettySystemClassPath() {
    final String sep = System.getProperty("path.separator");
    String cp = System.getProperty("java.class.path");
    if (cp == null) return "null";
    cp = cp.replace(sep, "\n");
    if (cp.endsWith("\n")) cp = cp.substring(0, cp.length() - 1);
    return cp;
  }
}
