/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.ui.plugin;

import java.util.*;
import java.util.regex.*;

import javax.swing.JTabbedPane;

import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.options.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 * @exclude
 */
public class PluggableViewHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(PluggableViewHandler.class);
  /**
   * Mapping of view names to their descritpor.
   */
  private final Map<String, PluggableViewDescriptor> viewMap = new HashMap<>();

  /**
   * Add the specified view with the specified name.
   * @param name the name of the view.
   * @param elt the view's component tree.
   * @return {@code true} if the view was added successfully, {@code false} otherwise.
   */
  public boolean addView(final String name, final OptionElement elt) {
    PluggableViewDescriptor desc = new PluggableViewDescriptor(name, elt, null, -1);
    viewMap.put(name, desc);
    return true;
  }

  /**
   * Add the specified view with the specified name.
   * @param name the name of the view.
   * @return {@code true} if the view was added successfully, {@code false} otherwise.
   */
  private boolean addViewFromConfig(final String name) {
    List<String> errors = new ArrayList<>();
    TypedProperties config = JPPFConfiguration.getProperties();
    String prefix = String.format("jppf.admin.console.view.%s", name);
    boolean active = config.getBoolean(prefix + ".enabled", true);
    if (!active) return false;
    String className = config.getString(prefix + ".class", null);
    if ((className == null) || "".equals(className)) errors.add(String.format("no class name defined for pluggable view '%s'", name));
    String containerName = config.getString(prefix + ".addto", null);
    if ((containerName == null) || "".equals(containerName)) errors.add(String.format("no 'addto' property defined for pluggable view '%s'", name));
    TabbedPaneOption container = null;
    PluggableViewDescriptor containerDesc = viewMap.get(containerName);
    if (containerDesc == null) errors.add(String.format("container '%s' for pluggable view '%s' could not be found", containerName, name));
    else container = (TabbedPaneOption) containerDesc.getOption();

    PluggableViewOption option = null;
    if ((container != null) && (className != null) && !"".equals(className)) {
      Class<?> clazz = null;
      try {
        clazz = Class.forName(className);
      } catch (Exception e) {
        errors.add(String.format("the class '%s' for pluggable view '%s' could not be found%n%s", className, name, ExceptionUtils.getStackTrace(e)));
      }
      if (clazz != null) {
        PluggableView view = null;
        try {
          view = (PluggableView) clazz.newInstance();
        } catch (Exception e) {
          errors.add(String.format("the class '%s' for pluggable view '%s' could not be instantiated%n%s", className, name, ExceptionUtils.getStackTrace(e)));
        }
        if (view != null) {
          view.setTopologyManager(StatsHandler.getInstance().getTopologyManager());
          view.setJobMonitor(StatsHandler.getInstance().getJobMonitor());
          option = new PluggableViewOption(view);
          option.setName(name);
          String title = config.getString(prefix + ".title", name);
          if ((title == null) || "".equals(title.trim())) title = name;
          option.setLabel(title);
          String iconPath = config.getString(prefix + ".icon", null);
          if (iconPath != null) option.setIconPath(iconPath);
          option.setDetachable(true);
          option.createUI();
          int pos = config.getInt(prefix + ".position", -1);
          try {
            if (pos < 0) pos = container.getChildren().size();
            container.add(option, pos);
            JTabbedPane pane = (JTabbedPane) container.getUIComponent();
            if (config.getBoolean(prefix + ".autoselect", false)) pane.setSelectedIndex(pos);
            if (log.isDebugEnabled()) log.debug("successfully added pluggable view '{}'", name);
            return true;
          } catch (Exception e) {
            if (pos >= 0) errors.add(String.format("the pluggable view '%s' could not be added to the container '%s' at position %d%n%s", className, name, pos, ExceptionUtils.getStackTrace(e)));
            else errors.add(String.format("the pluggable view '%s' could not be added to the container '%s'%n%s", className, name, ExceptionUtils.getStackTrace(e)));
          }
        }
      }
    }
    if (!errors.isEmpty()) logErrors(name, errors);
    return false;
  }

  /**
   * Discover and install the pluggable views from the configuration.
   */
  public void installViews() {
    TypedProperties fullConfig = JPPFConfiguration.getProperties();
    final Pattern pattern = Pattern.compile("jppf\\.admin\\.console\\.view\\.(.+)\\..+");
    final Set<String> names = new HashSet<>();
    fullConfig.filter(new TypedProperties.Filter() {
      @Override
      public boolean accepts(final String name, final String value) {
        Matcher matcher = pattern.matcher(name);
        if (matcher.matches()) {
          String s = matcher.group(1);
          if ((s != null) && !names.contains(s)) {
            names.add(s);
            return true;
          }
        }
        return false;
      }
    });
    for (String name: names) addViewFromConfig(name);
  }

  /**
   * Log the errors reproted while trying to add a custom view.
   * @param viewName the name of the vies.
   * @param errors the list of reported errors.
   */
  private void logErrors(final String viewName, final List<String> errors) {
    String s = errors.size() > 1 ? "s" : "";
    StringBuilder sb = new StringBuilder("Error").append(s).append(" reported while creating the pluggable view '").append(viewName).append("':");
    for (String error: errors) sb.append("\n").append(error);
    log.warn(sb.toString());
  }
}
