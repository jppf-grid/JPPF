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

package org.jppf.admin.web.admin;

import java.util.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.jppf.admin.web.*;
import org.slf4j.*;
import org.wicketstuff.wicket.mount.core.annotation.MountPath;

import com.googlecode.wicket.jquery.ui.JQueryUIBehavior;

/**
 * This is the admin page. It can only be instantiated for users with a {@code jppf-admin} role.
 * @author Laurent Cohen
 */
@MountPath(AbstractJPPFPage.PATH_PREFIX + "admin")
@AuthorizeInstantiation("jppf-admin")
public class AdminPage extends TemplatePage {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AdminPage.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * 
   */
  private final Map<ConfigType, AbstractConfigPanel> panelMap = new EnumMap<>(ConfigType.class);

  /**
   * 
   */
  public AdminPage() {
    add(new JQueryUIBehavior("#tabs", "tabs"));
    add(ConfigType.CLIENT, new ClientConfigPanel());
    //add(new DiscoveryConfigPanel());
    add(ConfigType.SSL, new SSLConfigPanel());
    if (adminLink != null) {
      if (debugEnabled) log.debug("setting style on the link");
      adminLink.add(new AttributeModifier("style", "color: #6D78B6; background-color: #C5D0F0"));
      adminLink.setEnabled(false);
    }
  }

  /**
   * 
   * @param type the trype of panel to add.
   * @param panel the panel to add.
   */
  private void add(final ConfigType type, final AbstractConfigPanel panel) {
    panelMap.put(type, panel);
    add(panel);
  }

  /**
   * @param type the type of panel to get.
   * @return the config panel.
   */
  public AbstractConfigPanel getConfigPanel(final ConfigType type) {
    return panelMap.get(type);
  }
}
