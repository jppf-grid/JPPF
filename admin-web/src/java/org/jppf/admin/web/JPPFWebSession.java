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

import org.apache.wicket.Session;
import org.apache.wicket.request.Request;
import org.jppf.admin.web.jobs.JobsTreeData;
import org.jppf.admin.web.settings.UserSettings;
import org.jppf.admin.web.tabletree.TableTreeData;
import org.jppf.admin.web.topology.TopologyTreeData;
import org.jppf.ui.treetable.TreeViewType;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;
import org.wicketstuff.wicket.servlet3.auth.ServletContainerAuthenticatedWebSession;

/**
 *
 * @author Laurent Cohen
 */
public class JPPFWebSession extends ServletContainerAuthenticatedWebSession {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFWebSession.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The JPPF-internal id for this session data.
   */
  private final long sessionDataId;
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
    SessionData sessionData = new SessionData();
    JPPFWebConsoleApplication.sessionDataMap.put(sessionData.getId(), sessionData);
    log.info("created sessiondata with id={}", sessionData.getId());
    this.sessionDataId = sessionData.getId();
    if (debugEnabled) log.debug(String.format("new instance #%d, request=%s", sessionDataId, request));
  }

  @Override
  public void onInvalidate() {
    super.onInvalidate();
    JPPFWebConsoleApplication.removeSessionData(sessionDataId);
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
    return getSessionData().getData(type);
  }

  /**
   * @return the data elements for the grid topology.
   */
  public TopologyTreeData getTopologyData() {
    return (TopologyTreeData) getSessionData().getData(TreeViewType.TOPOLOGY);
  }

  /**
   * @return the data elements for the jobs view.
   */
  public JobsTreeData getJobsData() {
    return (JobsTreeData) getSessionData().getData(TreeViewType.JOBS);
  }

  /**
   * @return the JPPF-internal id for this session.
   */
  public long getSessionDataId() {
    return sessionDataId;
  }

  /**
   * @return the session data for this session.
   */
  public SessionData getSessionData() {
    SessionData data = JPPFWebConsoleApplication.getSessionData(sessionDataId);
    if (data == null) {
      data = new SessionData(sessionDataId);
      JPPFWebConsoleApplication.setSessionData(sessionDataId, data);
    }
    return data;
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
      userSettings = new UserSettings(user);
      userSettings.load();
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
