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

package org.jppf.management;

import java.util.*;

import org.jppf.utils.*;
import org.jppf.utils.stats.*;
import org.slf4j.*;

/**
 * This class encapsulates the system information for a node.<br>
 * It includes:
 * <ul>
 * <li>System properties, including -D flags</li>
 * <li>Runtime information such as available processors and memory usage</li>
 * <li>Environment variables</li>
 * <li>JPPF configuration properties</li>
 * <li>IPV4 and IPV6 addresses assigned to the JVM host</li>
 * <li>Disk space information</li>
 * <li>Server statistics (server-side only)</li>
 * </ul>
 * @author Laurent Cohen
 */
public class JPPFSystemInformation implements PropertiesCollection<String> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFSystemInformation.class);
  /**
   * Determines whether the trace level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Mapping of all properties containers.
   */
  private Map<String, TypedProperties> map = new HashMap<>();
  /**
   * {@code true} if the JPPF component is local (local node or local client executor), {@code false} otherwise.
   */
  private boolean local;
  /**
   * If {@code true}, then the name resolution for {@code InetAddress}es should occur immediately,
   */
  private boolean resolveInetAddressesNow;
  /**
   *
   */
  private transient TypedProperties[] propertiesArray;
  /**
   * An optional statistics object from which events can be received so the corresponding properties can be kept up to date.
   */
  private transient JPPFStatistics stats;

  /**
   * Initialize this system information object with the specified uuid.
   * @param uuid the uuid of the corresponding JPPF component.
   * @param local {@code true} if the JPPF component is local (local node or local client executor), {@code false} otherwise.
   * @param resolveInetAddressesNow if {@code true}, then name resolution for {@code InetAddress}es should occur immediately,
   * otherwise it is different and executed in a separate thread.
   */
  public JPPFSystemInformation(final String uuid, final boolean local, final boolean resolveInetAddressesNow) {
    this(uuid, local, resolveInetAddressesNow, null);
  }

  /**
   * Initialize this system information object with the specified uuid.
   * @param uuid the uuid of the corresponding JPPF component.
   * @param local {@code true} if the JPPF component is local (local node or local client executor), {@code false} otherwise.
   * @param resolveInetAddressesNow if {@code true}, then name resolution for {@code InetAddress}es should occur immediately,
   * otherwise it is different and executed in a separate thread.
   * @param stats an optional statistics object from which events can be received so the corresponding properties can be kept up to date.
   */
  public JPPFSystemInformation(final String uuid, final boolean local, final boolean resolveInetAddressesNow, final JPPFStatistics stats) {
    this.local = local;
    this.resolveInetAddressesNow = resolveInetAddressesNow;
    this.stats = stats;
    final TypedProperties uuidProps = new TypedProperties();
    uuidProps.setProperty("jppf.uuid", (uuid == null) ? "" : uuid);
    uuidProps.setInt("jppf.pid", SystemUtils.getPID());
    final VersionUtils.Version v = VersionUtils.getVersion();
    uuidProps.setProperty("jppf.version.number", v.getVersionNumber());
    uuidProps.setProperty("jppf.build.number", v.getBuildNumber());
    uuidProps.setProperty("jppf.build.date", v.getBuildDate());
    addProperties("uuid", uuidProps);
    populate();
  }

  /**
   * Get the map holding the system properties.
   * @return a {@code TypedProperties} instance.
   * @see org.jppf.utils.SystemUtils#getSystemProperties()
   */
  public TypedProperties getSystem() {
    return getProperties("system");
  }

  /**
   * Get the map holding the runtime information.
   * <p>The resulting map will contain the following properties:
   * <ul>
   * <li>availableProcessors = <i>number of processors available to the JVM</i></li>
   * <li>freeMemory = <i>current free heap size in bytes</i></li>
   * <li>totalMemory = <i>current total heap size in bytes</i></li>
   * <li>maxMemory = <i>maximum heap size in bytes (i.e. as specified by -Xmx JVM option)</i></li>
   * <li>usedMemory = <i>currently used heap memory in bytes</i></li>
   * <li>availableMemory = <i>the currently available heap memory in bytes; equal to: </i>{@code maxMemory - usedMemory}</li>
   * <li>startTime = <i>the approximate timestamp in millis of when the JVM started</i></li>
   * <li>upTime = <i>how long the JVM has been up in millis</i></li>
   * <li>inputArgs = <i>arguments given to the 'java' command, excluding those passed to the main method.
   * Arguments are given as a list of strings separated by the ", " delimiter (a comma followed by a space)</i></li>
   * </ul>
   * <p>Some or all of these properties may be missing if a security manager is installed
   * that does not grant access to the related {@link java.lang.Runtime} APIs.
   * @return a {@code TypedProperties} instance.
   * @see org.jppf.utils.SystemUtils#getRuntimeInformation()
   */
  public TypedProperties getRuntime() {
    return getProperties("runtime");
  }

  /**
   * Get the map holding the environment variables.
   * @return a {@code TypedProperties} instance.
   * @see org.jppf.utils.SystemUtils#getEnvironment()
   */
  public TypedProperties getEnv() {
    return getProperties("env");
  }

  /**
   * Get the  map of the network configuration.
   * <p>The resulting map will contain the following properties:
   * <ul>
   * <li>ipv4.addresses = <i>hostname_1</i>|<i>ipv4_address_1</i> ... <i>hostname_n</i>|<i>ipv4_address_n</i></li>
   * <li>ipv6.addresses = <i>hostname_1</i>|<i>ipv6_address_1</i> ... <i>hostname_p</i>|<i>ipv6_address_p</i></li>
   * </ul>
   * <p>Each property is a space-separated list of <i>hostname</i>|<i>ip_address</i> pairs,
   * the hostname and ip address being separated by a pipe symbol &quot;|&quot;.
   * @return a {@code TypedProperties} instance.
   * @see org.jppf.utils.SystemUtils#getNetwork()
   */
  public TypedProperties getNetwork() {
    return getProperties("network");
  }

  /**
   * Get the map holding the JPPF configuration properties.
   * @return a {@code TypedProperties} instance.
   * @see org.jppf.utils.JPPFConfiguration#getProperties()
   */
  public TypedProperties getJppf() {
    return getProperties("jppf");
  }

  /**
   * Get the map holding the host storage information.
   * <p>The map will contain the following information:
   * <ul>
   * <li>host.roots.names = <i>root_name_0</i> ... <i>root_name_n-1</i> : the names of all accessible file system roots</li>
   * <li>host.roots.number = <i>n</i> : the number of accessible file system roots</li>
   * <li><b>For each root <i>i</i>:</b></li>
   * <li>root.<i>i</i>.name = <i>root_name</i> : for instance &quot;C:\&quot; on Windows or &quot;/&quot; on Unix</li>
   * <li>root.<i>i</i>.space.free = <i>space_in_bytes</i> : current free space for the root</li>
   * <li>root.<i>i</i>.space.total = <i>space_in_bytes</i> : total space for the root</li>
   * <li>root.<i>i</i>.space.usable = <i>space_in_bytes</i> : space available to the user the JVM is running under</li>
   * </ul>
   * If the JVM version is prior to 1.6, the space information will not be available.
   * @return a {@code TypedProperties} instance.
   * @see org.jppf.utils.SystemUtils#getStorageInformation()
   */
  public TypedProperties getStorage() {
    return getProperties("storage");
  }

  /**
   * Get the properties object holding the JPPF uuid and version information.
   * The following properties are provided:
   * <ul>
   * <li>"jppf.uuid" : the uuid of the node or driver</li>
   * <li>"jppf.pid" : the process id of the JVM running the JPPF driver or node</li>
   * <li>"jppf.version.number" : the current JPPF version number</li>
   * <li>"jppf.build.number" : the current build number</li>
   * <li>"jppf.build.date" : the build date, including the time zone, in the format "yyyy-MM-dd hh:mm z" </li>
   * </ul>
   * @return a {@code TypedProperties} wrapper for the uuid and version information of the corresponding JPPF component.
   */
  public TypedProperties getUuid() {
    return getProperties("uuid");
  }

  /**
   * Get the properties object holding the JPPF server statistics, listed as constants in {@link JPPFStatisticsHelper}.
   * @return a {@code TypedProperties} wrapper for the server statistics; for a node this will return an empty set of properties.
   */
  public TypedProperties getStats() {
    return getProperties("stats");
  }

  /**
   * Get the properties object holding the operating system information.
   * The following properties are provided:
   * <ul>
   * <li>"os.TotalPhysicalMemorySize" : total physical RAM (long value)</li>
   * <li>"os.FreePhysicalMemorySize" : available physical RAM (long value)</li>
   * <li>"os.TotalSwapSpaceSize" : total swap space (long value)</li>
   * <li>"os.FreeSwapSpaceSize" : available swap space (long value)</li>
   * <li>"os.CommittedVirtualMemorySize" : total committed virtual memory of the process (long value)</li>
   * <li>"os.ProcessCpuLoad" : process CPU load (double value in range [0 ... 1])</li>
   * <li>"os.ProcessCpuTime" : process CPU time (long value)</li>
   * <li>"os.SystemCpuLoad" : system total CPU load (double value in range [0 ... 1])</li>
   * <li>"os.Name" : the name of the OS (string value, ex: "Windows 7")</li>
   * <li>"os.Version" : the OS version (string value, ex: "6.1")</li>
   * <li>"os.Arch" : the OS architecture (string value, ex: "amd64")</li>
   * <li>"os.AvailableProcessors" : number of processors available to the OS (int value)</li>
   * <li>"os.SystemLoadAverage" : average system CPU load over the last minute (double value in the range [0 ... 1], always returns -1 on Windows)</li>
   * </ul>
   * @return a {@code TypedProperties} wrapper for the operating system information of the corresponding JPPF component.
   */
  public TypedProperties getOS() {
    return getProperties("os");
  }

  /**
   * Get all the properties as an array.
   * @return an array of all the sets of properties.
   */
  @Override
  public TypedProperties[] getPropertiesArray() {
    synchronized(map) {
      if (propertiesArray == null) propertiesArray = map.values().toArray(new TypedProperties[map.size()]);
      return propertiesArray;
    }
  }

  @Override
  public void addProperties(final String key, final TypedProperties properties) {
    synchronized(map) {
      map.put(key, properties);
      propertiesArray = map.values().toArray(new TypedProperties[map.size()]);
    }
  }

  @Override
  public TypedProperties getProperties(final String key) {
    synchronized(map) {
      return map.get(key);
    }
  }

  /**
   * Populate this system information object.
   * @return this {@code JPPFSystemInformation} object.
   */
  public JPPFSystemInformation populate() {
    if (traceEnabled) {
      final Exception e = new Exception("call stack for JPPFSystemInformation.populate(" + resolveInetAddressesNow + ")");
      log.trace(e.getMessage(), e);
    }
    addProperties("system", SystemUtils.getSystemProperties());
    addProperties("runtime", SystemUtils.getRuntimeInformation());
    addProperties("env", SystemUtils.getEnvironment());
    addProperties("jppf", new TypedProperties(JPPFConfiguration.getProperties()));
    getJppf().setProperty("jppf.channel.local", String.valueOf(local));
    final Runnable r = new Runnable() {
      @Override
      public void run() {
        addProperties("network", SystemUtils.getNetwork());
      }
    };
    if (resolveInetAddressesNow) r.run();
    else new Thread(r).start();
    addProperties("storage", SystemUtils.getStorageInformation());
    if (getProperties("uuid") == null) addProperties("uuid", new TypedProperties());
    addProperties("os", SystemUtils.getOS());
    final TypedProperties statsProperties = new TypedProperties();
    addProperties("stats", statsProperties);
    if (stats != null) {
      for (JPPFSnapshot snapshot: stats) JPPFStatisticsHelper.toProperties(statsProperties, snapshot);
      stats = null;
    }
    return this;
  }

  /**
   * Parse the list of IP v4 addresses contained in this JPPFSystemInformation instance.<br>
   * This method is provided as a convenience so developers don't have to do the parsing themselves.
   * @return an array of {@code HostIP} instances.
   * @exclude
   */
  public HostIP[] parseIPV4Addresses() {
    return NetworkUtils.parseAddresses(getNetwork().getString("ipv4.addresses"));
  }

  /**
   * Parse the list of IP v6 addresses contained in this JPPFSystemInformation instance.<br>
   * This method is provided as a convenience so developers don't have to do the parsing themselves.
   * @return an array of {@code HostIP} instances.
   * @exclude
   */
  public HostIP[] parseIPV6Addresses() {
    return NetworkUtils.parseAddresses(getNetwork().getString("ipv6.addresses"));
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("local=").append(local);
    sb.append(", resolveInetAddressesNow=").append(resolveInetAddressesNow);
    sb.append(", map=").append(map);
    return sb.append(']').toString();
  }

  @Override
  public String getProperty(final String name) {
    for (TypedProperties props: getPropertiesArray()) {
      if (props == null) continue;
      if (props.containsKey(name)) return props.getProperty(name);
    }
    return null;
  }

  @Override
  public boolean containsKey(final String name) {
    for (TypedProperties props: getPropertiesArray()) {
      if (props == null) continue;
      if (props.containsKey(name)) return true;
    }
    return false;
  }
}
