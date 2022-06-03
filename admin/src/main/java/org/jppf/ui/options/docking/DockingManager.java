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

package org.jppf.ui.options.docking;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.*;

import org.jppf.ui.options.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.utils.GuiUtils;
import org.jppf.utils.LocalizationUtils;

/**
 * This class handles the docking / undocking of of the tabs in all tabbed views.
 * @author Laurent Cohen
 */
public final class DockingManager {
  /**
   * 
   */
  private static final String I18N_BASE = "org.jppf.ui.i18n.docking";
  /**
   * Prefix for the name of each view.
   */
  public static final String VIEW_PREFIX = localize("view.name.prefix");
  /**
   * Name of the initial / main view.
   */
  public static final String INITIAL_VIEW = localize("intial.view.name");
  /**
   * Sequence number for each created view.
   */
  private static final AtomicInteger VIEW_SEQ = new AtomicInteger(0);
  /**
   * The singleton instance of this class.
   */
  private static final DockingManager instance = new DockingManager();
  /**
   * Mapping of components to their display state.
   */
  private Map<Component, DetachableComponentDescriptor> componentMap = new IdentityHashMap<>();
  /**
   * Mapping of listener components to their corresponding detachable component.
   */
  private Map<Component, Component> listenerToComponentMap = new IdentityHashMap<>();
  /**
   * Mapping of view ids to the corresponding UI frame.
   */
  private final Map<String, ViewDescriptor> viewMap = new TreeMap<>();
  /**
   * Handles what to do when a view frame is closed.
   */
  private final WindowAdapter windowAdapter = new WindowAdapter() {
    @Override
    public void windowClosing(final WindowEvent e) {
      String key = null;
      for (Map.Entry<String, ViewDescriptor> entry: viewMap.entrySet()) {
        if (entry.getValue().getFrame() == e.getWindow()) {
          key = entry.getKey();
          break;
        }
      }
      if (key != null) removeView(key);
    }
  };
  /**
   * The mouse listener for the popup menu on right-click.
   */
  private final DockingMouseAdapter mouseAdapter = new DockingMouseAdapter();
  /**
   * 
   */
  private Queue<DetachableComponentDescriptor> pendingQueue = new ConcurrentLinkedQueue<>();

  /**
   * Get the docking manager for the JVM.
   * @return an instance of {@link DockingManager}.
   */
  public static DockingManager getInstance() {
    return instance;
  }

  /**
   * Prevent instantiation from another class.
   */
  private DockingManager() {
  }

  /**
   * Set the main view for the application.
   * @param frame the {@link Frame} to set as the main view.
   * @param container the option container for the view.
   */
  public void setMainView(final Frame frame, final OptionContainer container) {
    if (viewMap.get(INITIAL_VIEW) != null) throw new IllegalStateException("the main view is already set");
    final ViewDescriptor view = new ViewDescriptor(frame, container);
    viewMap.put(INITIAL_VIEW, view);
    while (!pendingQueue.isEmpty()) view.addComponent(pendingQueue.poll());
  }

  /**
   * Add a new component to the map of detachable components.
   * @param element the component to add.
   * @param listenerComponent the UI component which has the mouse listener.
   */
  public void register(final OptionElement element, final Component listenerComponent) {
    final TabbedPaneOption parent = (TabbedPaneOption) element.getParent();
    final Component tabComponent = parent.getTabComponent(element);
    final DetachableComponentDescriptor desc = new DetachableComponentDescriptor(element, listenerComponent, tabComponent);
    componentMap.put(element.getUIComponent(), desc);
    listenerToComponentMap.put(listenerComponent, element.getUIComponent());
    final ViewDescriptor view = viewMap.get(INITIAL_VIEW);
    if (view != null) view.addComponent(desc);
    else pendingQueue.offer(desc);
  }

  /**
   * Add a new component to the map of detachable components.
   * @param element the component to add.
   * @param listenerComponent the UI component which has the mouse listener.
   */
  public void update(final OptionElement element, final Component listenerComponent) {
    final DetachableComponentDescriptor desc = getComponent(element.getUIComponent());
    listenerToComponentMap.remove(desc.getListenerComponent());
    desc.setListenerComponent(listenerComponent);
    listenerToComponentMap.put(listenerComponent, element.getUIComponent());
  }

  /**
   * Add a new component to the map of detachable components.
   * @param element the component to add.
   * @return <code>true</code> if the component is already registered, <code>false</code> otherwise.
   */
  public boolean isRegistered(final OptionElement element) {
    return componentMap.containsKey(element.getUIComponent());
  }

  /**
   * Attach a new component to the specvified view.
   * @param element the component to qttqch to the viez.
   * @param viewId the id of the viez to zhcih to attach the co;ponent.
   */
  public void attach(final OptionElement element, final String viewId) {
    final ViewDescriptor newView = viewMap.get(viewId);
    if (newView == null) throw new IllegalArgumentException("the view '" + viewId + "' does not exist");
    final DetachableComponentDescriptor desc = componentMap.get(element.getUIComponent());
    if (desc == null) throw new IllegalArgumentException("the component '" + element + "' could not be found");
    final TabbedPaneOption targetContainer = (TabbedPaneOption) (INITIAL_VIEW.equals(viewId) ? desc.getInitialContainer() : newView.getContainer());
    final ViewDescriptor oldView = viewMap.get(desc.getViewId());
    desc.getCurrentContainer().remove(element);
    targetContainer.add(element, desc.getTabComponent());
    desc.setCurrentContainer(targetContainer);
    desc.setViewId(viewId);
    oldView.removeComponent(desc);
    newView.addComponent(desc);
  }

