/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
import java.awt.event.ActionEvent;
import java.util.*;

import javax.swing.*;

import org.jppf.ui.options.event.ValueChangeEvent;
import org.jppf.ui.utils.GuiUtils;
import org.jppf.utils.LocalizationUtils;

/**
 * Abstract superclass for actions used in toolbars or popup menus.
 * @author Laurent Cohen
 */
public abstract class AbstractUpdatableAction extends AbstractAction implements UpdatableAction
{
  /**
   * The base location for internationalized messages.
   */
  protected String BASE = null;
  /**
   * The list of selected elements.
   */
  protected List<Object> selectedElements = new LinkedList<Object>();
  /**
   * Location at which to display any window or dialog created by this action.
   */
  protected Point location = new Point(10, 10);

  /**
   * Get the location at which to display any window or dialog created by this action.
   * @return a <code>Point</code> instance.
   */
  public Point getLocation()
  {
    return location;
  }

  /**
   * Set the location at which to display any window or dialog created by this action.
   * @param location a <code>Point</code> instance.
   */
  public void setLocation(final Point location)
  {
    this.location = location;
  }

  /**
   * Update this action's enabled state based on a list of selected elements.
   * @param selectedElements a list of objects.
   * @see org.jppf.ui.actions.UpdatableAction#updateState(java.util.List)
   */
  @Override
  public void updateState(final List<Object> selectedElements)
  {
    this.selectedElements = selectedElements;
  }

  /**
   * Method called when the action is triggered.
   * @param event the event encapsulating the source of the event.
   * @see org.jppf.ui.options.event.ValueChangeListener#valueChanged(org.jppf.ui.options.event.ValueChangeEvent)
   */
  @Override
  public void valueChanged(final ValueChangeEvent event)
  {
    actionPerformed(new ActionEvent(event.getSource(), ActionEvent.ACTION_PERFORMED, ""));
  }

  /**
   * Set the icon for this action using the specified image file name.
   * @param name  the name of the icon image file.
   */
  protected void setupIcon(final String name)
  {
    if (name != null) putValue(Action.SMALL_ICON, GuiUtils.loadIcon(name));
  }

  /**
   * Set the action name and tooltip text.
   * @param name the key to find the name and tooltip in the localized resource bundles.
   */
  protected void setupNameAndTooltip(final String name)
  {
    putValue(NAME, localize(name + ".label"));
    putValue(SHORT_DESCRIPTION, localize(name + ".tooltip"));
  }

  /**
   * Set the action tooltip text.
   * @param name the key to find the tooltip in the localized resource bundles.
   */
  protected void setupTooltip(final String name)
  {
    putValue(SHORT_DESCRIPTION, localize(name + ".tooltip"));
  }

  /**
   * Get a localized message given its unique name and the current locale.
   * @param message the unique name of the localized message.
   * @return a message in the current locale, or the default locale
   * if the localization for the current locale is not found.
   */
  protected String localize(final String message)
  {
    return LocalizationUtils.getLocalized(BASE, message);
  }

  /**
   * Execute the specified runnable in a new thread.
   * The thread name is <code>this.getClass().getSimpleName()</code>.
   * @param r the <code>Runnable</code> to execute.
   */
  protected void runAction(final Runnable r)
  {
    new Thread(r, getClass().getSimpleName()).start();
  }
}
