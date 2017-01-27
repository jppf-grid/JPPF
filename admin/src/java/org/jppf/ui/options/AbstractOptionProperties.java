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

import java.awt.event.MouseListener;
import java.util.*;

import javax.swing.*;

import org.jppf.ui.options.event.ValueChangeListener;
import org.jppf.ui.options.xml.OptionDescriptor.ScriptDescriptor;
import org.jppf.ui.utils.GuiUtils;

/**
 * Default abstract implementation of the <code>OptionElement</code> interface.
 * @author Laurent Cohen
 */
public abstract class AbstractOptionProperties implements OptionProperties {
  /**
   * The label or title displayed with the UI component.
   */
  protected String label = null;
  /**
   * The name of this option element.
   */
  protected String name;
  /**
   * The tooltip text displayed with the UI component.
   */
  protected String toolTipText = null;
  /**
   * Get the UI component for this option element.
   */
  protected transient JComponent UIComponent = null;
  /**
   * Path to an eventual icon displayed in the button.
   */
  protected String iconPath = null;
  /**
   * Determines whether this page should be enclosed within a scroll pane.
   */
  protected boolean scrollable = false;
  /**
   * Whether the scroll pane can have a horizontal scrollbar (if {@code scrollable} is {@code true}).
   */
  protected boolean horizontalScrollbar = true;
  /**
   * Whether the scroll pane can have a vertical scrollbar (if {@code scrollable} is {@code true}).
   */
  protected boolean verticalScrollbar = true;
  /**
   * Determines whether this option has a border around it.
   */
  protected boolean bordered = false;
  /**
   * Scripts used by this option or its children.
   */
  protected List<ScriptDescriptor> scripts = new ArrayList<>();
  /**
   * The action to fire immediately after the page is built, allowing to
   * perform initializations before the page is displayed and used.
   */
  protected ValueChangeListener initializer = null;
  /**
   * The action to fire immediately when the page is disposed.
   */
  protected ValueChangeListener finalizer = null;
  /**
   * A Java or scripted mouse click listener.
   */
  protected MouseListener mouseListener = null;
  /**
   * Determines whether firing events is enabled or not.
   */
  protected boolean eventsEnabled = true;
  /**
   * Mig layout constraints for the entire layout.
   */
  protected String layoutConstraints = null;
  /**
   * Mig layout constraints for a component.
   */
  protected String componentConstraints = null;
  /**
   * Determines whether this component can be detached to a different view.
   */
  protected boolean detachable = true;

  /**
   * Constructor provided as a convenience to facilitate the creation of
   * option elements through reflexion.
   */
  protected AbstractOptionProperties() {
  }

  /**
   * Create the UI components for this option.
   */
  public abstract void createUI();

  /**
   * Get the label displayed with the UI component.
   * @return the label as a string.
   */
  @Override
  public String getLabel() {
    return label;
  }

  /**
   * Set the label displayed with the UI component.
   * @param label the label as a string.
   */
  public void setLabel(final String label) {
    this.label = label;
  }

  /**
   * Get the name of this option.
   * @return the name as a string.
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Set the name of this option.
   * @param name the name as a string.
   */
  public void setName(final String name) {
    this.name = name;
  }

  /**
   * Get the UI component for this option.
   * @return a <code>JComponent</code> instance.
   */
  @Override
  public JComponent getUIComponent() {
    return UIComponent;
  }

  /**
   * Set the UI component for this option.
   * @param component a <code>JComponent</code> instance.
   */
  public void setUIComponent(final JComponent component) {
    UIComponent = component;
  }

  /**
   * Get the tooltip text displayed with the UI component.
   * @return the tooltip as a string.
   */
  @Override
  public String getToolTipText() {
    return toolTipText;
  }

  /**
   * Set the tooltip text displayed with the UI component.
   * @param tooltip the tooltip as a string.
   */
  public void setToolTipText(final String tooltip) {
    String html = tooltip;
    if (((tooltip == null) || "".equals(tooltip.trim()))) html = null;
    else if (tooltip.contains("\\n")) html = "<html>"+tooltip.replace("\\n", "<br>")+"</html>";
    this.toolTipText = html;
  }

  /**
   * Determine whether this page should be enclosed within a scroll pane.
   * @return true if the page is to be enclosed in a scroll page, false otherwise.
   */
  @Override
  public boolean isScrollable() {
    return scrollable;
  }

  /**
   * Determine whether this page should be enclosed within a scroll pane.
   * @param scrollable true if the page is to be enclosed in a scroll pane, false otherwise.
   */
  public void setScrollable(final boolean scrollable) {
    this.scrollable = scrollable;
  }

  /**
   * Determine whether this page has a border around it.
   * @return true if the page has a border, false otherwise.
   * @see org.jppf.ui.options.OptionElement#isBordered()
   */
  @Override
  public boolean isBordered() {
    return bordered;
  }

  /**
   * Determine whether this page has a border around it.
   * @param bordered true if the page has a border, false otherwise.
   */
  public void setBordered(final boolean bordered) {
    this.bordered = bordered;
  }

  /**
   * Get the scripts used by this option or its children.
   * @return a list of <code>ScriptDescriptor</code> instances.
   */
  @Override
  public List<ScriptDescriptor> getScripts() {
    return scripts;
  }

