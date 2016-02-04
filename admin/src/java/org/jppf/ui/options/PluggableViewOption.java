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
package org.jppf.ui.options;

import org.jppf.ui.plugin.PluggableView;

/**
 * An option that embeds a user-ddefined pluggable view.
 * @author Laurent Cohen
 * @exclude
 */
public class PluggableViewOption extends AbstractOption {
  /**
   * The pluggable view which provides the UI component to embed.
   */
  protected final PluggableView pluggableView;

  /**
   * Construct this option from the specified pluggable view.
   * @param pluggableView the pluggable view which provides the UI component to embed.
   */
  public PluggableViewOption(final PluggableView pluggableView) {
    this.pluggableView = pluggableView;
  }

  @Override
  public void createUI() {
    UIComponent = pluggableView.getUIComponent();
  }

  @Override
  protected void setupValueChangeNotifications() {
  }


  @Override
  public void setEnabled(final boolean enabled) {
  }
}