  /**
   * Create a new view.
   * @return the id of the view.
   */
  public String createView() {
    final String id = VIEW_PREFIX + VIEW_SEQ.incrementAndGet();
    final JDialog dialog = new JDialog(OptionsHandler.getMainWindow(), id, false);
    dialog.setIconImage(GuiUtils.loadIcon(GuiUtils.JPPF_ICON).getImage());
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    dialog.addWindowListener(windowAdapter);
    final TabbedPaneOption container = new TabbedPaneOption();
    container.setName(id);
    container.createUI();
    dialog.getContentPane().add(container.getUIComponent(), BorderLayout.CENTER);
    final ViewDescriptor view = new ViewDescriptor(dialog, container);
    viewMap.put(id, view);
    return id;
  }

  /**
   * Get the view with the specified id.
   * @param viewId the id of the view to find.
   * @return a {@link ViewDescriptor} object.
   */
  public ViewDescriptor getView(final String viewId) {
    return viewMap.get(viewId);
  }

  /**
   * Get the detachable component for the specified AWT component.
   * @param comp the component for which to find a descriptor.
   * @return a {@link DetachableComponentDescriptor} object.
   */
  public DetachableComponentDescriptor getComponent(final Component comp) {
    return componentMap.get(comp);
  }

  /**
   * Get the detachable component for the specified AWT component on which the mouse listener is registered.
   * @param comp the component for which to find a descriptor.
   * @return a {@link DetachableComponentDescriptor} object.
   */
  public DetachableComponentDescriptor getComponentFromListenerComp(final Component comp) {
    final Component temp = listenerToComponentMap.get(comp);
    return temp == null ? null : componentMap.get(temp);
  }

  /**
   * Get the id of the view that contains the specified container.
   * @param container the container tolook for.
   * @return the id of the view which has the container as main container.
   */
  public String getViewIdForContainer(final OptionContainer container) {
    for (Map.Entry<String, ViewDescriptor> entry: viewMap.entrySet()) {
      if (entry.getValue().getContainer() == container) return entry.getKey();
    }
    return INITIAL_VIEW;
  }

  /**
   * Get the mouse listener for the popup menu on right-click.
   * @return an {@link DockingMouseAdapter} object.
   */
  public DockingMouseAdapter getMouseAdapter() {
    return mouseAdapter;
  }

  /**
   * Remove the specified view, and move the tabs it contains to their initial containers.
   * @param viewId the id of the view to remove.
   */
  private void removeView(final String viewId) {
    final ViewDescriptor view = viewMap.get(viewId);
    final Set<DetachableComponentDescriptor> set = new HashSet<>(view.getComponents());
    for (DetachableComponentDescriptor desc: set) dockToInitialContainer(desc.getComponent().getUIComponent());
    view.getFrame().setVisible(false);
    view.getFrame().dispose();
    viewMap.remove(viewId);
  }

  /**
   * Dock a component to its initial container.
   * @param comp the component to move.
   */
  public void dockToInitialContainer(final Component comp) {
    final DetachableComponentDescriptor desc = componentMap.get(comp);
    final OptionElement element = desc.getComponent();
    desc.getCurrentContainer().remove(element);
    ((TabbedPaneOption) desc.getInitialContainer()).add(element, desc.getTabComponent());
    desc.setCurrentContainer(desc.getInitialContainer());
    final ViewDescriptor oldView = viewMap.get(desc.getViewId());
    oldView.removeComponent(desc);
    final String id = getViewIdForContainer(desc.getInitialContainer());
    desc.setViewId(id);
    viewMap.get(id).addComponent(desc);
  }

  /**
   * Get a localized message given its unique name and the current locale.
   * @param message the unique name of the localized message.
   * @return a message in the current locale, or the default locale
   * if the localization for the current locale is not found.
   */
  static String localize(final String message) {
    return LocalizationUtils.getLocalized(I18N_BASE, message);
  }

  /**
   * Get the Mapping of view ids to the corresponding UI frame.
   * @return a map of string ids to {@link ViewDescriptor} instances.
   */
  public Map<String, ViewDescriptor> getViewMap() {
    return viewMap;
  }

  /**
   * Search all esiting views for an eklement with the specified name.
   * @param name the name of the element to find.
   * @return an {@link OptionElement} instance representing the element.
   */
  public OptionElement findFirstElementWithName(final String name) {
    for (final ViewDescriptor view: viewMap.values()) {
      final OptionContainer cont = view.getContainer();
      if (cont == null) continue;
      final OptionElement elt = cont.findFirstWithName(name);
      if (elt != null) return elt;
    }
    return null;
  }
}
