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

package org.jppf.ui.plugin;

import java.util.*;

/**
 *
 * @author Laurent Cohen
 */
public class PredefinedView {
  /**
   * The name of this view.
   */
  private final String name;
  /**
   * The name of the container view that holds this view, if any.
   */
  private final String containerName;
  /**
   * The position container-wise at which this view is added initially.
   */
  private final int position;
  /**
   * The fully qualified class name of the root UI compoent of this view (for user-defined views only).
   */
  private String className;
  /**
   * Whether this view is enabled.
   */
  private boolean enabled;
  /**
   * 
   */
  private static Map<String, PredefinedView> pluggableViewMap = initViewMap();

  /**
   * Initialize this pluggable view with the specified name.
   * @param name the name of this view.
   */
  public PredefinedView(final String name) {
    this(name, null, -1, null, true);
  }

  /**
   * Initialize this pluggable view with the specified parameters.
   * @param name the name of this view.
   * @param containerName the name of the container view that holds this view, if any.
   */
  public PredefinedView(final String name, final String containerName) {
    this(name, containerName, -1, null, true);
  }

  /**
   * Initialize this pluggable view with the specified parameters.
   * @param name the name of this view.
   * @param containerName the name of the container view that holds this view, if any.
   * @param position the position container-wise at which this view is added initially.
   * @param className the fully qualified class name of the root UI compoent of this view (for user-defined vies only).
   * @param enabled whether this view is enabled.
   */
  public PredefinedView(final String name, final String containerName, final int position, final String className, final boolean enabled) {
    this.name = name;
    this.containerName = containerName;
    this.position = position;
    this.className = className;
    this.enabled = enabled;
  }

  /**
   * Initialize the pluggable view map.
   * @return a mapping of predefined views to their name.
   */
  private static Map<String, PredefinedView> initViewMap() {
    Map<String, PredefinedView> map = new HashMap<>();
    String[] predefinedNames = { "ServerChooser", "StatusBar", "Topology", "TopologyTree", "TopologyGraph", "TopologyHealth",
      "JobData", "ServerStats", "Charts", "ServerCharts", "ChartsConfiguration", "LoadBalancing" };
    for (String name: predefinedNames) map.put(name, new PredefinedView(name));
    return map;
  }
}
