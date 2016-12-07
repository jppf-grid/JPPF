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
import org.jppf.admin.web.stats.StatsUpdater;
import org.jppf.admin.web.topology.TopologyPage;
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
  private final Map<PanelType, ConfigurationHandler> configMap = new EnumMap<>(PanelType.class);
  /**
   * Updates the statistics from all drivers.
   */
  private StatsUpdater statsUpdater;

  /**
   * Default constructor.
   */
  public JPPFWebConsoleApplication() {
    if (debugEnabled) log.debug("in JPPFWebConsoleApplication<init>()");
    setConfigurationType(RuntimeConfigurationType.DEPLOYMENT);
    configMap.put(PanelType.CLIENT, new ConfigurationHandler(PanelType.CLIENT) {
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
    configMap.put(PanelType.SSL, new ConfigurationHandler(PanelType.SSL));
  }

  @Override
  public Class<? extends Page> getHomePage() {
    return TopologyPage.class;
  }

  @Override
  protected void init() {
    super.init();
    getPageSettings().setVersionPagesByDefault(false);
    this.setPageManagerProvider(new MyPageManagerProvider(this));
    JPPFConfiguration.reset(getConfig(PanelType.CLIENT).getProperties());
    this.topologyManager = new TopologyManager();
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
   * @return the topololgy manager.
   */
  public JobMonitor getJobMonitor() {
    return jobMonitor;
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
   * Get a localized message given its unique name and the current locale.
   * @param message - the unique name of the localized message.
   * @return a message in the current locale, or the default locale
   * if the localization for the current locale is not found.
   */
  public String localize(final String message) {
    return LocalizationUtils.getLocalized(BASE, message);
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
  public ConfigurationHandler getConfig(final PanelType type) {
    return (type == null) ? null : configMap.get(type);
  }

  /**
   * @return the objects which updates the statistics from all drivers.
   */
  public StatsUpdater getStatsUpdater() {
    return statsUpdater;
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
