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

package org.jppf.client;

import static org.jppf.client.JPPFClientConnectionStatus.*;

import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.jppf.client.event.*;
import org.jppf.comm.socket.SocketInitializer;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.server.protocol.BundleParameter;
import org.jppf.server.protocol.JPPFTaskBundle;
import org.jppf.utils.NetworkUtils;
import org.slf4j.*;

/**
 * This class provides an API to submit execution requests and administration
 * commands, and request server information data.<br>
 * It has its own unique identifier, used by the nodes, to determine whether
 * classes from the submitting application should be dynamically reloaded or not
 * depending on whether the uuid has changed or not.
 * @author Laurent Cohen
 */
public abstract class AbstractJPPFClientConnection extends BaseJPPFClientConnection
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractJPPFClientConnection.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Priority given to the driver this client is connected to. The client is always connected to the available driver(s) with the highest
   * priority. If multiple drivers have the same priority, they will be used as a pool and tasks will be evenly distributed among them.
   */
  protected int priority = 0;
  /**
   * Status of the connection.
   */
  protected AtomicReference<JPPFClientConnectionStatus> status = new AtomicReference<JPPFClientConnectionStatus>(CONNECTING);
  /**
   * List of status listeners for this connection.
   */
  protected final List<ClientConnectionStatusListener> listeners = new LinkedList<ClientConnectionStatusListener>();
  /**
   * Holds the tasks, data provider and submission mode for the current execution.
   */
  protected JPPFJob job = null;
  /**
   * Determines whether this connection has been shut down;
   */
  protected boolean isShutdown = false;
  /**
   * The name displayed for this connection.
   */
  protected String displayName;
  /**
   * Determines whether the communication via the server is done via SSL.
   */
  protected boolean ssl = false;
  /**
   * Represents the system information.
   */
  private JPPFSystemInformation systemInfo = null;

  /**
   * Configure this client connection with the specified parameters.
   * @param uuid the unique identifier for this local client.
   * @param name configuration name for this local client.
   * @param host the name or IP address of the host the JPPF driver is running on.
   * @param driverPort the TCP port the JPPF driver listening to for submitted tasks.
   * @param priority the assigned to this client connection.
   * @param ssl determines whether the communication via the server is done via SSL.
   */
  protected void configure(final String uuid, final String name, final String host, final int driverPort, final int priority, final boolean ssl)
  {
    this.uuid = uuid;
    this.host = NetworkUtils.getHostName(host);
    this.port = driverPort;
    this.priority = priority;
    this.name = name;
    this.ssl = ssl;
    try
    {
      String s = InetAddress.getByName(host).getCanonicalHostName();
      displayName = name + '[' + s + ':' + port + ']';
    }
    catch(UnknownHostException e)
    {
      displayName = name;
    }
    this.taskServerConnection = new TaskServerConnectionHandler(this, this.host, this.port);
  }

  /**
   * Initialize this client connection.
   * @see org.jppf.client.JPPFClientConnection#init()
   */
  @Override
  public abstract void init();

  /**
   * Get the priority assigned to this connection.
   * @return a priority as an int value.
   * @see org.jppf.client.JPPFClientConnection#getPriority()
   */
  @Override
  public int getPriority()
  {
    return priority;
  }

  /**
   * Set the priority assigned to this connection.
   * @param priority a priority as an int value.
   */
  public void setPriority(final int priority)
  {
    this.priority = priority;
  }

  /**
   * Get the status of this connection.
   * @return a <code>JPPFClientConnectionStatus</code> enumerated value.
   * @see org.jppf.client.JPPFClientConnection#getStatus()
   */
  @Override
  public JPPFClientConnectionStatus getStatus()
  {
    return status.get();
  }

  /**
   * Set the status of this connection.
   * @param status  a <code>JPPFClientConnectionStatus</code> enumerated value.
   * @see org.jppf.client.JPPFClientConnection#setStatus(org.jppf.client.JPPFClientConnectionStatus)
   */
  @Override
  public void setStatus(final JPPFClientConnectionStatus status)
  {
    JPPFClientConnectionStatus oldStatus = getStatus();
    this.status.set(status);
    if (!status.equals(oldStatus)) fireStatusChanged(oldStatus);
  }

  /**
   * Add a connection status listener to this connection's list of listeners.
   * @param listener the listener to add to the list.
   * @see org.jppf.client.JPPFClientConnection#addClientConnectionStatusListener(org.jppf.client.event.ClientConnectionStatusListener)
   */
  @Override
  public void addClientConnectionStatusListener(final ClientConnectionStatusListener listener)
  {
    synchronized(listeners)
    {
      listeners.add(listener);
    }
  }

  /**
   * Remove a connection status listener from this connection's list of listeners.
   * @param listener the listener to remove from the list.
   * @see org.jppf.client.JPPFClientConnection#removeClientConnectionStatusListener(org.jppf.client.event.ClientConnectionStatusListener)
   */
  @Override
  public void removeClientConnectionStatusListener(final ClientConnectionStatusListener listener)
  {
    synchronized(listeners)
    {
      listeners.remove(listener);
    }
  }

  /**
   * Notify all listeners that the status of this connection has changed.
   * @param oldStatus the connection status before the change.
   */
  protected void fireStatusChanged(final JPPFClientConnectionStatus oldStatus)
  {
    ClientConnectionStatusEvent event = new ClientConnectionStatusEvent(this, oldStatus);
    ClientConnectionStatusListener[] array = null;
    synchronized(listeners)
    {
      array = listeners.toArray(new ClientConnectionStatusListener[listeners.size()]);
    }
    for (ClientConnectionStatusListener listener: array) listener.statusChanged(event);
  }

  /**
   * Get a string representation of this client connection.
   * @return a string representing this connection.
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    return displayName + " : " + status;
  }

  /**
   * Create a socket initializer.
   * @return an instance of a class implementing <code>SocketInitializer</code>.
   */
  @Override
  protected abstract SocketInitializer createSocketInitializer();

  /**
   * Get the object that holds the tasks, data provider and submission mode for the current execution.
   * @return a <code>JPPFJob</code> instance.
   */
  public JPPFJob getCurrentJob()
  {
    return job;
  }

  /**
   * Set the object that holds the tasks, data provider and submission mode for the current execution.
   * @param currentExecution a <code>ClientExecution</code> instance.
   */
  public void setCurrentJob(final JPPFJob currentExecution)
  {
    this.job = currentExecution;
  }

  /**
   * Invoked to notify of a status change event on a client connection.
   * @param event the event to notify of.
   * @see org.jppf.client.event.ClientConnectionStatusListener#statusChanged(org.jppf.client.event.ClientConnectionStatusEvent)
   */
  public void delegateStatusChanged(final ClientConnectionStatusEvent event)
  {
    JPPFClientConnectionStatus s1 = event.getClientConnectionStatusHandler().getStatus();
    JPPFClientConnectionStatus s2 = taskServerConnection.getStatus();
    processStatusChanged(s1, s2);
  }

  /**
   * Invoked to notify of a status change event on a client connection.
   * @param event the event to notify of.
   * @see org.jppf.client.event.ClientConnectionStatusListener#statusChanged(org.jppf.client.event.ClientConnectionStatusEvent)
   */
  public void taskServerConnectionStatusChanged(final ClientConnectionStatusEvent event)
  {
    JPPFClientConnectionStatus s1 = event.getClientConnectionStatusHandler().getStatus();
    JPPFClientConnectionStatus s2 = delegate.getStatus();
    processStatusChanged(s2, s1);
  }

  /**
   * Handle a status change from either the class server delegate or the task server connection
   * and determine whether it triggers a status change for the client connection.
   * @param delegateStatus status of the class server delegate connection.
   * @param taskConnectionStatus status of the task server connection.
   */
  protected void processStatusChanged(final JPPFClientConnectionStatus delegateStatus, final JPPFClientConnectionStatus taskConnectionStatus)
  {
    if (FAILED.equals(delegateStatus)) setStatus(FAILED);
    else if (ACTIVE.equals(delegateStatus))
    {
      if (ACTIVE.equals(taskConnectionStatus) && !ACTIVE.equals(this.getStatus())) setStatus(ACTIVE);
      else if (!taskConnectionStatus.equals(this.getStatus())) setStatus(taskConnectionStatus);
    }
    else
    {
      if (ACTIVE.equals(taskConnectionStatus)) setStatus(delegateStatus);
      else
      {
        int n = delegateStatus.compareTo(taskConnectionStatus);
        if ((n < 0) && !delegateStatus.equals(this.getStatus())) setStatus(delegateStatus);
        else if (!taskConnectionStatus.equals(this.getStatus())) setStatus(taskConnectionStatus);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSSL()
  {
    return ssl;
  }

  /**
   * Get the system information.
   * @return a {@link JPPFSystemInformation} instance.
   */
  public JPPFSystemInformation getSystemInfo()
  {
    return systemInfo;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JPPFTaskBundle sendHandshakeJob() throws Exception
  {
    JPPFTaskBundle bundle = super.sendHandshakeJob();
    this.systemInfo = (JPPFSystemInformation) bundle.getParameter(BundleParameter.SYSTEM_INFO_PARAM);
    return bundle;
  }
}
