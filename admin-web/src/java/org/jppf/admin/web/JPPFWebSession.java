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

package org.jppf.admin.web;

import java.security.Principal;
import java.util.*;

import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.Session;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.jppf.admin.web.auth.*;
import org.jppf.admin.web.filter.TopologyFilter;
import org.jppf.admin.web.health.HealthTreeData;
import org.jppf.admin.web.jobs.JobsTreeData;
import org.jppf.admin.web.settings.UserSettings;
import org.jppf.admin.web.tabletree.TableTreeData;
import org.jppf.admin.web.topology.TopologyTreeData;
import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.ui.treetable.TreeViewType;
import org.slf4j.*;
import org.wicketstuff.wicket.servlet3.auth.ServletContainerAuthenticatedWebSession;

/**
 * The Wession class. It handles container-based authentication and holds the data used to
 * render the views in the web console.
 * @author Laurent Cohen
 */
public class JPPFWebSession extends ServletContainerAuthenticatedWebSession {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFWebSession.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Name of the settings property for the node filter's active state.
   */
  public static final String NODE_FILTER_ACTIVE_PROP = "node.filter.active";
  /**
   * Holds the data for each type of table tree view.
   */
  private final EnumMap<TreeViewType, TableTreeData> dataMap;
  /**
   * The user settings.
   */
  private UserSettings userSettings;
  /**
   * The currently selected driver.
   */
  private TopologyDriver currentDriver;
  /**
   * The user's node filter.
   */
  private TopologyFilter nodeFilter;
  /**
   * Whether to show IP addresses vs. host names.
   */
  private boolean showIP;

  /**
   * Initialize a new session.
   * @param request the request.
   */
  public JPPFWebSession(final Request request) {
    super(request);
    dataMap = new EnumMap<>(TreeViewType.class);
  }

  @Override
  public void onInvalidate() {
    super.onInvalidate();
    for (TreeViewType type: TreeViewType.values()) {
      TableTreeData ttd = dataMap.get(type);
      if (ttd != null) ttd.cleanup();
    }
    dataMap.clear();
    if (userSettings != null) {
      userSettings.getProperties().clear();
      userSettings = null;
    }
  }

  /**
   * @param type the type of view for which to obtain data.
   * @return the data elements for the specified type of view.
   */
  public TableTreeData getTableTreeData(final TreeViewType type) {
    TableTreeData data = dataMap.get(type);
    if (data == null) {
      switch(type) {
        case TOPOLOGY:
          data = new TopologyTreeData();
          break;

        case HEALTH:
          data = new HealthTreeData();
          break;

        case JOBS:
          data = new JobsTreeData();
          break;
      }
    }
    dataMap.put(type, data);
    return data;
  }

  /**
   * @return the data elements for the grid topology.
   */
  public TopologyTreeData getTopologyData() {
    return (TopologyTreeData) getTableTreeData(TreeViewType.TOPOLOGY);
  }

  /**
   * @return the data elements for the jobs view.
   */
  public JobsTreeData getJobsData() {
    return (JobsTreeData) getTableTreeData(TreeViewType.JOBS);
  }

  /**
   * @return the data elements for the JVM health view.
   */
  public HealthTreeData getHealthData() {
    return (HealthTreeData) getTableTreeData(TreeViewType.HEALTH);
  }

  /**
   * Get the associated JPPF session obect.
   * @return a {@link JPPFWebSession} instance.
   */
  public static JPPFWebSession get() {
    return (JPPFWebSession) Session.get();
  }

  @Override
  public boolean authenticate(final String user, final String pwd) {
    boolean ret = super.authenticate(user, pwd);
    if (ret) {
      List<String> roles = getUserRoles();
      if (debugEnabled) log.debug("successful authentication for user {}, roles = {}", user, roles);
      userSettings = new UserSettings(user).load();
      nodeFilter = new TopologyFilter(getUserName());
      boolean active = userSettings.getProperties().getBoolean(NODE_FILTER_ACTIVE_PROP, false);
      if (debugEnabled) log.debug("node filter is {}", active ? "active" : "inactive");
      nodeFilter.setActive(active);
      if (roles.contains(JPPFRoles.MANAGER) || roles.contains(JPPFRoles.MONITOR)) {
        for (TreeViewType type: TreeViewType.values()) getTableTreeData(type);
      }
      getHealthData().initThresholds(userSettings.getProperties());
    } else if (debugEnabled) log.debug("failed authentication for user {}", user);
    return ret;
  }

  /**
   * @return the user settings, or {@code null} if the user is not authenticated.
   */
  public UserSettings getUserSettings() {
    return userSettings;
  }

  /**
   * @return the currently selected driver.
   */
  public TopologyDriver getCurrentDriver() {
    if (currentDriver == null) currentDriver = JPPFWebConsoleApplication.get().getStatsUpdater().getCurrentDriver();
    return currentDriver;
  }

  /**
   * Set the current driver.
   * @param currentDriver the driver to set.
   */
  public void setCurrentDriver(final TopologyDriver currentDriver) {
    this.currentDriver = currentDriver;
  }


  /**
   * @return the name of the authenticated user, or {@code null} if the user is not authenticated.
   */
  public static String getSignedInUser() {
    HttpServletRequest req = (HttpServletRequest) RequestCycle.get().getRequest().getContainerRequest();
    Principal p = req.getUserPrincipal();
    return p == null ? null : p.getName();
  }

  /**
   *
   * @return the roles of the current signed-in user, if any.
   */
  private List<String> getUserRoles() {
    List<String> result = new ArrayList<>();
    HttpServletRequest req = (HttpServletRequest) RequestCycle.get().getRequest().getContainerRequest();
    for (JPPFRole r: JPPFRole.values()) {
      String role = r.getRoleName();
      if (req.isUserInRole(role)) result.add(role);
    }
    return result;
  }

  /**
   * @return the user's node filter
   */
  public TopologyFilter getNodeFilter() {
    return nodeFilter;
  }

  /**
   * @return whether to show IP addresses vs. host names.
   */
  public boolean isShowIP() {
    return showIP;
  }

  /**
   *
   * @param showIP {@code true} to show IP addresses, {@code false} to show host names.
   */
  public void setShowIP(final boolean showIP) {
    this.showIP = showIP;
  }
}
