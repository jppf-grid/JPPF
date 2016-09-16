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

import org.apache.wicket.*;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.*;
import org.jppf.admin.web.topology.TopologyTree;
import org.jppf.client.monitoring.topology.TopologyManager;
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
  protected transient static String BASE = "";
  /**
   * The topololgy manager.
   */
  private transient TopologyManager topologyManager;

  /**
   * Default constructor.
   */
  public JPPFWebConsoleApplication() {
    if (debugEnabled) log.debug("in JPPFWebConsoleApplication<init>()");
  }

  @Override
  public Class<? extends Page> getHomePage() {
    return TopologyTree.class;
  }

  @Override
  protected void init() {
    super.init();
    this.topologyManager = new TopologyManager();
    //getMarkupSettings().setDefaultBeforeDisabledLink("<b>");
    //getMarkupSettings().setDefaultAfterDisabledLink("</b>");
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
    return new JPPFWebSession(request);
  }
}
