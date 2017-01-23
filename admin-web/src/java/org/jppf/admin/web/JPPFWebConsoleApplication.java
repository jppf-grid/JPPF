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
import java.util.*;

import org.apache.wicket.*;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.page.*;
import org.apache.wicket.pageStore.*;
import org.apache.wicket.pageStore.memory.*;
import org.jppf.admin.web.admin.*;
import org.jppf.admin.web.auth.LoginPage;
import org.jppf.admin.web.settings.*;
import org.jppf.admin.web.stats.StatsUpdater;
import org.jppf.admin.web.topology.TopologyPage;
import org.jppf.client.JPPFClient;
import org.jppf.client.monitoring.jobs.*;
import org.jppf.client.monitoring.topology.TopologyManager;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;
import org.wicketstuff.wicket.servlet3.auth.*;

/**
 * This is the Wicket {@link Application} class for the JPPF web console.
 * @author Laurent Cohen
 */
public class JPPFWebConsoleApplication extends ServletContainerAuthenticatedWebApplication {
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
   * The topololgy manager.
   */
  private transient TopologyManager topologyManager;
  /**
   * The topololgy manager.
   */
  private transient JobMonitor jobMonitor;
  /**
   * Mapping of configurations to their type.
   */
  private final Map<ConfigType, ConfigurationHandler> configMap = new EnumMap<>(ConfigType.class);
  /**
   * Updates the statistics from all drivers.
   */
  private StatsUpdater statsUpdater;
  /**
   * The persistence for this web application.
   */
  private Persistence persistence;

  /**
   * Default constructor.
   */
  public JPPFWebConsoleApplication() {
    setConfigurationType(RuntimeConfigurationType.DEPLOYMENT);
  }

  @Override
  protected void init() {
    super.init();
    String name = getInitParameter("jppfPersistenceClassName");
    if (debugEnabled) log.debug("read persistence class name '{}' from init parameter", name);
    if (name == null) {
      name = JPPFAsyncFilePersistence.class.getName();
      if (debugEnabled) log.debug("using default persistence class name '{}'", name);
    }
    persistence = PersistenceFactory.newPersistence(name);
    if (debugEnabled) log.debug("in JPPFWebConsoleApplication.init()");
    configMap.put(ConfigType.CLIENT, new ConfigurationHandler(ConfigType.CLIENT) {
      @Override
      public synchronized ConfigurationHandler load() {
        ConfigurationHandler handler = super.load();
        getProperties().set(JPPFProperties.SSL_CONFIGURATION_SOURCE, SSLConfigSource.class.getName()).remove(JPPFProperties.SSL_CONFIGURATION_FILE);
        return handler;
      }

      @Override
      public synchronized ConfigurationHandler save() {
        getProperties().set(JPPFProperties.SSL_CONFIGURATION_SOURCE, SSLConfigSource.class.getName()).remove(JPPFProperties.SSL_CONFIGURATION_FILE);
        return super.save();
      }
    });
    configMap.put(ConfigType.SSL, new ConfigurationHandler(ConfigType.SSL));

    getPageSettings().setVersionPagesByDefault(false);
    this.setPageManagerProvider(new MyPageManagerProvider(this));
    TypedProperties config = getConfig(ConfigType.CLIENT).getProperties();
    JPPFConfiguration.reset(config);
    this.topologyManager = new TopologyManager(config.get(JPPFProperties.ADMIN_REFRESH_INTERVAL_TOPOLOGY), config.get(JPPFProperties.ADMIN_REFRESH_INTERVAL_HEALTH), null, true);
    this.jobMonitor = new JobMonitor(JobMonitorUpdateMode.POLLING, 3000L, topologyManager);
    this.statsUpdater = new StatsUpdater(topologyManager);
  }

  /**
   * @return the topology manager.
   */
  public TopologyManager getTopologyManager() {
    return topologyManager;
  }

  /**
   * @return the job monitor.
   */
  public JobMonitor getJobMonitor() {
    return jobMonitor;
  }

  @Override
  public Class<? extends Page> getHomePage() {
    return TopologyPage.class;
  }


  @Override
  protected Class<? extends ServletContainerAuthenticatedWebSession> getContainerManagedWebSessionClass() {
    return JPPFWebSession.class;
  }

  @Override
  protected Class<? extends WebPage> getSignInPageClass() {
    return LoginPage.class;
  }

  /**
   * @return the current application instance.
   */
  public static JPPFWebConsoleApplication get() {
    return (JPPFWebConsoleApplication) Application.get();
  }

  /**
   * @param type the type of config to get.
   * @return the configuration handler for the specified config type.
   */
  public ConfigurationHandler getConfig(final ConfigType type) {
    return (type == null) ? null : configMap.get(type);
  }

  /**
   * @return the objects which updates the statistics from all drivers.
   */
  public StatsUpdater getStatsUpdater() {
    return statsUpdater;
  }

  /**
   * @return the persistence for this web application.
   */
  public Persistence getPersistence() {
    return persistence;
  }

  @Override
  protected void onDestroy() {
    if (persistence != null) persistence.close();
    JPPFClient client = null;
    if (topologyManager != null) {
      topologyManager.close();
      client = topologyManager.getJPPFClient();
    }
    if (jobMonitor != null) {
      jobMonitor.close();
    }
    if (client != null) client.close();
  }

  /**
   * Does not save to persistent store.
   */
  private static final class MyPageManagerProvider extends DefaultPageManagerProvider {
    /**
     * @param application the wicket application.
     */
    private MyPageManagerProvider(final Application application) { super(application); }

    @Override protected IDataStore newDataStore() {
      // keep everything in memory
      return new HttpSessionDataStore(new DefaultPageManagerContext(), new IDataStoreEvictionStrategy() {
        @Override public void evict(final PageTable pageTable) { }
      });
    }

    @Override protected IPageStore newPageStore(final IDataStore dataStore) { return new NullPageStore(); }
  }

  /**
   * Disables serialization.
   */
  private static class NullPageStore implements IPageStore {
    @Override public void destroy() { }
    @Override public IManageablePage getPage(final String sessionId, final int pageId) { return null; }
    @Override public void removePage(final String sessionId, final int pageId) { }
    @Override public void storePage(final String sessionId, final IManageablePage page) { }
    @Override public void unbind(final String sessionId) { }
    @Override public Serializable prepareForSerialization(final String sessionId, final Serializable page) { return null; }
    @Override public Object restoreAfterSerialization(final Serializable serializable) { return null; }
    @Override public IManageablePage convertToPage(final Object page) { return null; }
  }
}
