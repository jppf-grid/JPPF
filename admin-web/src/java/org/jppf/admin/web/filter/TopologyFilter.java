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

package org.jppf.admin.web.filter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jppf.admin.web.JPPFWebConsoleApplication;
import org.jppf.admin.web.settings.*;
import org.jppf.node.policy.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class TopologyFilter {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(UserSettings.class);
  /**
   * The user name.
   */
  private final String user;
  /**
   * The user name.
   */
  private final String userHash;
  /**
   * The perisstence handler for these settings.
   */
  private final Persistence persistence;
  /**
   * The XML policy as loaded from the persistent store.
   */
  private String xmlPolicy;
  /**
   * The execution policy used for actual filtering.
   */
  private ExecutionPolicy policy;
  /**
   * Whether the filter is active.
   */
  private boolean active = false;
  /**
   * The list of listeners.
   */
  private final List<TopologyFilterListener> listeners = new CopyOnWriteArrayList<>();

  /**
   * Initialize with the specified user.
   * @param user th euser name.
   */
  public TopologyFilter(final String user) {
    this.user = user;
    this.userHash = CryptoUtils.computeHash(user + "_node_filter", "SHA-256");
    this.persistence = JPPFWebConsoleApplication.get().getPersistenceFactory().newPersistence();
  }

  /**
   * Load the settings with the persistence handler.
   * @return these user settings.
   */
  public synchronized TopologyFilter load() {
    try  {
      xmlPolicy = persistence.loadString(userHash);
      if ((xmlPolicy != null) && !xmlPolicy.isEmpty()) policy = PolicyParser.parsePolicy(xmlPolicy);
    } catch(Exception e) {
      log.error("error loading topology filter for user {} : {}", user, ExceptionUtils.getStackTrace(e));
    }
    return this;
  }

  /**
   * Save the settings  with the persistence handler.
   */
  public synchronized void save() {
    try  {
      persistence.saveString(userHash, xmlPolicy);
    } catch(Exception e) {
      log.error("error saving topology filter for user {} : {}", user, ExceptionUtils.getStackTrace(e));
    }
  }

  /**
   * @return the XML policy as loaded from the persistent store.
   */
  public synchronized String getXmlPolicy() {
    return xmlPolicy;
  }

  /**
   * Set the xml policy.
   * @param xmlPolicy the xml policy as a string.
   */
  public synchronized void setXmlPolicy(final String xmlPolicy) {
    this.xmlPolicy = xmlPolicy;
    try  {
      if ((xmlPolicy != null) && !xmlPolicy.isEmpty()) policy = PolicyParser.parsePolicy(xmlPolicy);
    } catch(Exception e) {
      log.error("error parsing topology filter for user {} : {}", user, ExceptionUtils.getStackTrace(e));
      policy = null;
    }
    save();
    fireChangeEvent();
  }

  /**
   * @return the execution policy used for actual filtering.
   */
  public synchronized ExecutionPolicy getPolicy() {
    return policy;
  }

  /**
   * @return dhether the filter is active.
   */
  public synchronized boolean isActive() {
    return active;
  }

  /**
   * Set the active state of the filter.
   * @param active {@code true} to activate, {@code false} to deactivate.
   */
  public synchronized void setActive(final boolean active) {
    this.active = active;
    fireChangeEvent();
  }

  /**
   * Add a listener to this filter.
   * @param listener the listener to add.
   */
  public void addListener(final TopologyFilterListener listener) {
    if (listener != null) listeners.add(listener);
  }

  /**
   * Remove a listener from this filter.
   * @param listener the listener to remove.
   */
  public void removeListener(final TopologyFilterListener listener) {
    if (listener != null) listeners.remove(listener);
  }

  /**
   * Notify all listeners that a change occurred.
   */
  private void fireChangeEvent() {
    TopologyFilterEvent event = new TopologyFilterEvent(this);
    for (TopologyFilterListener listener: listeners) listener.onFilterChange(event);;
  }
}
