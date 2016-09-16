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

package org.jppf.admin.web.tabletree;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.monitoring.topology.AbstractTopologyComponent;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * A selectionHandler that handles the selection of a single item.
 */
public class SingleSelectionHandler extends AbstractSelectionHandler {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(SingleSelectionHandler.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Selected rows in the table by driver/node uuid.
   */
  private transient String selected = null;

  @Override
  public boolean handle(final DefaultMutableTreeNode node, final Object...params) {
    if (debugEnabled) log.debug("handling {}", node);
    AbstractTopologyComponent data = (AbstractTopologyComponent) node.getUserObject();
    String uuid = data.getUuid();
    selected = (uuid.equals(selected)) ? null : uuid;
    return false;
  }

  @Override
  public List<String> getSelected() {
    if (selected == null) return  Collections.emptyList();
    else return  Arrays.asList(selected);
  }

  @Override
  public boolean isSelected(final String uuid) {
    return (uuid == null) ? false : uuid.equals(selected);
  }

  @Override
  public void clear() {
    selected = null;
  }
}