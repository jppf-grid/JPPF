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

import javax.swing.tree.*;

import org.apache.wicket.*;
import org.apache.wicket.ajax.*;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.tree.TableTree;
import org.apache.wicket.extensions.markup.html.repeater.util.TreeModelProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.*;
import org.apache.wicket.model.*;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.jppf.admin.web.JPPFWebSession;
import org.jppf.client.monitoring.AbstractComponent;
import org.jppf.ui.treetable.*;
import org.jppf.ui.utils.TreeTableUtils;
import org.jppf.utils.TypedProperties;

/**
 *
 */
public class JPPFTableTree extends TableTree<DefaultMutableTreeNode, String> {
  /**
   *
   */
  private transient final SelectionHandler selectionHandler;
  /**
   * Provides the content for rendering tree nodes, i.e. icon and text.
   */
  private transient final TreeNodeRenderer nodeRenderer;
  /**
   * The backing tree data model.
   */
  private transient final AbstractJPPFTreeTableModel nodeTreeModel;
  /**
   *
   */
  private transient final List<Component> updateTargets = new ArrayList<>();
  /**
   * The type of view this table tree is in.
   */
  private transient final TreeViewType type;

  /**
   *
   * @param type the type of view this table tree is in.
   * @param id .
   * @param columns .
   * @param nodeTreeModel .
   * @param rowsPerPage .
   * @param selectionHandler handles selection events.
   * @param nodeRenderer provides the content for rendering tree nodes, i.e. icon and text.
   * @param expansionModel keeps tabs on the expanded nodes in the tree.
   */
  public JPPFTableTree(final TreeViewType type, final String id, final List<? extends IColumn<DefaultMutableTreeNode, String>> columns, final AbstractJPPFTreeTableModel nodeTreeModel,
    final long rowsPerPage, final SelectionHandler selectionHandler, final TreeNodeRenderer nodeRenderer, final IModel<Set<DefaultMutableTreeNode>> expansionModel) {
    super(id, columns, new JPPFModelProvider(nodeTreeModel), rowsPerPage, expansionModel);
    this.type = type;
    this.selectionHandler = selectionHandler;
    this.nodeRenderer = nodeRenderer;
    this.nodeTreeModel = nodeTreeModel;
    //if (selectionHandler instanceof AbstractSelectionHandler) ((AbstractSelectionHandler) selectionHandler).setTableTree(this);
  }

  @Override
  protected Component newContentComponent(final String id, final IModel<DefaultMutableTreeNode> model) {
    DefaultMutableTreeNode node = model.getObject();
    WebMarkupContainer panel = new NodeContent(id, node, nodeRenderer, JPPFWebSession.get().isShowIP());
    return panel;
  }

  @Override
  protected Item<DefaultMutableTreeNode> newRowItem(final String id, final int index, final IModel<DefaultMutableTreeNode> model) {
    final Item<DefaultMutableTreeNode> item = new OddEvenItem<>(id, index, model);
    if (selectionHandler != null) {
      item.add(new SelectionBehavior(model.getObject(), type));
    }
    return item;
  }

  /**
   * @return the selection handler for this table tree.
   */
  public SelectionHandler getSelectionHandler() {
    return selectionHandler;
  }

  /**
   * @return the backing tree data model.
   */
  public AbstractJPPFTreeTableModel getNodeTreeModel() {
    return nodeTreeModel;
  }

  /**
   * Add a component to update upon selection changes.
   * @param comp the component to add.
   */
  public void addUpdateTarget(final Component comp) {
    updateTargets.add(comp);
  }

  /**
   *
   */
  public static class JPPFModelProvider extends TreeModelProvider<DefaultMutableTreeNode> {
    /**
     *
     * @param treeModel the backing tree data model.
     */
    public JPPFModelProvider(final TreeModel treeModel) {
      super(treeModel, false);
    }

    @Override
    public IModel<DefaultMutableTreeNode> model(final DefaultMutableTreeNode object) {
      return Model.of(object);
    }
  }

  /**
   * 
   */
  public static class SelectionBehavior extends AjaxEventBehavior {
    /**
     * Uuid of the tree node to which the selection event applies.
     */
    private final String uuid;
    /**
     * The type of view.
     */
    private final TreeViewType type;

    /**
     *
     * @param node the tree node to which the selection applies.
     * @param type the type of view.
     */
    public SelectionBehavior(final DefaultMutableTreeNode node, final TreeViewType type) {
      super("click");
      this.uuid = ((AbstractComponent<?>) node.getUserObject()).getUuid();
      this.type = type;
    }

    @Override
    protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
      super.updateAjaxAttributes(attributes);
      attributes.getDynamicExtraParameters().add("return {'ctrl' : attrs.event.ctrlKey, 'shift' : attrs.event.shiftKey}");
    }

    @Override
    protected void onEvent(final AjaxRequestTarget target) {
      JPPFWebSession session = (JPPFWebSession) target.getPage().getSession();
      TableTreeData data = session.getTableTreeData(type);
      SelectionHandler selectionHandler = data.getSelectionHandler();
      DefaultMutableTreeNode node = TreeTableUtils.findTreeNode((DefaultMutableTreeNode) data.getModel().getRoot(), uuid, selectionHandler.getFilter());
      if (node != null) {
        IRequestParameters params = RequestCycle.get().getRequest().getRequestParameters();
        TypedProperties props = new TypedProperties()
          .setBoolean("ctrl", params.getParameterValue("ctrl").toBoolean(false))
          .setBoolean("shift", params.getParameterValue("shift").toBoolean(false));
        Page page = target.getPage();
        if (selectionHandler.handle(target, node, props) && (page instanceof TableTreeHolder)) {
          TableTreeHolder holder = (TableTreeHolder) page;
          target.add(holder.getTableTree());
          target.add(holder.getToolbar());
        }
      }
    }
  }
}