  /**
   * Get the initializer for this option.
   * @return a <code>ValueChangeListener</code> instance.
   */
  @Override
  public ValueChangeListener getInitializer() {
    return initializer;
  }

  /**
   * Set the initializer for this option.
   * @param initializer a <code>ValueChangeListener</code> instance.
   */
  public void setInitializer(final ValueChangeListener initializer) {
    this.initializer = initializer;
  }

  /**
   * Get the finalizer for this option.
   * @return a <code>ValueChangeListener</code> instance.
   */
  @Override
  public ValueChangeListener getFinalizer() {
    return finalizer;
  }

  /**
   * Set the finalizer for this option.
   * @param finalizer a <code>ValueChangeListener</code> instance.
   */
  public void setFinalizer(final ValueChangeListener finalizer) {
    this.finalizer = finalizer;
  }

  /**
   * Get the path to an eventual icon displayed in the button.
   * @return the path as a string.
   */
  @Override
  public String getIconPath() {
    return iconPath;
  }

  /**
   * Set the path to an eventual icon displayed in the button.
   * @param iconPath the path as a string.
   */
  public void setIconPath(final String iconPath) {
    this.iconPath = iconPath;
  }

  /**
   * Determine whether the events firing in this option and/or its children are enabled.
   * @return enabled true if the events are enabled, false otherwise.
   */
  @Override
  public boolean isEventsEnabled() {
    return eventsEnabled;
  }

  /**
   * Enable or disable the events firing in this option and/or its children.
   * @param enabled true to enable the events, false to disable them.
   */
  @Override
  public void setEventsEnabled(final boolean enabled) {
    eventsEnabled = enabled;
  }

  /**
   * Get the Mig layout constraints for the entire layout.
   * @return the constraints as a string.
   */
  @Override
  public String getLayoutConstraints() {
    return layoutConstraints;
  }

  /**
   * Set the Mig layout constraints for the entire layout.
   * @param layoutConstraints - the constraints as a string.
   */
  @Override
  public void setLayoutConstraints(final String layoutConstraints) {
    this.layoutConstraints = layoutConstraints;
  }

  /**
   * Get the Mig layout constraints for a component.
   * @return the constraints as a string.
   */
  @Override
  public String getComponentConstraints() {
    return componentConstraints;
  }

  /**
   * Set the Mig layout constraints for a component.
   * @param componentConstraints - the constraints as a string.
   */
  @Override
  public void setComponentConstraints(final String componentConstraints) {
    this.componentConstraints = componentConstraints;
  }

  @Override
  public void setEditable(final boolean editable) {
  }

  @Override
  public boolean isDetachable() {
    return detachable;
  }

  /**
   * Specify whether this component can be detached to a different view.
   * @param detachable <code>true</code> if this component can be detached, <code>false</code> otherwise.
   */
  public void setDetachable(final boolean detachable) {
    this.detachable = detachable;
  }

  @Override
  public MouseListener getMouseListener() {
    return mouseListener;
  }

  @Override
  public void setMouseListener(final MouseListener mouseListener) {
    this.mouseListener = mouseListener;
  }

  /**
   * Determine whether the scroll pane can have a horizontal scrollbar (if {@code scrollable} is {@code true}).
   * @return {@code true} if the scroll pane can have a horizontal scroll bar, {@code false} otherwise.
   */
  public boolean isHorizontalScrollbar() {
    return horizontalScrollbar;
  }

  /**
   * Specify whether the scroll pane can have a horizontal scrollbar (if {@code scrollable} is {@code true}).
   * @param horizontalScrollbar {@code true} if the scroll pane can have a horizontal scroll bar, {@code false} otherwise.
   */
  public void setHorizontalScrollbar(final boolean horizontalScrollbar) {
    this.horizontalScrollbar = horizontalScrollbar;
  }

  /**
   * Determine whether the scroll pane can have a vertical scrollbar (if {@code scrollable} is {@code true}).
   * @return {@code true} if the scroll pane can have a vertical scroll bar, {@code false} otherwise.
   */
  public boolean isVerticalScrollbar() {
    return verticalScrollbar;
  }

  /**
   * Specify whether the scroll pane can have a vertical scrollbar (if {@code scrollable} is {@code true}).
   * @param verticalScrollbar {@code true} if the scroll pane can have a vertical scroll bar, {@code false} otherwise.
   */
  public void setVerticalScrollbar(final boolean verticalScrollbar) {
    this.verticalScrollbar = verticalScrollbar;
  }

  /**
   * Create a JScrollPane for the specified view, based on the scroll pane policies.
   * @param comp the view for which to create the scroll pane.
   * @return {@link JScrollPane} instance.
   */
  protected JScrollPane createScrollPane(final JComponent comp) {
    JScrollPane jsc = new JScrollPane(comp);
    GuiUtils.adjustScrollbarsThickness(jsc);
    jsc.setHorizontalScrollBarPolicy(isHorizontalScrollbar() ? ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED : ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    jsc.setVerticalScrollBarPolicy(isVerticalScrollbar() ? ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED : ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    return jsc;
  }
}
