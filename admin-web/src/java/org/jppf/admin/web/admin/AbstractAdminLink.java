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

import org.jppf.admin.web.utils.AjaxButtonWithIcon;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * This class represents the save configuration button in the config panel of the admin page.
 * @author Laurent Cohen
 */
public abstract class AbstractAdminLink extends AjaxButtonWithIcon {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(AbstractAdminLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The type of config panel to add this button to.
   */
  protected final ConfigType type;

  /**
   * Initialize.
   * @param type the type of config panel to add this button to.
   * @param id the id assigned to this action button.
   * @param imageName the name of the icon associated with this button..
   */
  public AbstractAdminLink(final ConfigType type, final String id, final String imageName) {
    super(computeId(type, id), imageName);
    this.type = type;
  }

  /**
   * Compute an id given the config panel type and id suffix.
   * @param type the type of config panel.
   * @param id the given id.
   * @return a Wicket-compatible id.
   */
  private static String computeId(final ConfigType type, final String id) {
    return (id.startsWith(type.getPrefix())) ? id : type.getPrefix() + id;
  }
}
