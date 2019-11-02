/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.management.forwarding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.*;

import org.jppf.management.*;
import org.jppf.server.JPPFDriver;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.Logger;

/**
 * Implementation of the <code>JPPFNodeForwardingMBean</code> interface.
 * @author Laurent Cohen
 * @exclude
 */
public abstract class AbstractNodeForwarding extends NotificationBroadcasterSupport implements ForwardingNotficationEmitter {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggingUtils.getLogger(AbstractNodeForwarding.class, false);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Used to generate listener IDs.
   */
  final AtomicLong listenerSequence = new AtomicLong(0L);
  /**
   * Reference to the JPPF driver.
   */
  final JPPFDriver driver;
  /**
   * Manages the forwarding of node notifications to the registered clients.
   */
  final ForwardingNotificationManager manager;
  /**
   * Provides an API for selecting nodes based on a {@link NodeSelector}.
   */
  final NodeSelectionHelper selectionHelper;
  /**
   * Number of executor threads.
   */
  final int core;
  /**
   * Use to send management/monitoring requests in parallel with regards to the nodes.
   */
  final ExecutorService executor;

  /**
   * Initialize this MBean implementation.
   * @param driver reference to the JPPF driver.
   */
  public AbstractNodeForwarding(final JPPFDriver driver) {
    this.driver = driver;
    selectionHelper = new NodeSelectionHelper(driver);
    NodeForwardingHelper.getInstance().setSelectionProvider(selectionHelper);
    manager = new ForwardingNotificationManager(this);
    this.core = driver.getConfiguration().get(JPPFProperties.NODE_FORWARDING_POOL_SIZE);
    executor = ConcurrentUtils.newFixedExecutor(core, "NodeForwarding");
    if (debugEnabled) log.debug("initialized JPPFNodeForwarding");
  }

  /**
   * Register a listener with the specified node selector and MBean.
   * @param selector the node slector to apply to the listener.
   * @param mBeanName the name of the node mbeans to receive notifications from.
   * @return a unique id for the listener.
   * @throws IllegalArgumentException if <code>selector</code> or <code>mBeanName</code> is null.
   */
  @Override
  public String registerForwardingNotificationListener(final NodeSelector selector, final String mBeanName) throws IllegalArgumentException {
    if (debugEnabled) log.debug("before registering listener with selector=" + selector + ", mbean=" + mBeanName);
    if (selector == null) throw new IllegalArgumentException("selector cannot be null");
    if (mBeanName == null) throw new IllegalArgumentException("mBeanName cannot be null");
    final String id = StringUtils.build(driver.getUuid(), ':', listenerSequence.incrementAndGet());
    this.manager.addNotificationListener(id, selector, mBeanName);
    if (debugEnabled) log.debug("registered listener id=" + id);
    return id;
  }

  /**
   * Unregister the specified listener.
   * @param listenerID the ID of the listener to unregister.
   * @throws ListenerNotFoundException if the listener could not be found.
   */
  @Override
  public void unregisterForwardingNotificationListener(final String listenerID) throws ListenerNotFoundException {
    if (debugEnabled) log.debug("before unregistering listener id={}", listenerID);
    manager.removeNotificationListener(listenerID);
    if (debugEnabled) log.debug("unregistered listener id={}", listenerID);
  }

  @Override
  public void sendNotification(final Notification notification) {
    if (debugEnabled) log.debug("sending notif: " + notification);
    super.sendNotification(notification);
  }

  /**
   * Get the object that provides an API for selecting nodes based on a {@link NodeSelector}.
   * @return a {@link NodeSelectionHelper} instance.
   */
  NodeSelectionHelper getSelectionHelper() {
    return selectionHelper;
  }
}
