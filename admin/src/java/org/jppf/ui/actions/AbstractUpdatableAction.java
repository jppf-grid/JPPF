/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

import java.awt.Point;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.jppf.ui.options.OptionElement;
import org.jppf.ui.options.event.ValueChangeEvent;
import org.jppf.ui.utils.GuiUtils;
import org.jppf.utils.LocalizationUtils;

/**
 * Abstract superclass for actions used in toolbars or popup menus.
 * @author Laurent Cohen
 */
public abstract class AbstractUpdatableAction extends AbstractAction implements UpdatableAction {
  /**
   * The base location for internationalized messages.
   */
  protected String BASE = null;
  /**
   * The list of selected elements.
   */
  protected List<Object> selectedElements = new LinkedList<>();
  /**
   * Location at which to display any window or dialog created by this action.
   */
  protected Point location = new Point(10, 10);

  /**
   * Get the location at which to display any window or dialog created by this action.
   * @return a <code>Point</code> instance.
   */
  public Point getLocation() {
    return location;
  }

  /**
   * Set the location at which to display any window or dialog created by this action.
   * @param location a <code>Point</code> instance.
   */
  public void setLocation(final Point location) {
    this.location = location;
  }

  /**
   * Update this action's enabled state based on a list of selected elements.
   * @param selectedElements a list of objects.
   */
  @Override
  public void updateState(final List<Object> selectedElements) {
    this.selectedElements = selectedElements;
  }

  /**
   * Method called when the action is triggered.
   * @param event the event encapsulating the source of the event.
   */
  @Override
  public void valueChanged(final ValueChangeEvent event) {
    actionPerformed(new ActionEvent(event.getSource(), ActionEvent.ACTION_PERFORMED, ""));
  }

  /**
   * Set the icon for this action using the specified image file name.
   * @param name  the name of the icon image file.
   */
  protected void setupIcon(final String name) {
    if (name != null) putValue(Action.SMALL_ICON, GuiUtils.loadIcon(name));
  }

  /**
   * Set the action name and tooltip text.
   * @param name the key to find the name and tooltip in the localized resource bundles.
   */
  protected void setupNameAndTooltip(final String name) {
    putValue(NAME, localize(name + ".label"));
    putValue(SHORT_DESCRIPTION, localize(name + ".tooltip"));
  }

  /**
   * Set the action tooltip text.
   * @param name the key to find the tooltip in the localized resource bundles.
   */
  protected void setupTooltip(final String name) {
    putValue(SHORT_DESCRIPTION, localize(name + ".tooltip"));
  }

  /**
   * Get a localized message given its unique name and the current locale.
   * @param message the unique name of the localized message.
   * @return a message in the current locale, or the default locale if the localization for the current locale is not found.
   */
  protected String localize(final String message) {
    return LocalizationUtils.getLocalized(BASE, message);
  }

  /**
   * Execute the specified runnable in a new thread.
   * The thread name is <code>this.getClass().getSimpleName()</code>.
   * @param r the <code>Runnable</code> to execute.
   */
  protected void runAction(final Runnable r) {
    GuiUtils.runAction(r, getClass().getSimpleName());
  }

  /**
   * Associate a keyboard virtual key with an action.
   * @param comp the {@link JComponent} whose {@link InputMap} and {@link ActionMap} this method modifies.
   * @param vkey the virtual key code to associate with the action, built from
   * one of the values in {@link KeyEvent}, for instance: <code>KeyEvent.VK_ENTER</code>.
   * @param action the action to trigger upon pressing the keyboard key.
   * @param actionKey a key to use int he {@link ActionMap}.
   */
  protected void setKeyAction(final JComponent comp, final KeyStroke vkey, final Action action, final Object actionKey) {
    InputMap inputMap = comp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    inputMap.put(vkey, actionKey);
    ActionMap map = comp.getActionMap();
    map.put(actionKey, action);
  }

  /**
   * Associate 'ok' and 'cancel' actions with 'Enter' and 'Esc' keys, repsectively.
   * @param option contains the {@link JComponent} whose {@link InputMap} and {@link ActionMap} this method modifies.
   * @param okAction the action to execute upon pressing 'Enter'.
   * @param cancelAction the action to execute upon pressing 'Esc'.
   */
  protected void setOkCancelKeys(final OptionElement option, final Action okAction, final Action cancelAction) {
    setOkCancelKeys(option.getUIComponent(), okAction, cancelAction);
  }

  /**
   * Associate 'ok' and 'cancel' actions with 'Enter' and 'Esc' keys, repsectively.
   * @param option contains the {@link JComponent} whose {@link InputMap} and {@link ActionMap} this method modifies.
   * @param okAction the action to execute upon pressing 'Enter'.
   * @param cancelAction the action to execute upon pressing 'Esc'.
   */
  protected void setOkCancelKeys(final JComponent option, final Action okAction, final Action cancelAction) {
    if (okAction != null) setKeyAction(option, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), okAction, "ok");
    if (cancelAction != null) setKeyAction(option, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), cancelAction, "cancel");
  }
}
