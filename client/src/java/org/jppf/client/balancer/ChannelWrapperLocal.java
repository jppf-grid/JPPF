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

import org.jppf.JPPFException;
import org.jppf.client.JPPFClientConnectionStatus;
import org.jppf.client.balancer.execution.LocalExecutionManager;
import org.jppf.client.event.ClientConnectionStatusEvent;
import org.jppf.client.event.ClientConnectionStatusHandler;
import org.jppf.client.event.ClientConnectionStatusListener;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.management.JPPFSystemInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Context associated with a local channel serving state and tasks submission.
 * @author Martin JANDA
 */
public class ChannelWrapperLocal extends ChannelWrapper implements ClientConnectionStatusHandler
{
  /**
   * The task execution manager for this wrapper.
   */
  private final LocalExecutionManager executionManager = new LocalExecutionManager();
  /**
   * Status of the connection.
   */
  private JPPFClientConnectionStatus status = JPPFClientConnectionStatus.ACTIVE;
  /**
   * Executor for submitting bundles for processing.
   */
  private final Executor executor = Executors.newSingleThreadExecutor();
  /**
   * Unique ID for the connection.
   */
  private final String connectionUuid = UUID.randomUUID().toString();
  /**
   * List of status listeners for this connection.
   */
  private final List<ClientConnectionStatusListener> listeners = new ArrayList<ClientConnectionStatusListener>();

  /**
   * Default initializer for local channel wrapper.
   */
  public ChannelWrapperLocal()
  {
    JPPFSystemInformation info = new JPPFSystemInformation(getConnectionUuid());
    info.populate();

    JPPFManagementInfo managementInfo = new JPPFManagementInfo("local", -1, getConnectionUuid(), JPPFManagementInfo.NODE);
    managementInfo.setSystemInfo(info);
    setSystemInfo(info);
    setManagementInfo(managementInfo);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getConnectionUuid()
  {
    return connectionUuid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JPPFClientConnectionStatus getStatus()
  {
    return status;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setStatus(final JPPFClientConnectionStatus status)
  {
    JPPFClientConnectionStatus oldValue = this.status;
    this.status = status;
    fireStatusChanged(oldValue, this.status);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addClientConnectionStatusListener(final ClientConnectionStatusListener listener)
  {
    synchronized (listeners) {
      listeners.add(listener);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeClientConnectionStatusListener(final ClientConnectionStatusListener listener)
  {
    synchronized (listeners) {
      listeners.remove(listener);
    }
  }

  /**
   * Notify all listeners that the status of this connection has changed.
   * @param oldStatus the connection status before the change.
   * @param newStatus the connection status after the change.
   */
  protected void fireStatusChanged(final JPPFClientConnectionStatus oldStatus, final JPPFClientConnectionStatus newStatus) {
    if(oldStatus == newStatus) return;
    ClientConnectionStatusListener[] temp;
    synchronized (listeners) {
      temp = listeners.toArray(new ClientConnectionStatusListener[listeners.size()]);
    }
    ClientConnectionStatusEvent event = new ClientConnectionStatusEvent(this, oldStatus);
    for (ClientConnectionStatusListener listener : temp)
    {
      listener.statusChanged(event);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void submit(final ClientJob bundle)
  {
    setStatus(JPPFClientConnectionStatus.EXECUTING);
    executor.execute(new LocalRunnable(bundle));
  }

  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("ChannelWrapperLocal");
    sb.append("{status=").append(status);
    sb.append(", connectionUuid='").append(connectionUuid).append('\'');
    sb.append('}');
    return sb.toString();
  }

  /**
   *
   */
  private class LocalRunnable implements Runnable
  {
    /**
     * The task bundle to execute.
     */
  private final ClientJob bundle;
    /**
     * Initialize this runnable for local execution.
     * @param bundle the execution to perform.
     */
    public LocalRunnable(final ClientJob bundle)
    {
      this.bundle = bundle;
    }

    @Override
    public void run()
    {
      try
      {
        executionManager.execute(bundle.getJob(), bundle.getTasks());
      }
      catch (Throwable t)
      {
//        log.error(t.getMessage(), t);
        Exception exception = (t instanceof Exception) ? (Exception) t : new JPPFException(t);
        t.printStackTrace();
      } finally {
        setStatus(JPPFClientConnectionStatus.ACTIVE);
      }
    }
  }
}
