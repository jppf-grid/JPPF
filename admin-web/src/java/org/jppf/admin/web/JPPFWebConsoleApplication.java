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

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.wicket.*;
import org.apache.wicket.page.*;
import org.apache.wicket.pageStore.*;
import org.apache.wicket.pageStore.memory.*;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.*;
import org.jppf.admin.web.tabletree.TableTreeData;
import org.jppf.admin.web.topology.TopologyTree;
import org.jppf.client.monitoring.topology.TopologyManager;
import org.jppf.ui.treetable.TreeViewType;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class JPPFWebConsoleApplication extends WebApplication {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(JPPFWebConsoleApplication.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Base name for localization bundle lookups.
   */
  protected static String BASE = "";
  /**
   *
   */
  static Map<Long, SessionData> sessionDataMap = new ConcurrentHashMap<>();
  /**
   * The topololgy manager.
   */
  private transient TopologyManager topologyManager;

  /**
   * Default constructor.
   */
  public JPPFWebConsoleApplication() {
    if (debugEnabled) log.debug("in JPPFWebConsoleApplication<init>()");
    setConfigurationType(RuntimeConfigurationType.DEPLOYMENT);
  }

  @Override
  public Class<? extends Page> getHomePage() {
    return TopologyTree.class;
  }

  @Override
  protected void init() {
    super.init();
    this.setPageManagerProvider(new MyPageManagerProvider(this));
    log.info("max size per session = {}", getStoreSettings().getMaxSizePerSession());
    this.topologyManager = new TopologyManager();
  }

  /**
   *
   * @return the topology manager.
   */
  public TopologyManager getTopologyManager() {
    return topologyManager;
  }


  /**
   * Get a localized message given its unique name and the current locale.
   * @param message - the unique name of the localized message.
   * @return a message in the current locale, or the default locale
   * if the localization for the current locale is not found.
   */
  public String localize(final String message) {
    return LocalizationUtils.getLocalized(BASE, message);
  }

  @Override
  public Session newSession(final Request request, final Response response) {
    SessionData sessionData = new SessionData();
    sessionDataMap.put(sessionData.getId(), sessionData);
    log.info("created sessiondata with id={}", sessionData.getId());
    return new JPPFWebSession(request, sessionData.getId());
  }

  /**
   * Remove the session data with the specified id.
   * @param id the id of the session data to remove.
   */
  static void removeSessionData(final long id) {
    SessionData sessionData = sessionDataMap.remove(id);
    if (sessionData != null) {
      for (TreeViewType type: TreeViewType.values()) {
        TableTreeData ttd = sessionData.getData(type);
        if (ttd != null) ttd.cleanup();
      }
    }
  }

  /**
   * Add the specified session data with the specified id.
   * @param id the id of the session data to remove.
   * @param data the data to add.
   */
  static void setSessionData(final long id, final SessionData data) {
    if (data != null) {
      sessionDataMap.put(id, data);
    }
  }

  /**
   * @param id the id of the session data to lookup
   * @return the {@link SessionData} instance for the specified id.
   */
  static SessionData getSessionData(final long id) {
    return sessionDataMap.get(id);
  }

  /**
   * Shall not save.
   */
  private static final class MyPageManagerProvider extends DefaultPageManagerProvider {
    /**
     *
     * @param application the wicket application.
     */
    private MyPageManagerProvider(final Application application) {
      super(application);
    }

    @Override
    protected IDataStore newDataStore() {
      // keep everything in memory
      return new HttpSessionDataStore(new DefaultPageManagerContext(), new IDataStoreEvictionStrategy() {
        @Override
        public void evict(final PageTable pageTable) {
        }
      });
    }

    @Override
    protected IPageStore newPageStore(final IDataStore dataStore) {
      return new NullPageStore();
    }
  }

  /**
   *
   */
  private static class NullPageStore implements IPageStore {
    @Override
    public void destroy() {
    }

    @Override
    public IManageablePage getPage(final String sessionId, final int pageId) {
      return null;
    }

    @Override
    public void removePage(final String sessionId, final int pageId) {
    }

    @Override
    public void storePage(final String sessionId, final IManageablePage page) {
    }

    @Override
    public void unbind(final String sessionId) {
    }

    @Override
    public Serializable prepareForSerialization(final String sessionId, final Serializable page) {
      return null;
    }

    @Override
    public Object restoreAfterSerialization(final Serializable serializable) {
      return null;
    }

    @Override
    public IManageablePage convertToPage(final Object page) {
      return null;
    }
  }
}
