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
package org.jppf.client;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.client.event.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.JPPFUuid;
import org.jppf.utils.TypedProperties;
import org.slf4j.*;

/**
 * This class provides an API to submit execution requests and administration commands,
 * and request server information data.<br>
 * It has its own unique identifier, used by the nodes, to determine whether classes from
 * the submitting application should be dynamically reloaded or not, depending on whether
 * the uuid has changed or not.
 * @author Laurent Cohen
 */
public abstract class AbstractJPPFClient implements ClientConnectionStatusListener
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractJPPFClient.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Total count of the tasks submitted by this client.
   */
  protected int totalTaskCount = 0;
  /**
   * Contains all the connections pools in ascending priority order.
   */
  private final Map<Integer, ClientPool> pools = new TreeMap<Integer, ClientPool>(new DescendingIntegerComparator());
  /**
   * Unique universal identifier for this JPPF client.
   */
  protected String uuid = null;
  /**
   * A list of all the connections initially created.
   */
  private final List<JPPFClientConnection> allConnections = new LinkedList<JPPFClientConnection>();
  /**
   * List of listeners to this JPPF client.
   */
  private final List<ClientListener> listeners = new ArrayList<ClientListener>();
  /**
   * Determines whether this JPPF client is closed.
   * @exclude
   */
  protected final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Initialize this client with a specified application UUID.
   * @param uuid the unique identifier for this local client.
   */
  protected AbstractJPPFClient(final String uuid)
  {
    this.uuid = (uuid == null) ? new JPPFUuid().toString() : uuid;
    if (debugEnabled) log.debug("Instantiating JPPF client with uuid=" + this.uuid);
  }

  /**
   * Read all client connection information from the configuration and initialize
   * the connection pools accordingly.
   * @param config The JPPF configuration properties.
   */
  protected abstract void initPools(final TypedProperties config);

  /**
   * Get all the client connections handled by this JPPFClient.
   * @return a list of <code>JPPFClientConnection</code> instances.
   */
  public List<JPPFClientConnection> getAllConnections()
  {
    synchronized (pools)
    {
      return Collections.unmodifiableList(new ArrayList<JPPFClientConnection>(allConnections));
    }
  }

  /**
   * Get count of all client connections handled by this JPPFClient.
   * @return count of <code>JPPFClientConnection</code> instances.
   */
  protected int getAllConnectionsCount()
  {
    synchronized (pools)
    {
      return allConnections.size();
    }
  }

  /**
   * Get the names of all the client connections handled by this JPPFClient.
   * @return a list of connection names as strings.
   */
  public List<String> getAllConnectionNames()
  {
    List<String> names;
    synchronized (pools)
    {
      names = new LinkedList<String>();
      for (JPPFClientConnection c : allConnections) names.add(c.getName());
    }
    return names;
  }

  /**
   * Get a connection given its name.
   * @param name the name of the connection to find.
   * @return a <code>JPPFClientConnection</code> with the highest possible priority.
   */
  public JPPFClientConnection getClientConnection(final String name)
  {
    synchronized (pools)
    {
      for (JPPFClientConnection c : allConnections)
      {
        if (c.getName().equals(name)) return c;
      }
    }
    return null;
  }

  /**
   * Get an available connection with the highest possible priority.
   * @return a <code>JPPFClientConnection</code> with the highest possible priority.
   */
  public JPPFClientConnection getClientConnection()
  {
    return getClientConnection(true, true);
  }

  /**
   * Get an available connection with the highest possible priority.
   * @param oneAttempt determines whether this method should wait until a connection
   * becomes available (ACTIVE status) or fail immediately if no available connection is found.<br>
   * This enables the execution to be performed locally if the client is not connected to a server.
   * @return a <code>JPPFClientConnection</code> with the highest possible priority.
   */
  public JPPFClientConnection getClientConnection(final boolean oneAttempt)
  {
    return getClientConnection(oneAttempt, false);
  }

  /**
   * Get an available connection with the highest possible priority.
   * @param oneAttempt determines whether this method should wait until a connection
   * becomes available (ACTIVE status) or fail immediately if no available connection is found.<br>
   * This enables the execution to be performed locally if the client is not connected to a server.
   * @param anyState specifies whether this method should look for an active connection or not care about the connection state.
   * @return a <code>JPPFClientConnection</code> with the highest possible priority.
   */
  public JPPFClientConnection getClientConnection(final boolean oneAttempt, final boolean anyState)
  {
    JPPFClientConnection connection = null;
    synchronized(pools)
    {
      while ((connection == null) && !pools.isEmpty())
      {
        Iterator<Map.Entry<Integer, ClientPool>> poolIterator = pools.entrySet().iterator();
        while ((connection == null) && poolIterator.hasNext())
        {
          Map.Entry<Integer, ClientPool> entry = poolIterator.next();
          ClientPool pool = entry.getValue();
          int count = 0;
          while ((connection == null) && (count < pool.size()))
          {
            JPPFClientConnection c = pool.nextClient();
            if (c == null) break;
            switch (c.getStatus())
            {
              case ACTIVE:
                connection = c;
                break;
              case FAILED:
                pool.remove(c);
                if (pool.isEmpty()) poolIterator.remove();
                break;
              default:
                if (anyState) connection = c;
                break;
            }
            count++;
          }
        }
        if (pools.isEmpty()) log.warn("No more driver connection available for this client");
        if (oneAttempt) break;
      }
    }
    if (debugEnabled && (connection != null)) log.debug("found client connection \"" + connection + '\"');
    return connection;
  }

  /**
   * Initialize this client's security credentials.
   * @throws Exception if an error is raised during initialization.
   */
  public void initCredentials() throws Exception
  {
  }

  /**
   * Submit a JPPFJob for execution.
   * @param job the job to execute.
   * @return the results of the tasks' execution, as a list of <code>JPPFTask</code> instances for a blocking job, or null if the job is non-blocking.
   * @throws Exception if an error occurs while sending the job for execution.
   */
  public abstract List<JPPFTask> submit(JPPFJob job) throws Exception;

  /**
   * Invoked when the status of a client connection has changed.
   * @param event the event to notify of.
   * @see org.jppf.client.event.ClientConnectionStatusListener#statusChanged(org.jppf.client.event.ClientConnectionStatusEvent)
   */
  @Override
  public void statusChanged(final ClientConnectionStatusEvent event)
  {
    JPPFClientConnection c = (JPPFClientConnection) event.getClientConnectionStatusHandler();
    if (c.getStatus() == JPPFClientConnectionStatus.FAILED) connectionFailed(c);
  }

  /**
   * Invoked when the status of a connection has changed to <code>JPPFClientConnectionStatus.FAILED</code>.
   * @param c the connection that failed.
   */
  protected abstract void connectionFailed(final JPPFClientConnection c);

  /**
   * Add a new connection to the set of connections handled by this client.
   * @param connection the connection to add.
   */
  public void addClientConnection(final JPPFClientConnection connection)
  {
    if (connection == null) throw new IllegalArgumentException("connection is null");
    //connection.addClientConnectionStatusListener(this);
    //connection.setStatus(JPPFClientConnectionStatus.NEW);
    int priority = connection.getPriority();
    synchronized (pools)
    {
      ClientPool pool = pools.get(priority);
      if (pool == null)
      {
        pool = new ClientPool(priority);
        pools.put(priority, pool);
      }
      pool.add(connection);
      allConnections.add(connection);
    }
  }

  /**
   * Remove a connection from the set of connections handled by this client.
   * @param connection the connection to remove.
   */
  protected void removeClientConnection(final JPPFClientConnection connection)
  {
    if (connection == null) throw new IllegalArgumentException("connection is null");

    connection.removeClientConnectionStatusListener(this);
    int priority = connection.getPriority();
    synchronized (pools)
    {
      ClientPool pool = pools.get(priority);
      boolean emptyPools = false;
      if (pool != null)
      {
        pool.remove(connection);
        if (pool.isEmpty()) pools.remove(priority);
        allConnections.remove(connection);
        //if (pools.isEmpty()) throw new JPPFError("FATAL ERROR: No more driver connection available for this client");
        if (pools.isEmpty())
        {
          log.error("FATAL ERROR: No more driver connection available for this client");
          emptyPools = true;
        }
      }
    }
  }

  /**
   * Close this client and release all the resources it is using.
   */
  public void close()
  {
    List<JPPFClientConnection> list = getAllConnections();
    if (debugEnabled) log.debug("closing all connections: " + list);
    for (JPPFClientConnection c : list)
    {
      try
      {
        c.close();
      }
      catch (Exception e)
      {
        log.error(e.getMessage(), e);
      }
    }
  }

  /**
   * Add a listener to the list of listeners to this client.
   * @param listener the listener to add.
   */
  public void addClientListener(final ClientListener listener)
  {
    synchronized (listeners)
    {
      listeners.add(listener);
    }
  }

  /**
   * Remove a listener from the list of listeners to this client.
   * @param listener the listener to remove.
   */
  public void removeClientListener(final ClientListener listener)
  {
    synchronized (listeners)
    {
      listeners.remove(listener);
    }
  }

  /**
   * Notify all listeners to this client that a connection failed.
   * @param c the connection that triggered the event.
   */
  protected void fireConnectionFailed(final JPPFClientConnection c)
  {
    ClientEvent event = new ClientEvent(c);
    synchronized (listeners)
    {
      for (ClientListener listener : listeners)
      {
        listener.connectionFailed(event);
      }
    }
  }

  /**
   * Notify all listeners to this client that a new connection was added.
   * @param c the connection that was added.
   */
  protected void fireNewConnection(final JPPFClientConnection c)
  {
    ClientEvent event = new ClientEvent(c);
    synchronized (listeners)
    {
      for (ClientListener listener : listeners)
      {
        listener.newConnection(event);
      }
    }
  }

  /**
   * Notify all listeners that a new connection was created.
   * @param c the connection that was created.
   */
  public void newConnection(final JPPFClientConnection c)
  {
    fireNewConnection(c);
  }

  /**
   * Get the unique universal identifier for this JPPF client.
   * @return the uuid as a string.
   */
  public String getUuid()
  {
    return uuid;
  }

  /**
   * Determine whether this JPPF client is closed.
   * @return <code>true</code> if this client is closed, <code>false</code> otherwise.
   */
  public boolean isClosed()
  {
    return closed.get();
  }

  /**
   * This comparator defines a descending value order for integers.
   */
  static class DescendingIntegerComparator implements Comparator<Integer>, Serializable
  {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Compare two integers. This comparator defines a descending order for integers.
     * @param o1 first integer to compare.
     * @param o2 second integer to compare.
     * @return -1 if o1 > o2, 0 if o1 == o2, 1 if o1 < o2
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(final Integer o1, final Integer o2)
    {
      if (o1 < o2) return 1;
      if (o1 > o2) return -1;
      return 0;
    }
  }
}
