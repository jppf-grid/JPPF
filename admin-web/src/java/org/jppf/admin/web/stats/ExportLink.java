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

package org.jppf.admin.web.stats;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.util.resource.*;
import org.jppf.admin.web.*;
import org.jppf.admin.web.utils.*;
import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.ui.monitoring.data.*;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * This class represents the download configuration button in the config panel of the admin page.
 * @author Laurent Cohen
 */
public class ExportLink extends AjaxButtonWithIcon {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(ExportLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Export to text format.
   */
  static final int TEXT = 1;
  /**
   * Export to CSV format.
   */
  static final int CSV = 2;
  /**
   * 
   */
  private ConfigDownload configDownload;
  /**
   * 
   */
  private final int format;

  /**
   * Initialize.
   * @param format the format to export to.
   */
  public ExportLink(final int format) {
    super("stats.export." + (format == TEXT ? "text" : "csv"), (format == TEXT) ? "text-2.png" : "calc.png");
    this.format = format;
    add(configDownload = new ConfigDownload());
  }

  @Override
  public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
    if (debugEnabled) log.debug("clicked on {}", getId());
    configDownload.initiate(target);
  }

  /**
   * 
   */
  private class ConfigDownload extends AJAXDownload {
    @Override
    protected String getFileName() {
      return "statitics." + (format == TEXT ? "txt" : "csv");
    }

    @Override
    protected IResourceStream getResourceStream() {
      final JPPFWebSession session = JPPFWebSession.get();
      final TopologyDriver driver = session.getCurrentDriver();
      final BaseStatsHandler handler = JPPFWebConsoleApplication.get().getStatsUpdater();
      final StatsExporter exporter = (format == TEXT) ? new TextStatsExporter(handler, driver, session.getLocale()) : new CsvStatsExporter(handler, driver);
      return new StringResourceStream(exporter.formatAll(), "text/plain");
    }
  }
}
