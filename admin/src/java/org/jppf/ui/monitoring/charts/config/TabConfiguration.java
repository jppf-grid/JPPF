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
package org.jppf.ui.monitoring.charts.config;

import java.util.*;

import javax.swing.JPanel;

/**
 * This class manages the data related to the charts within a tab.
 * @author Laurent Cohen
 */
public class TabConfiguration
{
  /**
   * The name of the tab.
   */
  String name = null;
  /**
   * The panel that contains the configured charts.
   */
  public JPanel panel = null;
  /**
   * The list of chart configurations contained in this tab.
   */
  public List<ChartConfiguration> configs = new ArrayList<>();
  /**
   * The position of this tab in the list of tabs.
   */
  public int position = -1;

  /**
   * Create a tab configuration with uninitialized parameters.
   */
  public TabConfiguration()
  {
  }

  /**
   * Create a tab configuration with a specified name and position.
   * @param name the name of the tab to create.
   * @param position the position of the tab in the list of tabs.
   */
  public TabConfiguration(final String name, final int position)
  {
    this.name = name;
    this.position = position;
  }

  /**
   * Get a string representation of this TabConfiguration
   * @return a string with the tab name.
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    return name == null ? "unnamed tab" : name;
  }
}
