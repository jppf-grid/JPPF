/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.management;

import org.jppf.utils.LocalizationUtils;

/**
 * An enumeration of the possible pending actions for a node, set via one of the
 * {@code shutdown(false)} or {@code restart(false)} methods of {@link org.jppf.management.JPPFNodeAdminMBean JPPFNodeAdminMBean}. 
 * @author Laurent Cohen
 */
public enum NodePendingAction {
  /**
   * There is no pending action.
   */
  NONE("node.pending.none"),
  /**
   * A deferred shutdown was requested.
   */
  SHUTDOWN("node.pending.shutdown"),
  /**
   * A deferred restart was requested.
   */
  RESTART("node.pending.restart");

  /**
   * Base name for the localization resource bundles.
   */
  private static final String I18N = "org.jppf.server.i18n.messages";
  /**
   * The name to display.
   */
  private final String displayName;

  /**
   * Initialize this enum element with the specified localized display name.
   * @param msg the display name ot localize.
   */
  private NodePendingAction(final String msg) {
    displayName = LocalizationUtils.getLocalized(I18N, msg);
  }

  @Override
  public String toString() {
    return displayName;
  }
}
