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

/**
 *
 * @author Laurent Cohen
 */
public abstract class AbstractSelectionHandler implements SelectionHandler {
  /**
   * The table tree to which the selection applies.
   */
  protected JPPFTableTree tableTree;
  /**
   * The filter to use.
   */
  protected SelectionHandler.Filter filter;

  @Override
  public JPPFTableTree getTableTree() {
    return null;
  }

  @Override
  public SelectionHandler setFilter(final SelectionHandler.Filter filter) {
    this.filter = filter;
    return this;
  }

  /**
   * Set the table tree to which the selection applies.
   * @param tableTree a {@link JPPFTableTree} instance.
   * @exclude
   */
  public void setTableTree(final JPPFTableTree tableTree) {
    this.tableTree = tableTree;
  }

  @Override
  public void select(final String uuid) {
  }

  @Override
  public void unselect(final String uuid) {
  }
}
