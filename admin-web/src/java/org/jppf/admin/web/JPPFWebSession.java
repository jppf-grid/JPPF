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

import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;
import org.jppf.admin.web.tabletree.*;
import org.jppf.ui.monitoring.node.NodeTreeTableModel;

/**
 *
 * @author Laurent Cohen
 */
public class JPPFWebSession extends WebSession {
  /**
   * The topology tree table model.
   */
  private transient NodeTreeTableModel topologyModel;
  /**
   * Handles the selection of rows in the topology tree table.
   */
  private transient SelectionHandler topologySelectionHandler;
  /**
   * The topology tree table component.
   */
  private transient JPPFTableTree topologyTableTree;

  /**
   * Initialize a new session.
   * @param request the request.
   */
  public JPPFWebSession(final Request request) {
    super(request);
  }

  @Override
  public void onInvalidate() {
    super.onInvalidate();
    topologyModel = null;
    topologySelectionHandler = null;
  }

  /**
   *
   * @return the topology tree table model.
   */
  public NodeTreeTableModel getTopologyModel() {
    return topologyModel;
  }

  /**
   *
   * @param topologyModel the topology tree table model.
   */
  public void setTopologyModel(final NodeTreeTableModel topologyModel) {
    this.topologyModel = topologyModel;
  }

  /**
   *
   * @return the topology selection handler.
   */
  public SelectionHandler getTopologySelectionHandler() {
    return topologySelectionHandler;
  }

  /**
   *
   * @param selectionHandler the topology selection handler.
   */
  public void setTopologySelectionHandler(final SelectionHandler selectionHandler) {
    this.topologySelectionHandler = selectionHandler;
  }

  /**
   *
   * @return the topology tree table component.
   */
  public JPPFTableTree getTopologyTableTree() {
    return topologyTableTree;
  }

  /**
   *
   * @param topologyTableTree the topology tree table component.
   */
  public void setTopologyTableTree(final JPPFTableTree topologyTableTree) {
    this.topologyTableTree = topologyTableTree;
  }
}
