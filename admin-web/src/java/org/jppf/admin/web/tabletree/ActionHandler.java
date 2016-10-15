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

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.form.Form;
import org.jppf.admin.web.JPPFWebSession;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class ActionHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ActionHandler.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   *
   */
  private transient Map<String, UpdatableAction> actions = new HashMap<>();
  /**
   *
   */
  private transient Map<String, AbstractActionLink> actionLinks = new HashMap<>();

  /**
   *
   */
  public ActionHandler() {
  }

  /**
   * Update the actions managed by this handler based ont he selected nodes.
   * @param treeNodes the selected nodes.
   */
  public void selectionChanged(final List<DefaultMutableTreeNode> treeNodes) {
    if (debugEnabled) log.debug("selected nodes: {}", treeNodes);
    for (String id: actionLinks.keySet()) {
      AbstractActionLink link = actionLinks.get(id);
      UpdatableAction action = actions.get(id);
      if (action != null) {
        JPPFWebSession session = (JPPFWebSession) Session.get();
        action.setAuthorized(session.getRoles());
        action.setEnabled(treeNodes);
        if (link != null) link.setEnabled(action.isAuthorized() && action.isEnabled());
      } else {
        if (link != null) link.setEnabled(true);
      }
    }
  }

  /**
   * Add an action to this handler.
   * @param id id of the action to add.
   * @param action the action to add.
   */
  public void addAction(final String id, final UpdatableAction action) {
    actions.put(id, action);
  }

  /**
   * Add an action to this handler.
   * @param toolbar the parent of the link to add.
   * @param link the action link to add.
   */
  public void addActionLink(final Form<String> toolbar, final AbstractActionLink link) {
    UpdatableAction action = actions.get(link.getId());
    if (debugEnabled) log.debug(String.format("adding link %s, id=%s, action=%s", link, link.getId(), action));
    link.setAction(action);
    toolbar.add(link);
    actionLinks.put(link.getId(), link);
  }
}
