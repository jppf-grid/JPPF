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

package org.jppf.management;

import java.util.*;

import org.jppf.utils.*;
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
 * <li>Disk space information (JDK 1.6 or later only)</li>
 * </ul>
 * @author Laurent Cohen
 */
public class JPPFSystemInformation implements PropertiesCollection<String>
{
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
  private Map<String, TypedProperties> map = new LinkedHashMap<String, TypedProperties>();

  /**
   * Initialize this system information object with the specified uuid.
   * @param uuid the uuid of the corresponding JPPF component.
   */
  public JPPFSystemInformation(final String uuid)
  {
    TypedProperties uuidProps = new TypedProperties();
    uuidProps.setProperty("jppf.uuid", (uuid == null) ? "" : uuid);
    addProperties("uuid", uuidProps);
  }

  /**
   * Get the map holding the system properties.
   * @return a <code>TypedProperties</code> instance.
   * @see org.jppf.utils.SystemUtils#getSystemProperties()
   */
  public TypedProperties getSystem()
  {
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
   * </ul>
   * <p>Some or all of these properties may be missing if a security manager is installed
   * that does not grant access to the related {@link java.lang.Runtime} APIs.
   * @return a <code>TypedProperties</code> instance.
   * @see org.jppf.utils.SystemUtils#getRuntimeInformation()
   */
  public TypedProperties getRuntime()
  {
    return getProperties("runtime");
  }

  /**
   * Get the map holding the environment variables.
   * @return a <code>TypedProperties</code> instance.
   * @see org.jppf.utils.SystemUtils#getEnvironment()
   */
  public TypedProperties getEnv()
  {
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
   * @return a <code>TypedProperties</code> instance.
   * @see org.jppf.utils.SystemUtils#getNetwork()
   */
  public TypedProperties getNetwork()
  {
    return getProperties("network");
  }

  /**
   * Get the map holding the JPPF configuration properties.
   * @return a <code>TypedProperties</code> instance.
   * @see org.jppf.utils.JPPFConfiguration
   */
  public TypedProperties getJppf()
  {
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
   * @return a <code>TypedProperties</code> instance.
   * @see org.jppf.utils.SystemUtils#getStorageInformation()
   */
  public TypedProperties getStorage()
  {
    return getProperties("storage");
  }

  /**
   * Populate this node information object.
   * @return this <code>JPPFSystemInformation</code> object.
   */
  public JPPFSystemInformation populate()
  {
    return populate(true);
  }

  /**
   * Populate this node information object.
   * @param resolveInetAddressesNow if true, then name resolution for <code>InetAddress</code>es should occur immediately,
   * otherwise it is different and executed in a separate thread.
   * @return this <code>JPPFSystemInformation</code> object.
   */
  public JPPFSystemInformation populate(final boolean resolveInetAddressesNow)
  {
    if (traceEnabled)
    {
      Exception e = new Exception("call stack for JPPFSystemInformation.populate(" + resolveInetAddressesNow + ")");
      log.trace(e.getMessage(), e);
    }
    addProperties("system", SystemUtils.getSystemProperties());
    addProperties("runtime", SystemUtils.getRuntimeInformation());
    addProperties("env", SystemUtils.getEnvironment());
    addProperties("jppf", new TypedProperties(JPPFConfiguration.getProperties()));
    Runnable r = new Runnable() {
      @Override
      public void run() {
        addProperties("network", SystemUtils.getNetwork());
      }
    };
    if (resolveInetAddressesNow) r.run();
    else new Thread(r).start();
    addProperties("storage", SystemUtils.getStorageInformation());
    if (getProperties("uuid") == null) addProperties("uuid", new TypedProperties());
    return this;
  }

  /**
   * Parse the list of IP v4 addresses contained in this JPPFSystemInformation instance.<br>
   * This method is provided as a convenience so developers don't have to do the parsing themselves.
   * @return an array on <code>HostIP</code> instances.
   */
  private HostIP[] parseIPV4Addresses()
  {
    String s = getNetwork().getString("ipv4.addresses");
    if ((s == null) || "".equals(s.trim())) return null;
    return parseAddresses(s);
  }

  /**
   * Parse the list of IP v6 addresses contained in this JPPFSystemInformation instance.<br>
   * This method is provided as a convenience so developers don't have to do the parsing themselves.
   * @return an array on <code>HostIP</code> instances.
   */
  private HostIP[] parseIPV6Addresses()
  {
    String s = getNetwork().getString("ipv6.addresses");
    if ((s == null) || "".equals(s.trim())) return null;
    return parseAddresses(s);
  }

  /**
   * Parse a list of addresses.
   * @param addresses a string containing a space-separated list of host_name|ip_address pairs.
   * @return an array on <code>HostIP</code> instances.
   */
  private HostIP[] parseAddresses(final String addresses)
  {
    String[] pairs = addresses.split("\\s");
    if ((pairs == null) || (pairs.length <= 0)) return null;
    HostIP[] result = new HostIP[pairs.length];
    int count = 0;
    for (String pair: pairs)
    {
      String[] comps = pair.split("|");
      if ((comps[0] != null) && "".equals(comps[0].trim())) comps[0] = null;
      result[count++] = new HostIP(comps[0], comps[1]);
    }
    return result;
  }

  /**
   * Instances of this class represent a hostname / ip address pair.
   */
  public static class HostIP extends Pair<String, String>
  {
    /**
     * Initialize this HostIP object with the specified host name and IP address.
     * @param first the host name.
     * @param second the corresponding IP address.
     */
    public HostIP(final String first, final String second)
    {
      super(first, second);
    }

    /**
     * Get the host name.
     * @return the name as a string.
     */
    public String hostName()
    {
      return first();
    }

    /**
     * Get the ip address.
     * @return the ip address as a string.
     */
    public String ipAddress()
    {
      return second();
    }
  }

  /**
   * Get the properties object holding the uuid.
   * @return a <code>TypedProperties</code> wrapper for the uuid of the corresponding JPPF component.
   */
  public TypedProperties getUuid()
  {
    return getProperties("uuid");
  }

  /**
   * Get all the properties as an array.
   * @return an array of all the sets of properties.
   */
  @Override
  public TypedProperties[] getPropertiesArray()
  {
    synchronized(map)
    {
      return map.values().toArray(new TypedProperties[map.size()]);
    }
  }

  @Override
  public void addProperties(final String key, final TypedProperties properties)
  {
    synchronized(map)
    {
      map.put(key, properties);
    }
  }

  @Override
  public TypedProperties getProperties(final String key)
  {
    synchronized(map)
    {
      return map.get(key);
    }
  }
}
