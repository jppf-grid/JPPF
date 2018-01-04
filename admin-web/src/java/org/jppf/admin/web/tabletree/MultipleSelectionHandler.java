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

package org.jppf.admin.web.tabletree;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree.State;
import org.jppf.client.monitoring.AbstractComponent;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * A selectionHandler that handles the selection of multiple items.
 */
public class MultipleSelectionHandler extends AbstractSelectionHandler {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(MultipleSelectionHandler.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Selected rows in the table by driver/node uuid.
   */
  private transient final Set<String> selected = new HashSet<>();
  /**
   * The uuid of the last selected row, if any.
   */
  private transient String lastSelected;

  @Override
  public boolean handle(final AjaxRequestTarget target, final DefaultMutableTreeNode node, final Object...params) {
    if (debugEnabled) log.debug(String.format("handling %s with params %s", node, Arrays.asList(params)));
    if ((filter != null) && !filter.accepts(node)) return false;
    if (debugEnabled) log.debug(String.format("node %s accepted", node));
    TypedProperties props = null;
    if ((params != null) && (params.length > 0)) {
      if (params[0] instanceof TypedProperties) props = (TypedProperties) params[0];
    }
    final boolean ctrl = (props == null) ? false : props.getBoolean("ctrl", false);
    final boolean shift = (props == null) ? false : props.getBoolean("shift", false);
    final AbstractComponent<?> data = (AbstractComponent<?>) node.getUserObject();
    final String uuid = data.getUuid();
    final boolean sel = isSelected(uuid);
    final int size = selected.size();
    boolean selectionChanged = true;
    if (debugEnabled) log.debug(String.format("ctrl=%b, shift=%b, sel=%b, size=%d", ctrl, shift, sel, size));
    if (shift && (lastSelected != null)) {
      blockSelect(target, node, uuid);
      lastSelected = uuid;
    } else if (ctrl) {
      if (isSelected(uuid)) {
        selected.remove(uuid);
        lastSelected = null;
      } else {
        selected.add(uuid);
        lastSelected = uuid;
      }
    } else {
      if ((selected.size() == 1) && isSelected(uuid)) selectionChanged = false;
      selected.clear();
      selected.add(uuid);
      lastSelected = uuid;
    }
    if (selectionChanged) {
      for (SelectionListener listener: listeners) listener.selectionChanged(this);
    }
    return selectionChanged;
  }

  /**
   *
   * @param treeNode the node that was shift-clicked.
   * @param uuid the node's uuid.
   * @param target ajax target for the selection request.
   */
  private void blockSelect(final AjaxRequestTarget target, final DefaultMutableTreeNode treeNode, final String uuid) {
    final Map<String, Integer> uuidToPos = new HashMap<>();
    final SortedMap<Integer, String> posToUuid = new TreeMap<>();
    int pos = 0;
    final DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeNode.getRoot();
    for (int i=0; i<root.getChildCount(); i++) {
      final DefaultMutableTreeNode driver = (DefaultMutableTreeNode) root.getChildAt(i);
      final AbstractComponent<?> driverData = (AbstractComponent<?>) driver.getUserObject();
      final String driverUuid = driverData.getUuid();
      posToUuid.put(pos, driverUuid);
      uuidToPos.put(driverUuid, pos);
      pos++;
      //JPPFTableTree tableTree = (JPPFTableTree) target.getPage().get("table.tree");
      final JPPFTableTree tableTree = ((AbstractTableTreePage) target.getPage()).getTableTree();
      if (tableTree.getState(driver) == State.EXPANDED) {
        for (int j=0; j<driver.getChildCount(); j++) {
          final DefaultMutableTreeNode node = (DefaultMutableTreeNode) driver.getChildAt(j);
          if ((filter != null) && !filter.accepts(node)) continue;
          final AbstractComponent<?> nodeData = (AbstractComponent<?>) node.getUserObject();
          final String nodeUuid = nodeData.getUuid();
          posToUuid.put(pos, nodeUuid);
          uuidToPos.put(nodeUuid, pos);
          pos++;
        }
      }
    }
    if (lastSelected != null) {
      final int lastIndex = uuidToPos.get(lastSelected);
      final int nodeIndex = uuidToPos.get(uuid);
      final SortedMap<Integer, String> sub = (lastIndex <= nodeIndex) ? posToUuid.subMap(lastIndex, nodeIndex + 1) : posToUuid.subMap(nodeIndex, lastIndex + 1);
      selected.clear();
      selected.addAll(sub.values());
    }
  }

  @Override
  public List<String> getSelected() {
    return  new ArrayList<>(selected);
  }

  @Override
  public boolean isSelected(final String uuid) {
    return (uuid == null) ? false : selected.contains(uuid);
  }


  @Override
  public void select(final String uuid) {
    if (!selected.contains(uuid)) selected.add(uuid);
  }

  @Override
  public void unselect(final String uuid) {
    if (selected.contains(uuid)) selected.remove(uuid);
  }

  @Override
  public void clearSelection() {
    selected.clear();
  }
}
