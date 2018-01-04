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

package org.jppf.admin.web.topology.systeminfo;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.JPPFWebSession;
import org.jppf.admin.web.tabletree.*;
import org.jppf.admin.web.topology.TopologyConstants;
import org.jppf.admin.web.utils.AbstractActionLink;
import org.jppf.client.monitoring.topology.AbstractTopologyComponent;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.ui.utils.TopologyUtils;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class SystemInfoLink extends AbstractActionLink {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(SystemInfoLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   *
   */
  private transient ModalWindow modal;

  /**
   * @param form .
   */
  public SystemInfoLink(final Form<String> form) {
    super(TopologyConstants.SYSTEM_INFO_ACTION, Model.of("System info"));
    imageName = "info.gif";
    setEnabled(false);
    modal = new ModalWindow("topology.info.dialog");
    form.add(modal);
  }

  @Override
  public void onClick(final AjaxRequestTarget target) {
    if (debugEnabled) log.debug("clicked on System info");
    final JPPFWebSession session = JPPFWebSession.get();
    final TableTreeData data = session.getTopologyData();
    final List<DefaultMutableTreeNode> selectedNodes = data.getSelectedTreeNodes();
    if (!selectedNodes.isEmpty()) {
      final DefaultMutableTreeNode treeNode = selectedNodes.get(0);
      final AbstractTopologyComponent comp = (AbstractTopologyComponent) treeNode.getUserObject();
      final Locale locale = Session.get().getLocale();
      final String title = TopologyUtils.getSystemInfoTitle(comp, locale, false);
      final JPPFSystemInformation info = TopologyUtils.retrieveSystemInfo(comp);
      final StringBuilder html = new StringBuilder();
      html.append(TopologyUtils.formatProperties(info, new HTMLPropertiesTableFormat(title, false), locale));
      if (debugEnabled) log.debug("html = {}", html);
      modal.setPageCreator(new PageCreator(html.toString()));
      stopRefreshTimer(target);
      addTableTreeToTarget(target);
      modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
        @Override
        public void onClose(final AjaxRequestTarget target) {
          restartRefreshTimer(target);
        }
      });
      modal.show(target);
    }
  }

  /** */
  private static class PageCreator implements ModalWindow.PageCreator {
    /** */
    private transient final String html;

    /**
     * @param html .
     */
    public PageCreator(final String html) {
      this.html = html;
    }

    @Override
    public Page createPage() {
      return new SystemInfoPage(html);
    }
  }
}
