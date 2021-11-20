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

package org.jppf.admin.web.tabletree;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.ContextImage;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.jppf.admin.web.JPPFWebConsoleApplication;

/**
 * A panel that associates a label with an icon.
 * @author Laurent Cohen
 */
public class NodeContent extends Panel {
  /**
   *
   * @param id id of this component.
   * @param treeNode represents the node to render.
   * @param renderer provides the content for rendering the tree node, i.e. icon and text.
   * @param showIP whether to show IP addresses vs. host names.
   */
  public NodeContent(final String id, final DefaultMutableTreeNode treeNode, final TreeNodeRenderer renderer, final boolean showIP) {
    super(id);
    final String contextPath = RequestCycle.get().getRequest().getContextPath();
    final String iconPath = renderer.getIconPath(treeNode);
    final String resourceURL = (iconPath != null) ? JPPFWebConsoleApplication.get().getSharedImageURL(iconPath) : null;
    //if (debugEnabled) log.debug("resourceURL for key = {}: {}", iconPath, resourceURL);
    add(new ContextImage("icon", (resourceURL != null) ? contextPath + resourceURL : ""));
    final String text = renderer.getText(treeNode, showIP);
    add(new Label("text", (text != null) ? text : ""));
  }
}
