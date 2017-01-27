/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.admin.web.layout;

import java.util.List;

import org.jppf.ui.monitoring.LocalizedListItem;

/**
 * 
 * @author Laurent Cohen
 */
public interface SelectableLayout {
  /**
   * @return the list of visible columns/layout items in the page. 
   */
  List<LocalizedListItem> getVisibleItems();

  /**
   * @return the list of all available columns/layout items in the page. 
   */
  List<LocalizedListItem> getAllItems();

  /**
   * Set the list of visible items.
   * @param items the list of all available columns/layout items in the page. 
   */
  void setVisibleItems(List<LocalizedListItem> items);
}
