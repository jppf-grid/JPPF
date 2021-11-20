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
package org.jppf.ui.actions;

import javax.swing.*;

import org.jppf.ui.options.*;
import org.jppf.ui.utils.GuiUtils;
import org.slf4j.*;

/**
 * Task that sets the actions in the toolbar.
 */
public class ActionsInitializer implements Runnable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ActionsInitializer.class);
  /**
   * The panel to which the actions apply.
   */
  private OptionElement mainPanel = null;
  /**
   * The panel to which the actions apply.
   */
  private ActionHolder actionHolder = null;
  /**
   * The container for the buttons associated with the actions (toolbar).
   */
  private String btnContainerName = null;

  /**
   * Initialize this actions initializer.
   * @param mainPanel the panel to which the actions apply.
   * @param btnContainerName the container for the buttons associated with the actions (toolbar).
   */
  public ActionsInitializer(final OptionElement mainPanel, final String btnContainerName) {
    this(mainPanel, (ActionHolder) mainPanel, btnContainerName);
  }

  /**
   * Initialize this actions initializer.
   * @param mainPanel the panel to which the actions apply.
   * @param actionHolder the panel to which the actions apply.
   * @param btnContainerName the container for the buttons associated with the actions (toolbar).
   */
  public ActionsInitializer(final OptionElement mainPanel, final ActionHolder actionHolder, final String btnContainerName) {
    this.mainPanel = mainPanel;
    this.actionHolder = actionHolder;
    this.btnContainerName = btnContainerName;
  }

  @Override
  public void run() {
    OptionContainer page = null;
    while (page == null) {
      final OptionElement parent = mainPanel.getParent();
      if (parent != null) page = (OptionContainer) mainPanel.findFirstWithName(btnContainerName);
      try {
        Thread.sleep(100L);
      } catch(final InterruptedException e) {
        log.error(e.getMessage(), e);
      }
      if (page != null) {
        for (final OptionElement elt: page.getChildren()) {
          if (!(elt.getUIComponent() instanceof AbstractButton)) continue;
          final AbstractButton button = (AbstractButton) elt.getUIComponent();
          final UpdatableAction action = actionHolder.getActionHandler().getAction(elt.getName());
          if (action == null) continue;
          if (elt instanceof ButtonOption) {
            final ButtonOption btnOption = (ButtonOption) elt;
            if (btnOption.getIconPath() != null) action.putValue(Action.SMALL_ICON, GuiUtils.loadIcon(btnOption.getIconPath()));
          }
          button.setAction(action);
          button.setText("");
          button.setToolTipText((String) action.getValue(Action.SHORT_DESCRIPTION));
        }
        page.getUIComponent().invalidate();
        page.getUIComponent().repaint();
      }
    }
  }
}
