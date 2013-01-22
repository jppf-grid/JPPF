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

package org.jppf.management.forwarding;

import java.util.*;

import org.jppf.management.NodeSelector;

/**
 * This class provides helper methods that help implementing the selection of JMX notifications
 * within a notification filter that wraps the user defined one (including null filters).
 * @author Laurent Cohen
 * @exclude
 */
public final class NodeForwardingHelper
{
  /**
   * Singleton instance of this class.
   */
  private static final NodeForwardingHelper instance = new NodeForwardingHelper();
  /**
   * A mapping of register listener ids to the associated node selector and MBean name.
   */
  private Map<String, NotificationListenerWrapper> listenerWrappers = new Hashtable<String, NotificationListenerWrapper>();
  /**
   * The object which implements the node selection logic, based on a {@link NodeSelector}.
   */
  private NodeSelectionProvider selectionProvider = null;

  /**
   * This class can only be instantiated here.
   */
  private NodeForwardingHelper()
  {
  }

  /**
   * Get the singleton instance of this class.
   * @return a {@link NodeForwardingHelper} object.
   */
  static NodeForwardingHelper getInstance()
  {
    return instance;
  }

  /**
   * Get the listener wrapper with the specified id.
   * @param listenerID the id of the lisener wrapper to find.
   * @return a {@link NotificationListenerWrapper} object or <code>null</code> if there is no listener wrapper with this id.
   */
  NotificationListenerWrapper getListener(final String listenerID)
  {
    return listenerWrappers.get(listenerID);
  }

  /**
   * Get the listener wrapper with the specified id.
   * @param listenerID the id opf the lisener wrapper to find.
   * @param listenerWrapper a {@link NotificationListenerWrapper} object.
   */
  void setListener(final String listenerID, final NotificationListenerWrapper listenerWrapper)
  {
    listenerWrappers.put(listenerID, listenerWrapper);
  }

  /**
   * Remove the listener wrapper with the specified id.
   * @param listenerID the id of the lisener wrapper to remove.
   * @return the tremoved {@link NotificationListenerWrapper} object or <code>null</code> if there is no listener wrapper with this id.
   */
  NotificationListenerWrapper removeListener(final String listenerID)
  {
    return listenerWrappers.remove(listenerID);
  }

  /**
   * Get the list of all listenrs.
   * @return an {@link ArrayList} of {@link NotificationListenerWrapper} instances.
   */
  Collection<NotificationListenerWrapper> allListeners()
  {
    return new ArrayList<NotificationListenerWrapper>(listenerWrappers.values());
  }

  /**
   * Set the selection provider onto this helper.
   * @param selectionProvider the selction provider to set.
   */
  void setSelectionProvider(final NodeSelectionProvider selectionProvider)
  {
    this.selectionProvider = selectionProvider;
  }

  /**
   * Determine whether the specified selector accepts the specified node.
   * @param nodeUuid the uuid of the node to check.
   * @param selector the node selector used as a filter.
   * @return a set of {@link AbstractNodeContext} instances.
   */
  boolean isNodeAccepted(final String nodeUuid, final NodeSelector selector)
  {
    return (selectionProvider != null) && selectionProvider.isNodeAccepted(nodeUuid, selector);
  }
}
