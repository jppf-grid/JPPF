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
package org.jppf.ui.options;

import javax.swing.*;
import javax.swing.border.*;

import org.jppf.ui.utils.GuiUtils;

import net.miginfocom.swing.MigLayout;

/**
 * Instances of this page represent dynamic UI components representing a page (or panel) container,
 * with the underlying Swing component being a JPanel.<br/>
 * It also implements a specific behavior for radio buttons: any radio button that is a direct child is also
 * added to a {@link javax.swing.ButtonGroup ButtonGroup}.
 * @author Laurent Cohen
 */
public class OptionPanel extends AbstractOptionContainer {
  /**
   * The panel used to display this options page.
   */
  protected JPanel panel = null;
  /**
   * An eventual button group to which any radio button direct child is added.
   */
  protected ButtonGroup buttonGroup = null;

  /**
   * Default constructor.
   */
  public OptionPanel() {
  }

  /**
   * Initialize this option page with the specified parameters.
   * @param name this component's name.
   * @param label the panel's title.
   * @param scrollable determines whether this page should be enclosed within a scroll pane.
   * @param bordered determines whether this page has a border around it.
   */
  public OptionPanel(final String name, final String label, final boolean scrollable, final boolean bordered) {
    this.name = name;
    this.label = label;
    this.scrollable = scrollable;
    this.bordered = bordered;
    createUI();
  }

  /**
   * Initialize this option page with the specified parameters, setting up a page without border.
   * This constructor is used for building outermost pages.
   * @param name this component's name.
   * @param label the panel's title.
   * @param scrollable determines whether this page should be enclosed within a scroll pane.
   */
  public OptionPanel(final String name, final String label, final boolean scrollable) {
    this(name, label, scrollable, false);
  }

  @Override
  public void createUI() {
    panel = new JPanel();
    if (bordered) {
      final Border border = (label != null) ? BorderFactory.createTitledBorder(label) : BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
      panel.setBorder(border);
    } else panel.setBorder(BorderFactory.createEmptyBorder());
    if (toolTipText != null) panel.setToolTipText(toolTipText);
    final MigLayout mig = new MigLayout(layoutConstraints);
    panel.setLayout(mig);
    if (scrollable) {
      final JScrollPane sp = new JScrollPane(panel);
      sp.setBorder(BorderFactory.createEmptyBorder());
      GuiUtils.adjustScrollbarsThickness(sp);
      UIComponent = sp;
    } else UIComponent = panel;
  }

  @Override
  public void add(final OptionElement element) {
    super.add(element);
    if (element instanceof RadioButtonOption) {
      if (buttonGroup == null) buttonGroup = new ButtonGroup();
      buttonGroup.add((JRadioButton) element.getUIComponent());
    }
    panel.add(element.getUIComponent(), element.getComponentConstraints());
  }

  @Override
  public void remove(final OptionElement element) {
    children.remove(element);
    panel.remove(element.getUIComponent());
  }
}
