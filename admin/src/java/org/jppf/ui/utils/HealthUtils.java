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

package org.jppf.ui.utils;

import java.util.*;

import org.jppf.client.monitoring.topology.*;
import org.jppf.management.*;
import org.jppf.management.diagnostics.ThreadDump;
import org.jppf.utils.LocalizationUtils;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class HealthUtils {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(HealthUtils.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Base name of the resource bundles for localizing the thread dump dialog messages.
   */
  private static final String THREAD_DUMP_BASE = "org.jppf.ui.i18n.ThreadDumpPage";

  /**
   * Retrieve the thread dump for the specified topology object.
   * @param data the topology object for which to get the information.
   * @return a {@link JPPFSystemInformation} or <code>null</code> if the information could not be retrieved.
   */
  public static ThreadDump retrieveThreadDump(final AbstractTopologyComponent data) {
    ThreadDump info = null;
    try {
      if (data.isNode()) {
        final TopologyDriver parent = (TopologyDriver) data.getParent();
        final Map<String, Object> result = parent.getForwarder().threadDump(new UuidSelector(data.getUuid()));
        final Object o = result.get(data.getUuid());
        if (o instanceof ThreadDump) info = (ThreadDump) o;
      }
      else info = ((TopologyDriver) data).getDiagnostics().threadDump();
    } catch (final Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
    }
    return info;
  }

  /**
   * Generate the localized title for the sthread dump popup dialog/window for a given topology component.
   * @param comp the the topology object for which to get the information.
   * @param locale the locale to display the title in.
   * @return a localized string.
   */
  public static String getThreadDumpTitle(final AbstractTopologyComponent comp, final Locale locale) {
    return localizeThreadDumpInfo("threaddump.info_for", locale) + " " +
      localizeThreadDumpInfo(comp.isNode() ? "threaddump.node" : "threaddump.driver", locale) + " " + comp.getDisplayName();
  }

  /**
   * Localize the specified key in the thread dump page.
   * @param key the key to localize.
   * @param locale the locale to use.
   * @return the localized string for the key.
   */
  public static String localizeThreadDumpInfo(final String key, final Locale locale) {
    return LocalizationUtils.getLocalized(THREAD_DUMP_BASE, key, locale);
  }
}
