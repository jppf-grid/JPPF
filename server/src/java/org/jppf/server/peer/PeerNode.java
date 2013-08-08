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
package org.jppf.server.peer;

import java.util.*;

import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.comm.socket.SocketWrapper;
import org.jppf.io.*;
import org.jppf.management.JMXServer;
import org.jppf.server.JPPFDriver;
import org.jppf.server.node.AbstractCommonNode;
import org.jppf.server.protocol.*;
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
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();
  /**
   * Used to send the task results back to the requester.
   */
  private PeerNodeResultSender resultSender = null;
  /**
   * The name of the peer in the configuration file.
   */
  private final String peerNameBase;
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
   * @param peerNameBase the name of the peer int he configuration file.
   * @param connectionInfo peer connection information.
   * @param secure specifies whether the connection should be established over SSL/TLS.
   */
  public PeerNode(final String peerNameBase, final JPPFConnectionInformation connectionInfo, final boolean secure)
  {
    if(peerNameBase == null || peerNameBase.isEmpty()) throw new IllegalArgumentException("peerNameBase is blank");
    if(connectionInfo == null) throw new IllegalArgumentException("connectionInfo is null");

    this.peerNameBase = peerNameBase;
    this.nodeConnection = new RemotePeerConnection(peerNameBase, connectionInfo, secure);
    this.uuid = driver.getUuid();
    this.systemInformation = driver.getSystemInformation();
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
        if (debugEnabled) log.debug(getName() + " : " + e.getMessage(), e);
        setStopped(true);
        try
        {
          if (nodeConnection != null) nodeConnection.close();
        }
        catch (Exception e2)
        {
          log.error(e.getMessage(), e);
        }
        driver.getInitializer().getPeerDiscoveryThread().removeConnectionInformation(((RemotePeerConnection) nodeConnection).connectionInfo);
      }
      if (!isStopped())
      {
        try
        {
          resultSender = new PeerNodeResultSender(getSocketWrapper());
          perform();
        }
        catch(Exception e)
        {
          if (debugEnabled) log.debug(e.getMessage(), e);
          else log.warn(ExceptionUtils.getMessage(e));
          try
          {
            if (nodeConnection != null) nodeConnection.close();
            nodeConnection = null;
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
      if (bundle.isHandshake())
      {
        if (JPPFConfiguration.getProperties().getBoolean("jppf.management.enabled", true)) setupManagementParameters(bundle);
        bundle.setUuid(uuid);
        bundle.setParameter(BundleParameter.IS_PEER, true);
        bundle.setParameter(BundleParameter.NODE_UUID_PARAM, uuid);
        bundle.setParameter(BundleParameter.SYSTEM_INFO_PARAM, systemInformation);
      }
      if (bundleWrapper.getTaskCount() > 0)
      {
        //bundle.setTaskCount(bundleWrapper.getTaskCount());
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
      if (!bundle.isHandshake())
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
    nodeConnection.init();
    is = new SocketWrapperInputSource(getSocketWrapper());
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
    JPPFTaskBundle header = (JPPFTaskBundle) IOHelper.unwrappedData(getSocketWrapper(), ((RemotePeerConnection) nodeConnection).helper.getSerializer());
    int count = header.getTaskCount();
    if (debugEnabled) log.debug("received header from peer driver: " + header + " with " + count + " tasks");

    DataLocation dataProvider = IOHelper.readData(is);
    if (traceEnabled) log.trace("received data provider from peer driver, data length = " + dataProvider.getSize());

    List<DataLocation> tasks = new ArrayList<>(count);
    for (int i=1; i<count+1; i++)
    {
      DataLocation dl = IOHelper.readData(is);
      tasks.add(dl);
      if (traceEnabled) log.trace("received task #"+ i + " from peer driver, data length = " + dl.getSize());
    }
    return new ServerTaskBundleClient(header, dataProvider, tasks);
  }

  /**
   * Get the underlying socket connection wrapper.
   * @return a {@link SocketWrapper} instance.
   */
  private SocketWrapper getSocketWrapper()
  {
    return ((RemotePeerConnection) nodeConnection).getChannel();
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
      if (debugEnabled) log.debug(getName() + "closing socket: " + nodeConnection.getChannel());
      nodeConnection.close();
    }
    catch(Exception ex)
    {
      log.error(ex.getMessage(), ex);
    }
    nodeConnection = null;
  }

  /**
   * Get a string representation of this peer node's name.
   * @return the name as a string.
   */
  private String getName()
  {
    return ((RemotePeerConnection) nodeConnection).name;
  }

  @Override
  public JMXServer getJmxServer() throws Exception
  {
    return driver.getInitializer().getJmxServer(((RemotePeerConnection) nodeConnection).secure);
  }

  @Override
  public boolean isLocal()
  {
    return false;
  }

  @Override
  public boolean isOffline()
  {
    return false;
  }
}
