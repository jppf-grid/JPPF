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

import java.awt.event.MouseAdapter;

import javax.swing.*;

import org.slf4j.*;

import net.miginfocom.swing.MigLayout;

/**
 * An option that displays a UI component created through a Java class.
 * @author Laurent Cohen
 */
public class JavaOption extends AbstractOption {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JavaOption.class);
  /**
   * The fully qualified class name of the UI component to instantiate.
   */
  protected String className = null;
  /**
   * Name of the mouseListener to set on this element.
   */
  protected String mouseListenerClassName = null;
  /**
   * The instance of the class that is created.
   */
  protected JComponent instance;

  /**
   * Constructor provided as a convenience to facilitate the creation of
   * option elements through reflexion.
   */
  public JavaOption() {
  }

  @Override
  public void createUI() {
    try {
      final JComponent comp = instance = (JComponent) Class.forName(getClassName()).newInstance();
      final JScrollPane scrollPane = scrollable ? createScrollPane(comp) : null;
      if (comp instanceof JPanel) {
        final JPanel panel = (JPanel) comp;
        if (!(panel.getLayout() instanceof MigLayout) && (layoutConstraints != null) && !"".equals(layoutConstraints)) panel.setLayout(new MigLayout(layoutConstraints));
      }
      if (mouseListenerClassName != null) {
        final JavaOptionMouseListener ml = (JavaOptionMouseListener) Class.forName(mouseListenerClassName).newInstance();
        ml.setOption(this);
        comp.addMouseListener(ml);
      }
      if (scrollPane != null) {
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        UIComponent = scrollPane;
      } else UIComponent = comp;
    } catch (final Throwable e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Propagate the state changes of the underlying component to the listeners to this component.
   * @see org.jppf.ui.options.AbstractOption#setupValueChangeNotifications()
   */
  @Override
  protected void setupValueChangeNotifications() {
  }

  /**
   * Enable or disable this option.
   * @param enabled true to enable this option, false to disable it.
   * @see org.jppf.ui.options.Option#setEnabled(boolean)
   */
  @Override
  public void setEnabled(final boolean enabled) {
  }

  /**
   * Get the fully qualified class name of the UI component to instantiate.
   * @return the class name as a string.
   */
  public synchronized String getClassName() {
    return className;
  }

  /**
   * Set the fully qualified class name of the UI component to instantiate.
   * @param className the class name as a string.
   */
  public synchronized void setClassName(final String className) {
    this.className = className;
  }

  /**
   * Abstract superclass for mouse listeners set on this type of option.
   */
  public abstract static class JavaOptionMouseListener extends MouseAdapter {
    /**
     * The option on which this listener is set.
     */
    protected JavaOption option = null;

    /**
     * Get the option on which this listener is set.
     * @return a <code>JavaOption</code> instance.
     */
    public JavaOption getOption() {
      return option;
    }

    /**
     * Set the option on which this listener is set.
     * @param option a <code>JavaOption</code> instance.
     */
    public void setOption(final JavaOption option) {
      this.option = option;
    }
  }

  /**
   * Get the class name of the mouseListener to set on this element.
   * @return the class name as a string.
   */
  public String getMouseListenerClassName() {
    return mouseListenerClassName;
  }

  /**
   * Set the class name of the mouseListener to set on this element.
   * @param mouseListenerClassName the class name as a string.
   */
  public void setMouseListenerClassName(final String mouseListenerClassName) {
    this.mouseListenerClassName = mouseListenerClassName;
  }

  /**
   * Get the instance of the class that is created.
   * @return a {@link JComponent}.
   */
  public JComponent getInstance() {
    return instance;
  }
}
