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

package org.jppf.admin.web;

import java.util.EnumMap;

import org.apache.wicket.Session;
import org.apache.wicket.request.Request;
import org.jppf.admin.web.health.HealthTreeData;
import org.jppf.admin.web.jobs.JobsTreeData;
import org.jppf.admin.web.settings.UserSettings;
import org.jppf.admin.web.tabletree.TableTreeData;
import org.jppf.admin.web.topology.TopologyTreeData;
import org.jppf.ui.treetable.TreeViewType;
import org.wicketstuff.wicket.servlet3.auth.ServletContainerAuthenticatedWebSession;

/**
 * The Wession class. It handles container-based authentication and holds the data used to
 * render the views in the web console.
 * @author Laurent Cohen
 */
public class JPPFWebSession extends ServletContainerAuthenticatedWebSession {
  /**
   * Holds the data for each type of table tree view.
   */
  private transient EnumMap<TreeViewType, TableTreeData> dataMap = new EnumMap<>(TreeViewType.class);
  /**
   * The user settings.
   */
  private UserSettings userSettings;

  /**
   * Initialize a new session.
   * @param request the request.
   */
  public JPPFWebSession(final Request request) {
    super(request);
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
      userSettings = new UserSettings(user).load();
      getHealthData().initThresholds(userSettings.getProperties());
    }
    return ret;
  }

  /**
   * @return the user settings, or {@code null} if the user is not authenticated.
   */
  public UserSettings getUserSettings() {
    return userSettings;
  }
}
