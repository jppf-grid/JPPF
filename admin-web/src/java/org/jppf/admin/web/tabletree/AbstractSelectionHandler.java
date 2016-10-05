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

package org.jppf.admin.web.tabletree;

import java.util.*;

import org.jppf.ui.treetable.TreeNodeFilter;

/**
 *
 * @author Laurent Cohen
 */
public abstract class AbstractSelectionHandler implements SelectionHandler {
  /**
   * The filter to use.
   */
  protected transient TreeNodeFilter filter;
  /**
   * The listeners to this selection handler.
   */
  protected transient final List<SelectionListener> listeners = new ArrayList<>();

  @Override
  public TreeNodeFilter getFilter() {
    return filter;
  }

  @Override
  public SelectionHandler setFilter(final TreeNodeFilter filter) {
    this.filter = filter;
    return this;
  }

  @Override
  public void select(final String uuid) {
  }

  @Override
  public void unselect(final String uuid) {
  }

  @Override
  public void addSelectionListener(final SelectionListener listener) {
    if (listener != null) listeners.add(listener);
  }

  @Override
  public void removeSelectionListener(final SelectionListener listener) {
    if (listener != null) listeners.remove(listener);
  }

  @Override
  public void cleanup() {
    clearSelection();
    listeners.clear();
  }
}
