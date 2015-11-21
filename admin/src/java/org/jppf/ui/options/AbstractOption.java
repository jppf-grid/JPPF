/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import java.util.ArrayList;

import javax.swing.*;

import net.miginfocom.swing.MigLayout;

import org.jppf.ui.options.event.*;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * Default abstract implementation of the <code>Option</code> interface.
 * @author Laurent Cohen
 */
public abstract class AbstractOption extends AbstractOptionElement implements Option
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractOption.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The value of this option.
   */
  protected Object value = null;
  /**
   * List of listeners that are notified when the value of this option changes.
   */
  protected java.util.List<ValueChangeListener> listeners = new ArrayList<>();
  /**
   * Determines whether the value of this option should be saved in the user preferences.
   */
  protected boolean persistent = false;

  /**
   * Constructor provided as a convenience to facilitate the creation of
   * option elements through reflexion.
   */
  public AbstractOption()
  {
  }

  /**
   * The value of this option.
   * @return the value as an <code>Object</code> instance.
   * @see org.jppf.ui.options.Option#getValue()
   */
  @Override
  public Object getValue()
  {
    return value;
  }

  /**
   * Set the value of this option.
   * @param value the value as an <code>Object</code> instance.
   */
  public void setValue(final Object value)
  {
    this.value = value;
  }

  /**
   * Add a value change listener to this option's list of listeners.
   * @param listener the listener to add to the list.
   */
  public void addValueChangeListener(final ValueChangeListener listener)
  {
    listeners.add(listener);
  }

  /**
   * Remove a value change listener from this option's list of listeners.
   * @param listener the listener to remove from the list.
   */
  public void removeValueChangeListener(final ValueChangeListener listener)
  {
    listeners.remove(listener);
  }

  /**
   * Notify all registered listeners that the value of this option has changed.
   */
  public void fireValueChanged()
  {
    if (!eventsEnabled) return;
    if (debugEnabled) log.debug("firing event for " + this);
    for (ValueChangeListener listener: listeners)
    {
      listener.valueChanged(new ValueChangeEvent(this));
    }
  }

  /**
   * Concrete subclasses must implement this method, so they can properly forward
   * changes in the value edited by the underlying UI component/editor.
   */
  protected abstract void setupValueChangeNotifications();

  /**
   * Layout 2 components according to the specified layout constraints.
   * @param comp1 the first component to layout.
   * @param comp2 the second component to layout.
   * @return a <code>JPanel</code> instance, enclosing the 2 components plus the filler.
   */
  protected JPanel layoutComponents(final JComponent comp1, final JComponent comp2)
  {
    JPanel panel = new JPanel();
    String s = getLayoutConstraints().trim();
    MigLayout mig = new MigLayout(s);
    panel.setLayout(mig);
    if ((comp1 == null) && (comp2 == null)) return panel;
    if ((comp1 != null) && (comp2 != null))
    {
      panel.add(comp1, "align left, growx 0, pushx");
      panel.add(comp2, "gap rel, grow");
    }
    else
    {
      panel.add(comp1 != null ? comp1 : comp2);
    }
    return panel;
  }

  /**
   * Layout 2 components according to the specified layout constraints.
   * @param comp1 the first component to layout.
   * @param constraint1 the layout constraint for the 1st component.
   * @param comp2 the second component to layout.
   * @param constraint2 the layout constraint for the 2nd component.
   * @return a <code>JPanel</code> instance, enclosing the 2 components plus the filler.
   */
  protected JPanel layoutComponents(final JComponent comp1, final String constraint1, final JComponent comp2, final String constraint2)
  {
    JPanel panel = new JPanel();
    String s = getLayoutConstraints().trim();
    MigLayout mig = new MigLayout(s);
    panel.setLayout(mig);
    if ((comp1 == null) && (comp2 == null)) return panel;
    if ((comp1 != null) && (comp2 != null))
    {
      panel.add(comp1, constraint1);
      panel.add(comp2, constraint2);
    }
    else
    {
      panel.add(comp1 != null ? comp1 : comp2);
    }
    return panel;
  }

  /**
   * Determine whether the value of this option should be saved in the user preferences.
   * @return true if the value should be saved, false otherwise.
   * @see org.jppf.ui.options.Option#isPersistent()
   */
  @Override
  public boolean isPersistent()
  {
    return persistent;
  }

  /**
   * Set whether the value of this option should be saved in the user preferences.
   * @param persistent true if the value should be saved, false otherwise.
   */
  public void setPersistent(final boolean persistent)
  {
    this.persistent = persistent;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append('[').append((this instanceof OptionContainer) ? "Page" : "Option").append(" : ");
    sb.append(getClass().getName()).append("] ");
    sb.append("name=").append(name);
    sb.append("; label=").append(label);
    sb.append("; path=").append(getStringPath());
    sb.append("; value=").append(getValue());
    return sb.toString();
  }
}
