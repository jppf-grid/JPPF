/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import org.jppf.JPPFRuntimeException;
import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.comm.recovery.*;
import org.jppf.comm.socket.SocketWrapper;
import org.jppf.execute.ExecutionManager;
import org.jppf.io.*;
import org.jppf.management.JMXServer;
import org.jppf.node.connection.*;
import org.jppf.node.protocol.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.nodeserver.PeerAttributesHandler;
import org.jppf.server.node.AbstractCommonNode;
import org.jppf.server.protocol.ServerTaskBundleClient;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Instances of this class encapsulate execution nodes.
 * @author Laurent Cohen
 * @author Domingos Creado
 * @author Martin JANDA
 */
class PeerNode extends AbstractCommonNode implements ClientConnectionListener {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(PeerNode.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
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
   * Connection to the recovery server.
   */
  private ClientConnection recoveryConnection = null;
  /**
   * The peer connection information.
   */
  private JPPFConnectionInformation connectionInfo = null;
  /**
   * Specifies whether the connection should be established over SSL/TLS.
   */
  private final boolean secure;

  /**
   * Initialize this peer node with the specified configuration name.
   * @param peerNameBase the name of the peer int he configuration file.
   * @param connectionInfo peer connection information.
   * @param secure specifies whether the connection should be established over SSL/TLS.
   */
  public PeerNode(final String peerNameBase, final JPPFConnectionInformation connectionInfo, final boolean secure) {
    if(peerNameBase == null || peerNameBase.isEmpty()) throw new IllegalArgumentException("peerNameBase is blank");
    if(connectionInfo == null) throw new IllegalArgumentException(peerNameBase +" connectionInfo is null");
    this.secure = secure;
    this.peerNameBase = peerNameBase;
    this.uuid = driver.getUuid();
    this.systemInformation = driver.getSystemInformation();
    this.connectionInfo = connectionInfo;
  }

  /**
   * Main processing loop of this node.
   */
  @Override
  public void run() {
    stopped = false;
    if (debugEnabled) log.debug(getName() + " start of peer node main loop");
    while (!isStopped()) {
      try {
        init();
      } catch(Exception e) {
        if (debugEnabled) log.debug(getName() + " : " + e.getMessage(), e);
        stopNode();
      }
      if (!isStopped()) {
        try {
          resultSender = new PeerNodeResultSender(getSocketWrapper());
          perform();
        } catch(Exception e) {
          /*
          if (debugEnabled) log.debug(e.getMessage(), e);
          else log.warn(ExceptionUtils.getMessage(e));
          */
          close();
          throw new JPPFRuntimeException(e);
        } catch(Error e) {
          log.error(e.getMessage(), e);
          e.printStackTrace();
          throw e;
        }
      }
    }
    if (debugEnabled) log.debug(getName() + " end of peer node main loop");
  }

  /**
   * Perform the main execution loop for this node. At each iteration, this method listens for a task to execute,
   * receives it, executes it and sends the results back.
   * @throws Exception if an error was raised from the underlying socket connection or the class loader.
   */
  public void perform() throws Exception {
    if (debugEnabled) log.debug(getName() + " start of peer node secondary loop");
    try {
      while (!stopped) {
        ServerTaskBundleClient bundleWrapper = readBundle();
        TaskBundle bundle = bundleWrapper.getJob();
        if (bundle.isHandshake()) {
          if (JPPFConfiguration.get(JPPFProperties.MANAGEMENT_ENABLED)) setupBundleParameters(bundle);
          bundle.setUuid(uuid);
          bundle.setParameter(BundleParameter.IS_PEER, true);
          bundle.setParameter(BundleParameter.NODE_UUID_PARAM, uuid);
          JMXServer jmxServer = driver.getInitializer().getJmxServer(secure);
          bundle.setParameter(BundleParameter.NODE_MANAGEMENT_PORT_PARAM, jmxServer.getManagementPort());
          PeerAttributesHandler peerHandler = driver.getNodeNioServer().getPeerHandler();
          systemInformation.getJppf().setInt(PeerAttributesHandler.PEER_TOTAL_THREADS, peerHandler.getTotalThreads());
          systemInformation.getJppf().setInt(PeerAttributesHandler.PEER_TOTAL_NODES, peerHandler.getTotalNodes());
          if (debugEnabled) log.debug("sending totalNodes={}, totalThreads={}", peerHandler.getTotalNodes(), peerHandler.getTotalThreads());
          bundle.setParameter(BundleParameter.SYSTEM_INFO_PARAM, systemInformation);
        }
        if (bundleWrapper.getTaskCount() > 0) {
          bundle.getUuidPath().add(driver.getUuid());
          if (debugEnabled) log.debug("uuid path=" + bundle.getUuidPath().getList());
          bundleWrapper.addCompletionListener(resultSender);
          resultSender.bundle = bundleWrapper;
          JPPFDriver.getQueue().addBundle(bundleWrapper);
          resultSender.waitForExecution();
          resultSender.sendResults(bundleWrapper);
          setTaskCount(getTaskCount() + bundleWrapper.getTaskCount());
          if (debugEnabled) log.debug(getName() + " tasks executed: " + getTaskCount());
        } else {
          resultSender.bundle = bundleWrapper;
          resultSender.sendResults(bundleWrapper);
        }
        if (!bundle.isHandshake()) bundleWrapper.bundleEnded();
      }
    } finally {
      if (debugEnabled) log.debug(getName() + " end of peer node secondary loop");
    }
  }

  /**
   * Initialize this node's resources.
   * @throws Exception if an error is raised during initialization.
   */
  public synchronized void init() throws Exception {
    this.nodeConnection = new RemotePeerConnection(peerNameBase, connectionInfo, secure);
    nodeConnection.init();
    is = new SocketWrapperInputSource(getSocketWrapper());
    if (JPPFConfiguration.get(JPPFProperties.RECOVERY_ENABLED)) {
      if (recoveryConnection == null) {
        if (debugEnabled) log.debug("Initializing recovery");
        DriverConnectionInfo driverConnectionInfo = JPPFDriverConnectionInfo.fromJPPFConnectionInformation(connectionInfo, secure, true);
        recoveryConnection = new ClientConnection(uuid, driverConnectionInfo.getHost(), driverConnectionInfo.getRecoveryPort());
        recoveryConnection.addClientConnectionListener(this);
        new Thread(recoveryConnection, getName() + "reaper client connection").start();
      }
    }
  }

  /**
   * Perform the deserialization of the objects received through the socket connection.
   * @return an array of deserialized objects.
   * @throws Exception if an error occurs while deserializing.
   */
  private ServerTaskBundleClient readBundle() throws Exception {
    // Read the request header - with task count information
    if (debugEnabled) log.debug("waiting for next request");
    //TaskBundle header = (TaskBundle) IOHelper.unwrappedData(getSocketWrapper(), ((RemotePeerConnection) nodeConnection).helper.getSerializer());
    TaskBundle header = (TaskBundle) IOHelper.unwrappedData(getSocketWrapper(), JPPFDriver.getSerializer());
    int count = header.getTaskCount();
    if (debugEnabled) log.debug(getName() + " received header from peer driver: " + header + " with " + count + " tasks");

    DataLocation dataProvider = IOHelper.readData(is);
    if (traceEnabled) log.trace(getName() + " received data provider from peer driver, data length = " + dataProvider.getSize());

    List<DataLocation> tasks = new ArrayList<>(count);
    for (int i=1; i<count+1; i++) {
      DataLocation dl = IOHelper.readData(is);
      tasks.add(dl);
      if (traceEnabled) log.trace(getName() + " received task #"+ i + " from peer driver, data length = " + dl.getSize());
    }
    return new ServerTaskBundleClient(header, dataProvider, tasks);
  }

  /**
   * Get the underlying socket connection wrapper.
   * @return a {@link SocketWrapper} instance.
   */
  private SocketWrapper getSocketWrapper() {
    return ((RemotePeerConnection) nodeConnection).getChannel();
  }

  /**
   * Stop this node and release the resources it is using.
   */
  @Override
  public void stopNode() {
    if (debugEnabled) log.debug(getName() + " stopping peer node");
    this.setStopped(true);
    close();
    //driver.getInitializer().getPeerDiscoveryThread().removeConnectionInformation(connectionInfo);
  }

  /**
   * Stop this node and release the resources it is using.
   */
  public void close() {
    if (debugEnabled) log.debug(getName() + " closing peer node");
    try {
      if (debugEnabled) log.debug(getName() + " closing socket: " + nodeConnection.getChannel());
      nodeConnection.close();
    } catch(Exception ex) {
      log.error(ex.getMessage(), ex);
    }
    nodeConnection = null;
    if (recoveryConnection != null) {
      ClientConnection tmp = recoveryConnection;
      if (tmp != null) {
        recoveryConnection = null;
        tmp.close();
      }
    }
  }

  /**
   * Get a string representation of this peer node's name.
   * @return the name as a string.
   */
  private String getName() {
    return peerNameBase;
  }

  @Override
  public JMXServer getJmxServer() throws Exception {
    return driver.getInitializer().getJmxServer(((RemotePeerConnection) nodeConnection).secure);
  }

  @Override
  public boolean isLocal() {
    return false;
  }

  @Override
  public boolean isOffline() {
    return false;
  }

  @Override
  public ExecutionManager getExecutionManager() {
    return null;
  }

  @Override
  public void clientConnectionFailed(final ClientConnectionEvent event) {
    if (debugEnabled) log.debug("recovery connection failed, attempting to reconnect this node");
    close();
  }

  @Override
  public boolean isMasterNode() {
    return false;
  }

  @Override
  public boolean isSlaveNode() {
    return false;
  }

  @Override
  public boolean isDotnetCapable() {
    return false;
  }
}
