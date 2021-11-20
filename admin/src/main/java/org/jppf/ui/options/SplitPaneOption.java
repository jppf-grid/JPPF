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
package org.jppf.ui.options;

import javax.swing.JSplitPane;

import org.slf4j.*;

/**
 * This option class encapsulates a split pane, as the one present in the Swing api.
 * @author Laurent Cohen
 */
public class SplitPaneOption extends AbstractOptionContainer {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SplitPaneOption.class);
  /**
   * Horizontal split.
   */
  public static final int HORIZONTAL = 0;
  /**
   * Vertical split.
   */
  public static final int VERTICAL = 1;
  /**
   * Used when nothing is set in the left (top) panel.
   */
  protected final Option FILLER1 = new FillerOption(0, 0);
  /**
   * Used when nothing is set in the right (bottom) panel.
   */
  protected final Option FILLER2 = new FillerOption(0, 0);
  /**
   * The split pane's resize weight.
   */
  protected double resizeWeight = 0.5d;
  /**
   * The split pane's divider width.
   */
  protected int dividerWidth = 4;
  /**
   * The orientation of the split, one of {@link #VERTICAL} or {@link #HORIZONTAL}.
   */
  protected int orientation = 0;

  @Override
  public void createUI() {
    final JSplitPane pane = new JSplitPane();
    pane.setOrientation(orientation == HORIZONTAL ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT);
    UIComponent = pane;
    children.add(FILLER1);
    children.add(FILLER2);
    pane.setLeftComponent(FILLER1.getUIComponent());
    pane.setRightComponent(FILLER2.getUIComponent());
    pane.setDividerSize(dividerWidth);
    pane.setResizeWeight(resizeWeight);
    pane.setOpaque(false);
  }

  @Override
  public void add(final OptionElement element) {
    final JSplitPane pane = (JSplitPane) UIComponent;
    if (FILLER1 == children.get(0)) {
      children.remove(0);
      children.add(0, element);
      pane.setLeftComponent(element.getUIComponent());
    } else if (FILLER2 == children.get(1)) {
      children.remove(1);
      children.add(1, element);
      pane.setRightComponent(element.getUIComponent());
    } else {
      final String msg = '[' + this.toString() + "] This split pane can't contain more than 2 elements";
      System.err.println(msg);
      log.error(msg);
      return;
    }
    if (element instanceof AbstractOptionElement) ((AbstractOptionElement) element).setParent(this);
  }

  @Override
  public void remove(final OptionElement element) {
    final int idx = children.indexOf(element);
    if (idx < 0) return;
    final JSplitPane pane = (JSplitPane) UIComponent;
    if (idx == 0) {
      children.remove(0);
      children.add(0, FILLER1);
      pane.setLeftComponent(FILLER1.getUIComponent());
    } else {
      children.remove(1);
      children.add(1, FILLER2);
      pane.setRightComponent(FILLER2.getUIComponent());
    }
    if (element instanceof AbstractOptionElement) ((AbstractOptionElement) element).setParent(null);
  }

  /**
   * Get the split pane's divider width.
   * @return the divider width as an int value.
   */
  public int getDividerWidth() {
    return dividerWidth;
  }

  /**
   * Set the split pane's divider width.
   * @param dividerWidth the divider width as an int value.
   */
  public void setDividerWidth(final int dividerWidth) {
    this.dividerWidth = dividerWidth;
  }

  /**
   * Get the split pane's resize weight.
   * @return the resize weight as a double value.
   */
  public double getResizeWeight() {
    return resizeWeight;
  }

  /**
   * Set the split pane's resize weight.
   * @param resizeWeight the resize weight as a double value.
   */
  public void setResizeWeight(final double resizeWeight) {
    this.resizeWeight = resizeWeight;
  }

  /**
   * Get the orientation of the split.
   * @return one of {@link #VERTICAL} or {@link #HORIZONTAL}.
   */
  public int getOrientation() {
    return orientation;
  }

  /**
   * Set the orientation of the split.
   * @param orientation - one of {@link #VERTICAL} or {@link #HORIZONTAL}.
   */
  public void setOrientation(final int orientation) {
    this.orientation = orientation;
  }
}
