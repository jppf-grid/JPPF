/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.util.resource.*;
import org.jppf.admin.web.utils.*;
import org.slf4j.*;

/**
 * This class represents the download configuration button in the config panel of the admin page.
 * @author Laurent Cohen
 */
public class DownloadLink extends AjaxButtonWithIcon {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(DownloadLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = log.isDebugEnabled();
  /**
   * 
   */
  private ConfigDownload configDownload;

  /**
   * Initialize.
   */
  public DownloadLink() {
    super("node.filter.download", "download.png");
    add(configDownload = new ConfigDownload());
  }

  @Override
  public void onSubmit(final AjaxRequestTarget target) {
    if (debugEnabled) log.debug("clicked on node.filter.download");
    configDownload.initiate(target);
  }

  /**
   * 
   */
  private class ConfigDownload extends AJAXDownload {
    @Override
    protected String getFileName() {
      return "ExecutionPoliicy.xml";
    }

    @Override
    protected IResourceStream getResourceStream() {
      final TextArea<String> area = ((NodeFilterPage) getPage()).getPolicyField();
      String configString = area.getModelObject();
      if (configString == null) configString = "";
      return new StringResourceStream(configString, "text/xml");
    }
  }
}
