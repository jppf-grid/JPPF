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

package org.jppf.comm.discovery;

import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * An instance of this class listens to messages broadcast by the driver
 * to get the driver connection host and ports.
 * @author Laurent Cohen
 */
public class JPPFMulticastReceiver extends ThreadSynchronization
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFMulticastReceiver.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether trace-level logging is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Reference to the JPPF configuration.
   */
  private static TypedProperties config = JPPFConfiguration.getProperties();
  /**
   * Multicast group to join.
   */
  private	String group = "230.0.0.1";
  /**
   * Multicast port to listen to.
   */
  private	int port = 11111;
  /**
   * Timeout for UDP socket read operations.
   */
  private	int timeout = 5000;
  /**
   * Multicast group to join.
   */
  private InetAddress groupInetAddress = null;
  /**
   * List of retrieved connection information.
   */
  private LinkedList<JPPFConnectionInformation> infoList = new LinkedList<>();
  /**
   * Count of connection information objects used for ordering.
   */
  private AtomicLong count = new AtomicLong(0L);
  /**
   * Handles include and exclude IP filters.
   */
  private IPFilter ipFilter = null;
  /**
   * 
   */
  private Receiver[] receivers = null; 

  /**
   * Default constructor.
   */
  public JPPFMulticastReceiver()
  {
    this(null);
  }

  /**
   * Default constructor.
   * @param ipFilter handles include and exclude IP filters.
   */
  public JPPFMulticastReceiver(final IPFilter ipFilter)
  {
    group = config.getString("jppf.discovery.group", "230.0.0.1");
    port = config.getInt("jppf.discovery.port", 11111);
    timeout = config.getInt("jppf.discovery.timeout", 5000);
    this.ipFilter = ipFilter;
  }

  /**
   * Initialize this discovery thread with the specified UDP group, UDP port and timeout.
   * @param group the multicast group to join.
   * @param port the multicast port to listen to.
   * @param timeout the timeout for UDP socket read operations.
   */
  public JPPFMulticastReceiver(final String group, final int port, final int timeout)
  {
    this.group = group;
    this.port = port;
    this.timeout = timeout;
  }

  /**
   * Retrieve the driver connection information broadcast by the driver.
   * @return a <code>DriverConnectionInformation</code> instance, or null
   * if no broadcast information could be retrieved.
   */
  public synchronized JPPFConnectionInformation receive()
  {
    JPPFConnectionInformation info = null;
    try
    {
      if (groupInetAddress == null)
      {
        groupInetAddress = InetAddress.getByName(group);
        //List<InetAddress> addresses = NetworkUtils.getNonLocalIPV4Addresses();
        List<InetAddress> addresses = NetworkUtils.getNonLocalIPAddresses();
        if (addresses.isEmpty()) addresses.add(InetAddress.getByName("127.0.0.1"));
        int len = addresses.size();
        if (debugEnabled)
        {
          StringBuilder sb = new StringBuilder();
          sb.append("Found ").append(len).append(" address");
          if (len > 1) sb.append("es");
          sb.append(':');
          for (InetAddress addr: addresses) sb.append(' ').append(addr.getHostAddress());
          log.debug(sb.toString());
        }
        receivers = new Receiver[len];
        for (int i=0; i<len; i++) receivers[i] = new Receiver(addresses.get(i), port);
        for (Receiver r: receivers) r.start();
      }
      if (!hasConnectionInfo()) wait(timeout);
      else wait(50);
      info = getMostRecent();
    }
    catch(Exception e)
    {
      if (!(e instanceof InterruptedException)) log.error(e.getMessage(), e);
    }
    if (traceEnabled) log.trace("Auto-discovery of the driver connection information: " + info);
    return info;
  }

  /**
   * Add the specified connection information to the set of retrieved ones.
   * If the connection information already exists, nothing is added.
   * @param info the connection information to add.
   */
  private synchronized void addConnectionInfo(final JPPFConnectionInformation info)
  {
    try
    {
      if ((ipFilter != null) && !ipFilter.isAddressAccepted(InetAddress.getByName(info.host))) return;
    }
    catch (UnknownHostException e)
    {
      return;
    }
    infoList.remove(info);
    infoList.addFirst(info);
    if (debugEnabled) log.debug("nb connections: " + infoList.size());
    notifyAll();
  }

  /**
   * Get the most recently received connection information.
   * @return a <code>JPPFConnectionInformation</code> instance, or null if none was received.
   */
  private synchronized JPPFConnectionInformation getMostRecent()
  {
    if (infoList.isEmpty()) return null;
    return infoList.getFirst();
  }

  /**
   * Determine whether at least one connection information object has been retrieved.
   * @return true if at least one connection information object has been retrieved, false otherwise.
   */
  private synchronized boolean hasConnectionInfo()
  {
    return !infoList.isEmpty();
  }

  /**
   * Instances of this class attempt to receive broadcast connection information
   * from a multicast socket bound to a specified address.
   */
  public class Receiver extends Thread
  {
    /**
     * Address the multicast socket is bound to.
     */
    private InetAddress addr = null;
    /**
     * Port the multicast socket listens to.
     */
    private int port = 0;
    /**
     * Connection information retrieved by the multicast socket.
     */
    private JPPFConnectionInformation info = null;

    /**
     * Initialize this Receiver with the specified address and port.
     * @param addr address the multicast socket is bound to.
     * @param port port the multicast socket listens to.
     */
    public Receiver(final InetAddress addr, final int port)
    {
      super("Receiver@" + addr.getHostAddress() + ':' + port);
      this.addr = addr;
      this.port = port;
    }

    /**
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
      MulticastSocket socket = null;
      try
      {
        int t = 1000;
        //socket = new MulticastSocket(new InetSocketAddress(addr, port));
        socket = new MulticastSocket(port);
        socket.setInterface(addr);
        socket.joinGroup(getGroupInetAddress());
        socket.setSoTimeout(timeout);
        byte[] buf = new byte[512];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        while (!isStopped())
        {
          long start = System.currentTimeMillis();
          while (System.currentTimeMillis() - start < t)
          {
            try
            {
              socket.receive(packet);
              if (isStopped()) break;
              ByteBuffer buffer = ByteBuffer.wrap(buf);
              int len = buffer.getInt();
              byte[] bytes = new byte[len];
              buffer.get(bytes);
              info = JPPFConnectionInformation.fromBytes(bytes);
              String host = config.getString("jppf.management.host", null);
              if (host == null) host = addr.getHostAddress();
              info.managementHost = host;
              addConnectionInfo(info);
            }
            catch(SocketTimeoutException e)
            {
              //if (debugEnabled) log.debug(e.getMessage(), e);
            }
            if (System.currentTimeMillis() - start < t) Thread.sleep(50L);
          }
        }
        socket.leaveGroup(getGroupInetAddress());
      }
      catch(Exception e)
      {
        if (!(e instanceof InterruptedException)) log.error(e.getMessage(), e);
      }
      if (socket != null) socket.close();
    }

    /**
     * Get the connection information retrieved by the multicast socket.
     * @return a <code>JPPFConnectionInformation</code> instance or null if no information could be retrieved.
     */
    public JPPFConnectionInformation getInfo()
    {
      return info;
    }
  }

  /**
   * Get the multicast group to join.
   * @return an {@link InetAddress} instance.
   */
  private synchronized InetAddress getGroupInetAddress()
  {
    return groupInetAddress;
  }

  /**
   * Set the multicast group to join.
   * @param groupInetAddress an {@link InetAddress} instance.
   */
  private synchronized void setGroupInetAddress(final InetAddress groupInetAddress)
  {
    this.groupInetAddress = groupInetAddress;
  }

  @Override
  public synchronized void setStopped(final boolean stopped)
  {
    if (stopped && (receivers != null))
    {
      for (Receiver r: receivers) r.interrupt();
    }
    super.setStopped(stopped);
  }
}
