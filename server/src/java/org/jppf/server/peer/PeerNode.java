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
package org.jppf.server.peer;

import java.net.InetAddress;
import java.util.*;

import org.jppf.JPPFException;
import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.comm.socket.SocketClient;
import org.jppf.io.*;
import org.jppf.management.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.node.AbstractCommonNode;
import org.jppf.server.protocol.*;
import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class encapsulate execution nodes.
 * @author Laurent Cohen
 * @author Domingos Creado
 * @author Martin JANDA
 */
class PeerNode extends AbstractCommonNode
{
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(PeerNode.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Used to send the task results back to the requester.
   */
  private PeerNodeResultSender resultSender = null;
  /**
   * The name of the peer in the configuration file.
   */
  private final String peerNameBase;
  /**
   * The name of the peer with host and port information when connected.
   */
  private String peerName;
  /**
   * Peer connection information.
   */
  private final JPPFConnectionInformation connectionInfo;
  /**
   * Input source for the socket client.
   */
  private InputSource is = null;
  /**
   * Reference to the driver.
   */
  private JPPFDriver driver = JPPFDriver.getInstance();
  /**
   * Determines whether communication with remote peer servers should be secure.
   */
  private final boolean secure;

  /**
   * Initialize this peer node with the specified configuration name.
   * @param peerNameBase the name of the peer int he configuration file.
   * @param connectionInfo peer connection information.
   * @param secure specifies whether the connection should be established over SSL/TLS.
   */
  public PeerNode(final String peerNameBase, final JPPFConnectionInformation connectionInfo, final boolean secure)
  {
    if(peerNameBase == null || peerNameBase.isEmpty()) throw new IllegalArgumentException("peerNameBase is blank");
    if(connectionInfo == null) throw new IllegalArgumentException("connectionInfo is null");

    this.peerNameBase = peerNameBase;
    this.peerName = peerNameBase;
    this.connectionInfo = connectionInfo;
    this.secure = secure;
    this.uuid = driver.getUuid();
    this.systemInformation = driver.getSystemInformation();
    this.helper = new SerializationHelperImpl();
  }

  /**
   * Main processing loop of this node.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    stopped = false;
    if (debugEnabled) log.debug(getName() + "Start of peer node main loop");
    while (!isStopped())
    {
      try
      {
        init();
      }
      catch(Exception e)
      {
        setStopped(true);
        if (socketInitializer != null) socketInitializer.close();
        driver.getInitializer().getPeerDiscoveryThread().removeConnectionInformation(connectionInfo);
        if (debugEnabled) log.debug(getName() + " : " + e.getMessage(), e);
      }
      if (!isStopped())
      {
        try
        {
          resultSender = new PeerNodeResultSender(socketClient);
          perform();
        }
        catch(Exception e)
        {
          if (debugEnabled) log.debug(e.getMessage(), e);
          else log.warn(ExceptionUtils.getMessage(e));
          try
          {
            socketClient.close();
            socketClient = null;
            peerName = peerNameBase;
          }
          catch(Exception ex)
          {
            if (debugEnabled) log.debug(e.getMessage(), ex);
            else log.warn(ExceptionUtils.getMessage(ex));
          }
        }
        catch(Error e)
        {
          log.error(e.getMessage(), e);
          e.printStackTrace();
          throw e;
        }
      }
    }
    if (debugEnabled) log.debug(getName() + "End of peer node main loop");
  }

  /**
   * Perform the main execution loop for this node. At each iteration, this method listens for a task to execute,
   * receives it, executes it and sends the results back.
   * @throws Exception if an error was raised from the underlying socket connection or the class loader.
   */
  public void perform() throws Exception
  {
    if (debugEnabled) log.debug(getName() + "Start of peer node secondary loop");
    while (!stopped)
    {
      ServerTaskBundleClient bundleWrapper = readBundle();
      JPPFTaskBundle bundle = bundleWrapper.getJob();
      if (bundle.getState() == JPPFTaskBundle.State.INITIAL_BUNDLE)
      {
        if (JPPFConfiguration.getProperties().getBoolean("jppf.management.enabled", true)) setupManagementParameters(bundle);
        bundle.setUuid(uuid);
        bundle.setParameter(BundleParameter.IS_PEER, true);
        bundle.setParameter(BundleParameter.NODE_UUID_PARAM, uuid);
        bundle.setParameter(BundleParameter.SYSTEM_INFO_PARAM, systemInformation);
      }
      if (bundleWrapper.getTaskCount() > 0)
      {
        bundle.getUuidPath().add(driver.getUuid());
        if (debugEnabled) log.debug("uuid path=" + bundle.getUuidPath().getList());
        bundleWrapper.addCompletionListener(resultSender);
        resultSender.bundle = bundleWrapper;
        JPPFDriver.getQueue().addBundle(bundleWrapper);
        resultSender.waitForExecution();
        resultSender.sendResults(bundleWrapper);
        setTaskCount(getTaskCount() + bundleWrapper.getTaskCount());
        if (debugEnabled) log.debug(getName() + "tasks executed: " + getTaskCount());
      }
      else
      {
        resultSender.bundle = bundleWrapper;
        resultSender.sendResults(bundleWrapper);
      }
      if (bundle.getState() != JPPFTaskBundle.State.INITIAL_BUNDLE)
      {
        bundleWrapper.bundleEnded();
      }
    }
    if (debugEnabled) log.debug(getName() + " End of peer node secondary loop");
  }

  /**
   * Initialize this node's resources.
   * @throws Exception if an error is raised during initialization.
   */
  public synchronized void init() throws Exception
  {
    if (debugEnabled) log.debug(getName() + "] initializing socket client");
    boolean mustInit = false;
    if (socketClient == null)
    {
      mustInit = true;
      initSocketClient();
    }
    if (mustInit)
    {
      if (debugEnabled) log.debug(getName() + "initializing socket");
      System.out.println("Connecting to  " + peerName);
      socketInitializer.initializeSocket(socketClient);
      if (!socketInitializer.isSuccessful()) throw new JPPFException("Unable to reconnect to " + peerName);
      System.out.println("Reconnected to " + peerName);
      if (secure) socketClient = SSLHelper.createSSLClientConnection(socketClient);
      if (debugEnabled) log.debug("sending channel identifier");
      socketClient.writeInt(JPPFIdentifiers.NODE_JOB_DATA_CHANNEL);
      is = new SocketWrapperInputSource(socketClient);
    }
  }

  /**
   * Initialize this node's resources.
   * @throws Exception if an error is raised during initialization.
   */
  public void initSocketClient() throws Exception
  {
    if (debugEnabled) log.debug(getName() + "initializing socket client");
    String host = connectionInfo.host == null || connectionInfo.host.isEmpty() ? "localhost" : connectionInfo.host;
    host = InetAddress.getByName(host).getHostName();
    int port = secure ? connectionInfo.sslServerPorts[0] : connectionInfo.serverPorts[0];
    socketClient = new SocketClient();
    socketClient.setHost(host);
    socketClient.setPort(port);
    socketClient.setSerializer(helper.getSerializer());
    peerName = peerNameBase + '@' + host + ':' + port;
  }

  /**
   * Perform the deserialization of the objects received through the socket connection.
   * @return an array of deserialized objects.
   * @throws Exception if an error occurs while deserializing.
   */
  private ServerTaskBundleClient readBundle() throws Exception
  {
    // Read the request header - with task count information
    if (debugEnabled) log.debug("waiting for next request");
    JPPFTaskBundle header = (JPPFTaskBundle) IOHelper.unwrappedData(socketClient, getHelper().getSerializer());
    if (debugEnabled) log.debug("received header from peer driver: " + header);

    int count = header.getTaskCount();
    if (debugEnabled) log.debug("Received " + count + " tasks");

    DataLocation dataProvider = IOHelper.readData(is);
    if (debugEnabled) log.debug("received data provider from peer driver, data length = " + dataProvider.getSize());

    List<DataLocation> tasks = new ArrayList<DataLocation>(count);
    for (int i=1; i<count+1; i++)
    {
      DataLocation dl = IOHelper.readData(is);
      tasks.add(dl);
      if (debugEnabled) log.debug("received task #"+ i + " from peer driver, data length = " + dl.getSize());
    }
    return new ServerTaskBundleClient(header, dataProvider, tasks);
  }

  /**
   * Stop this node and release the resources it is using.
   * @see org.jppf.node.Node#stopNode()
   */
  @Override
  public void stopNode()
  {
    if (debugEnabled) log.debug(getName() + "closing node");
    stopped = true;
    try
    {
      if (debugEnabled) log.debug(getName() + "closing socket: " + socketClient.getSocket());
      socketClient.close();
    }
    catch(Exception ex)
    {
      log.error(ex.getMessage(), ex);
    }
    socketClient = null;
  }

  /**
   * Get a string representation of this peer node's name.
   * @return the name as a string.
   */
  private String getName()
  {
    return "[peer: " + peerName +"] ";
  }

  @Override
  public JMXServer getJmxServer() throws Exception
  {
    return driver.getInitializer().getJmxServer(secure);
  }

  @Override
  public boolean isLocal()
  {
    return false;
  }
}
