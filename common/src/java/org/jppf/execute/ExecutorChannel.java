/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.execute;

import java.util.concurrent.Future;

import org.jppf.load.balancer.*;
import org.jppf.load.balancer.spi.JPPFBundlerFactory;
import org.jppf.management.*;

/**
 * Execution context associated with a channel serving its state.
 * @param <T> type of task bundle.
 * @author Martin JANDA
 */
public interface ExecutorChannel<T> extends AutoCloseable {
  /**
   * Get the unique identifier of the client.
   * @return the uuid as a string.
   */
  String getUuid();

  /**
   * Get the unique ID for the connection.
   * @return the connection id.
   */
  String getConnectionUuid();

  /**
   * Get the bundler used to schedule tasks for the corresponding node.
   * @return a {@link Bundler} instance.
   */
  Bundler<?> getBundler();

  /**
   * Check whether the bundler held by this context is up to date by comparison
   * with the specified bundler.<br>
   * If it is not, then it is replaced with a copy of the specified bundler, with a
   * timestamp taken at creation time.
   * @param serverBundler the bundler to compare with.
   * @param jppfContext execution context.
   * @return true if the bundler is up to date, false if it wasn't and has been updated.
   */
  //boolean checkBundler(final Bundler serverBundler, final JPPFContext jppfContext);

  /**
   * Check whether the bundler held by this context is up to date by comparison
   * with the specified bundler.<br>
   * If it is not, then it is replaced with a copy of the specified bundler, with a
   * timestamp taken at creation time.
   * @param bundlerFactory the load-balancer factory.
   * @param jppfContext execution context.
   * @return true if the bundler is up to date, false if it wasn't and has been updated.
   */
  boolean checkBundler(final JPPFBundlerFactory bundlerFactory, final JPPFContext jppfContext);

  /**
   * Get the system information.
   * @return a {@link JPPFSystemInformation} instance.
   */
  JPPFSystemInformation getSystemInformation();

  /**
   * Get the management information.
   * @return a {@link JPPFManagementInfo} instance.
   */
  JPPFManagementInfo getManagementInfo();

  /**
   * Submit bundle for execution on corresponding node.
   * @param bundle the task bundle to submit.
   * @return a {@link JPPFFuture}.
   * @throws Exception if any error occurs.
   */
  Future<?> submit(final T bundle) throws Exception;

  /**
   * Determine whether this channel is local (for an in-JVM node).
   * @return <code>true</code> if the channel is local, <code>false</code> otherwise.
   */
  boolean isLocal();

  /**
   * Get the execution status of this channel.
   * @return a <code>ExecutorStatus</code> enumerated value.
   */
  ExecutorStatus getExecutionStatus();

  /**
   * Add a execution status listener to this channel's list of listeners.
   * @param listener the listener to add to the list.
   */
  void addExecutionStatusListener(final ExecutorChannelStatusListener listener);

  /**
   * Remove a execution status listener from this channel's list of listeners.
   * @param listener the listener to remove from the list.
   */
  void removeExecutionStatusListener(final ExecutorChannelStatusListener listener);

  /**
   * Get the monitor object used for synchronization.
   * @return an <code>Object</code> instance.
   */
  Object getMonitor();

  /**
   * Determine whether this channel is active.
   * @return <code>true</code> if the channel is active, <code>false</code> if it is inactive.
   */
  boolean isActive();
}
