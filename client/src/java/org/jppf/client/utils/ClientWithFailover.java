/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.client.utils;

import java.net.*;
import java.util.*;
import java.util.concurrent.locks.*;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.node.policy.*;
import org.jppf.node.protocol.Task;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * A wrapper for a JPPF client which provides connections failover based on their priority.
 * @author Laurent Cohen
 * @deprecated as of JPPF 4.1, the functionality provided by this class is now an integral
 * part of the implementation of {@link JPPFClient} and its accompanying classes. Hence this class
 * is no longer necessary. If it is used in your ocde, it will still work as intended, however
 * you should prepare for the fact that this this class will vbe removed in a future version. 
 */
public class ClientWithFailover implements ClientListener, ClientConnectionStatusListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ClientWithFailover.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The JPPF client to which requests are delegated.
   */
  protected final JPPFClient client;
  /**
   * Sorted map of connections grouped by descending priority.
   * @exclude
   */
  protected final SortedMap<Integer, Set<JPPFClientConnection>> connectionMap = new TreeMap<>();
  /**
   * Synchrnonizes access to the connections map.
   * @exclude
   */
  protected final Lock lock = new ReentrantLock();
  /**
   * This custom policy enforces that jobs are sent only to the working driver connections with the highest priority.
   * @exclude
   */
  protected final ExecutionPolicy failoverPolicy = new FailoverPolicy();

  /**
   * Initialize this client wrapper with the specified array of listeners.
   * @param listeners an array of {@link ClientListener}s to add to the client upon startup.
   * This array may be empty, in which case it is equivalent to invoking a no-args constructor. 
   */
  public ClientWithFailover(final ClientListener...listeners) {
    this(null, listeners);
  }

  /**
   * Initialize this client wrapper with the specified uuid and array of listeners.
   * @param uuid a user-defined uuid assigned to the JPPF client. 
   * @param listeners an array of {@link ClientListener}s to add to the client upon startup.
   * This array may be empty, in which case it is equivalent to invoking constructor with the uuid as its sole argument. 
   */
  public ClientWithFailover(final String uuid, final ClientListener...listeners) {
    List<ClientListener> list = new ArrayList<>();
    list.add(this);
    if ((listeners != null) && (listeners.length > 0)) list.addAll(Arrays.asList(listeners));
    client = new JPPFClient(uuid, list.toArray(new ClientListener[list.size()]));
    if (debugEnabled) log.debug("client initialized");
  }

  /**
   * Submit the specified job.
   * @param job the job to submit.
   * @return a list of jppf tasks or null if the job is non-blocking.
   * @throws Exception if any error occurs.*
   * @deprecated use {@link #submitJob(JPPFJob)} instead.
   */
  @Deprecated
  public List<JPPFTask> submit(final JPPFJob job) throws Exception {
    ExecutionPolicy policy = job.getClientSLA().getExecutionPolicy();
    // if an execution policy is already set, we join it with the failover policy instead of replacing it
    if (policy != null) job.getClientSLA().setExecutionPolicy(failoverPolicy.and(policy));
    else job.getClientSLA().setExecutionPolicy(failoverPolicy);
    return client.submit(job);
  }

  /**
   * Submit the specified job.
   * @param job the job to submit.
   * @return a list of jppf tasks or null if the job is non-blocking.
   * @throws Exception if any error occurs.
   */
  public List<Task<?>> submitJob(final JPPFJob job) throws Exception {
    ExecutionPolicy policy = job.getClientSLA().getExecutionPolicy();
    // if an execution policy is already set, we join it with the failover policy instead of replacing it
    if (policy != null) job.getClientSLA().setExecutionPolicy(failoverPolicy.and(policy));
    else job.getClientSLA().setExecutionPolicy(failoverPolicy);
    return client.submitJob(job);
  }

  /**
   * Cancel the job with the specified uuid.
   * @param uuid the uuid of the job to cancel.
   * @return <code>true</code> if the cancel succeeded, <code>false</code> if it failed or if a cacel has already been requested for the job.
   * @throws Exception if any error occurs.
   */
  public boolean cancelJob(final String uuid) throws Exception {
    return client.cancelJob(uuid);
  }

  /**
   * Close the underlying client and free its resources.
   */
  public void  close() {
    if (client != null) client.close();
  }

  /**
   * Get the JPPF client to which requests are delegated.
   * @return a {@link JPPFClient} instance.
   */
  public JPPFClient getClient() {
    return client;
  }

  @Override
  public void newConnection(final ClientEvent event) {
    JPPFClientConnection c = event.getConnection();
    if (debugEnabled) log.debug("New connection with name '" + c.getName() + "' priority=" + c.getPriority());
    c.addClientConnectionStatusListener(this);
  }

  @Override
  public void connectionFailed(final ClientEvent event) {
    JPPFClientConnection c = event.getConnection();
    if (debugEnabled) log.debug("Connection " + c.getName() + " has failed");
    c.removeClientConnectionStatusListener(this);
  }

  @Override
  public void statusChanged(final ClientConnectionStatusEvent event) {
    JPPFClientConnection c = (JPPFClientConnection) event.getClientConnectionStatusHandler();
    if (debugEnabled) log.debug("Connection '" + c.getName() + "' priority=" + c.getPriority() + " status changed from " +  event.getOldStatus() + " to " + event.getSource());
    boolean oldStatusWorking = isWorkingStatus(event.getOldStatus());
    boolean newStatusWorking = isWorkingStatus(c.getStatus());
    // the connection is broken
    if (oldStatusWorking && !newStatusWorking) removeConnection(c);
    // the connection is (re-)established
    else if (!oldStatusWorking && newStatusWorking) addConnection(c);
  }

  /**
   * Add a working connection.
   * @param c the connection to add.
   */
  private void addConnection(final JPPFClientConnection c) {
    int p = c.getPriority();
    lock.lock();
    try {
      Set<JPPFClientConnection> set = connectionMap.get(p);
      if (set == null) {
        set = new HashSet<>();
        connectionMap.put(p, set);
      }
      set.add(c);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Remove a broken connection.
   * @param c the connection to remove.
   */
  private void removeConnection(final JPPFClientConnection c) {
    int priority = c.getPriority();
    lock.lock();
    try {
      Set<JPPFClientConnection> set = connectionMap.get(priority);
      if (set != null) {
        set.remove(c);
        if (set.isEmpty()) connectionMap.remove(priority);
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Determine whether the specified status is a working one.
   * @param status the status to check.
   * @return <code>true</code> if the status indicates the connection is working, <code>false</code> otherwise.
   */
  private boolean isWorkingStatus(final JPPFClientConnectionStatus status) {
    return (status == JPPFClientConnectionStatus.ACTIVE) || (status == JPPFClientConnectionStatus.EXECUTING);
  }

  /**
   * This custom policy enforces that jobs are sent only to the working driver connections with the highest priority.
   * @exclude
   */
  public class FailoverPolicy extends CustomPolicy {
    /**
     * Constructor for this custom policy.
     * @param args the arguments for this policy, not used in this implementation.
     */
    public FailoverPolicy(final String...args) {
    }

    @Override
    public boolean accepts(final PropertiesCollection info) {
      Set<JPPFClientConnection> set = null;
      lock.lock();
      try {
        if (connectionMap.isEmpty()) return false;
        int priority =  connectionMap.lastKey();
        // we create a new hash set to ensure we need the lock for the minimum possible time
        set = new HashSet<>(connectionMap.get(priority));
      } finally {
        lock.unlock();
      }
      JPPFSystemInformation inf = (JPPFSystemInformation) info;
      String ipv4 = inf.getNetwork().getString("ipv4.addresses");
      String ipv6 = inf.getNetwork().getString("ipv6.addresses");
      int port = inf.getJppf().getInt("jppf.server.port");
      boolean accepted = false;
      for (JPPFClientConnection c: set) {
        // we attempt to resolve to the ip address
        String ip = c.getHost();
        try {
          InetAddress addr = InetAddress.getByName(c.getHost());
          ip = addr.getHostAddress();
        } catch (UnknownHostException e) {
          if (debugEnabled) log.debug(e.getMessage(), e);
          else log.warn(ExceptionUtils.getMessage(e));
        }
        accepted = accepted || ((ipv4.indexOf(ip) >= 0) || (ipv6.indexOf(ip) >= 0)) && (port == c.getPort());
        if (accepted) return true;
      }
      return false;
    }
  }
}
