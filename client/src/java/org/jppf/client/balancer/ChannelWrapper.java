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

package org.jppf.client.balancer;

import org.jppf.client.JPPFClientConnectionStatus;
import org.jppf.client.event.ClientConnectionStatusListener;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.server.scheduler.bundle.Bundler;
import org.jppf.server.scheduler.bundle.NodeAwareness;

/**
 * Context associated with a channel serving state and tasks submission.
 * @param <T> type of task bundle.
 * @author Laurent Cohen
 * @author Martin JANDA
 */
public abstract class ChannelWrapper<T>
{
  /**
   * The task bundle to send or receive.
   */
  private ClientJob bundle = null;
  /**
   * Bundler used to schedule tasks for the corresponding node.
   */
  private Bundler bundler = null;
  /**
   * Represents the system information.
   */
  private JPPFSystemInformation systemInfo = null;
  /**
   * Represents the management information.
   */
  private JPPFManagementInfo managementInfo = null;

  /**
   * Get the unique identifier of the client.
   * @return the uuid as a string.
   */
  public abstract String getUuid();

  /**
   * Get the unique ID for the connection.
   * @return the connection id.
   */
  public abstract String getConnectionUuid();

  /**
   * Get the status of this connection.
   * @return a <code>JPPFClientConnectionStatus</code> enumerated value.
   */
  public abstract JPPFClientConnectionStatus getStatus();

  /**
   * Add a connection status listener to this connection's list of listeners.
   * @param listener the listener to add to the list.
   */
  public abstract void addClientConnectionStatusListener(final ClientConnectionStatusListener listener);

  /**
   * Remove a connection status listener from this connection's list of listeners.
   * @param listener the listener to remove from the list.
   */
  public abstract void removeClientConnectionStatusListener(final ClientConnectionStatusListener listener);

  /**
   * Get the task bundle to send or receive.
   * @return a <code>ClientJob</code> instance.
   */
  public ClientJob getBundle()
  {
    return bundle;
  }

  /**
   * Set the task bundle to send or receive.
   * @param bundle a {@link ClientJob} instance.
   */
  public void setBundle(final ClientJob bundle)
  {
    this.bundle = bundle;
  }

  /**
   * Get the bundler used to schedule tasks for the corresponding node.
   * @return a {@link Bundler} instance.
   */
  public Bundler getBundler()
  {
    return bundler;
  }

  /**
   * Set the bundler used to schedule tasks for the corresponding node.
   * @param bundler a {@link Bundler} instance.
   */
  public void setBundler(final Bundler bundler)
  {
    this.bundler = bundler;
  }

  /**
   * Check whether the bundler held by this context is up to date by comparison
   * with the specified bundler.<br>
   * If it is not, then it is replaced with a copy of the specified bundler, with a
   * timestamp taken at creation time.
   * @param serverBundler the bundler to compare with.
   * @return true if the bundler is up to date, false if it wasn't and has been updated.
   */
  public boolean checkBundler(final Bundler serverBundler)
  {
    if (serverBundler == null) throw new IllegalArgumentException("serverBundler is null");

    if (this.bundler == null || this.bundler.getTimestamp() < serverBundler.getTimestamp())
    {
      if (this.bundler != null) this.bundler.dispose();
      this.bundler = serverBundler.copy();
      this.bundler.setup();
      if (this.bundler instanceof NodeAwareness) ((NodeAwareness) this.bundler).setNodeConfiguration(systemInfo);
      return true;
    }
    return false;
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
   * Set the system information.
   * @param systemInfo a {@link JPPFSystemInformation} instance.
   */
  public void setSystemInfo(final JPPFSystemInformation systemInfo)
  {
    this.systemInfo = systemInfo;
  }

  /**
   * Get the management information.
   * @return a {@link JPPFManagementInfo} instance.
   */
  public JPPFManagementInfo getManagementInfo()
  {
    return managementInfo;
  }

  /**
   * Set the management information.
   * @param managementInfo a {@link JPPFManagementInfo} instance.
   */
  public void setManagementInfo(final JPPFManagementInfo managementInfo)
  {
    this.managementInfo = managementInfo;
  }

  /**
   * Submit bundle for execution on corresponding node.
   * @param bundle a {@link ClientJob} instance.
   */
  public abstract void submit(final ClientJob bundle);
}
