/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

import org.jppf.JPPFException;
import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.comm.socket.SocketClient;
import org.jppf.io.*;
import org.jppf.management.*;
import org.jppf.node.AbstractNode;
import org.jppf.server.*;
import org.jppf.server.protocol.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class encapsulate execution nodes.
 * @author Laurent Cohen
 * @author Domingos Creado
 */
class PeerNode extends AbstractNode
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(PeerNode.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Used to send the task results back to the requester.
   */
  private AbstractResultSender resultSender = null;
  /**
   * The name of the peer in the configuration file.
   */
  private final String peerName;
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
   * Initialize this peer node with the specified configuration name.
   * @param peerName the name of the peer int he configuration file.
   * @param connectionInfo peer connection information.
   */
  public PeerNode(final String peerName, final JPPFConnectionInformation connectionInfo)
  {
    if(peerName == null || peerName.isEmpty()) throw new IllegalArgumentException("peerName is blank");
    if(connectionInfo == null) throw new IllegalArgumentException("connectionInfo is null");

    this.peerName = peerName;
    this.connectionInfo = connectionInfo;
    //this.uuid = new JPPFUuid().toString();
    this.uuid = driver.getUuid();
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
          log.error(e.getMessage(), e);
          try
          {
            socketClient.close();
            socketClient = null;
          }
          catch(Exception ex)
          {
            log.error(ex.getMessage(), ex);
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
      BundleWrapper bundleWrapper = readBundle();
      JPPFTaskBundle bundle = (JPPFTaskBundle) bundleWrapper.getJob();
      if (JPPFTaskBundle.State.INITIAL_BUNDLE.equals(bundle.getState()))
      {
        if (JPPFConfiguration.getProperties().getBoolean("jppf.management.enabled", true))
        {
          try
          {
            JMXServer jmxServer = driver.getInitializer().getJmxServer();
            TypedProperties props = JPPFConfiguration.getProperties();
            //bundle.setParameter(BundleParameter.NODE_MANAGEMENT_HOST_PARAM, NetworkUtils.getManagementHost());
            bundle.setParameter(BundleParameter.NODE_MANAGEMENT_HOST_PARAM, jmxServer.getManagementHost());
            //bundle.setParameter(BundleParameter.NODE_MANAGEMENT_PORT_PARAM, props.getInt("jppf.management.port", 11198));
            bundle.setParameter(BundleParameter.NODE_MANAGEMENT_PORT_PARAM, jmxServer.getManagementPort());
            bundle.setParameter(BundleParameter.NODE_MANAGEMENT_ID_PARAM, jmxServer.getId());
          }
          catch(Exception e)
          {
            log.error(e.getMessage(), e);
          }
        }

        bundle.setBundleUuid(uuid);
        bundle.setParameter(BundleParameter.IS_PEER, true);
        bundle.setParameter(BundleParameter.NODE_UUID_PARAM, uuid);
        JPPFSystemInformation sysInfo = new JPPFSystemInformation(uuid);
        sysInfo.populate();
        bundle.setParameter(BundleParameter.NODE_SYSTEM_INFO_PARAM, sysInfo);
      }
      //boolean notEmpty = (bundle.getTasks() != null) && (bundle.getTaskCount() > 0);
      boolean notEmpty = !bundleWrapper.getTasks().isEmpty();
      if (notEmpty)
      {
        int n = bundle.getTaskCount();
        bundle.getUuidPath().add(driver.getUuid());
        bundle.setCompletionListener(resultSender);
        JPPFDriver.getQueue().addBundle(bundleWrapper);
        resultSender.run(n);
        //resultSender.sendPartialResults(bundleWrapper);
        setTaskCount(getTaskCount() + n);
        if (debugEnabled) log.debug(getName() + "tasks executed: " + getTaskCount());
      }
      else
      {
        resultSender.sendPartialResults(bundleWrapper);
      }
      if (!JPPFTaskBundle.State.INITIAL_BUNDLE.equals(bundle.getState()))
        driver.getJobManager().jobEnded(bundleWrapper);
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
    initCredentials();
    if (mustInit)
    {
      if (debugEnabled) log.debug(getName() + "initializing socket");
      System.out.println(getName() + "Attempting connection to the peer node server");
      socketInitializer.initializeSocket(socketClient);
      if (!socketInitializer.isSuccessful()) throw new JPPFException(getName() + " : Unable to reconnect to peer server");
      System.out.println(getName() + "Reconnected to the peer node server");
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
    int port = connectionInfo.serverPorts[0];
    socketClient = new SocketClient();
    socketClient.setHost(host);
    socketClient.setPort(port);
    socketClient.setSerializer(helper.getSerializer());
  }

  /**
   * Initialize the security credentials associated with this JPPF node.
   */
  private void initCredentials()
  {
  }

  /**
   * Perform the deserialization of the objects received through the socket connection.
   * @return an array of deserialized objects.
   * @throws Exception if an error occurs while deserializing.
   */
  private BundleWrapper readBundle() throws Exception
  {
    // Read the request header - with tasks count information
    JPPFTaskBundle header = (JPPFTaskBundle) helper.getSerializer().deserialize(socketClient.receiveBytes(0).getBuffer());
    if (debugEnabled) log.debug("received header from peer driver");
    BundleWrapper headerWrapper = new BundleWrapper(header);

    int count = header.getTaskCount();
    if (debugEnabled) log.debug("Received " + count + " tasks");

    for (int i=0; i<count+1; i++)
    {
      DataLocation dl = IOHelper.readData(is);
      if (i == 0)
      {
        headerWrapper.setDataProvider(dl);
        if (debugEnabled) log.debug("received data provider from peer driver, data length = " + dl.getSize());
      }
      else
      {
        headerWrapper.addTask(dl);
        if (debugEnabled) log.debug("received task #"+ i + " from peer driver, data length = " + dl.getSize());
      }
    }
    return headerWrapper;
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
}
