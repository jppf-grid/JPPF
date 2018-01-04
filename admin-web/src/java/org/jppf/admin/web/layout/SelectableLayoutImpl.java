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

import java.util.*;

import org.jppf.admin.web.JPPFWebSession;
import org.jppf.admin.web.settings.UserSettings;
import org.jppf.ui.monitoring.LocalizedListItem;
import org.jppf.utils.StringUtils;

/**
 *
 * @author Laurent Cohen
 */
public class SelectableLayoutImpl implements SelectableLayout {
  /**
   * List of all available items.
   */
  private final List<LocalizedListItem> allItems;
  /**
   * List of visible items.
   */
  private List<LocalizedListItem> visibleItems;
  /**
   * Name of the property used when saving to / loading from the user settings.
   */
  private final String propertyName;

  /**
   * @param allItems the list of all available items.
   * @param propertyName name of the property used when saving to / loading from the user settings.
   */
  public SelectableLayoutImpl(final List<LocalizedListItem> allItems, final String propertyName) {
    this.allItems = allItems;
    this.propertyName = propertyName;
  }

  @Override
  public List<LocalizedListItem> getVisibleItems() {
    if (visibleItems == null) {
      final UserSettings settings = JPPFWebSession.get().getUserSettings();
      if (settings != null) { 
        final String s = settings.getProperties().getString(propertyName);
        if ((s != null) && !s.trim().isEmpty()) {
          final int[] indices = StringUtils.parseIntValues(s);
          visibleItems = new ArrayList<>(indices.length);
          for (int i=0; i<indices.length; i++) {
            for (final LocalizedListItem item: getAllItems()) {
              if (item.index == indices[i]) {
                visibleItems.add(item);
                break;
              }
            }
          } 
        }
      }
    }
    if ((visibleItems == null) || visibleItems.isEmpty()) visibleItems = new ArrayList<>(getAllItems());
    return visibleItems;
  }

  @Override
  public List<LocalizedListItem> getAllItems() {
    return allItems;
  }

  @Override
  public void setVisibleItems(final List<LocalizedListItem> items) {
    if (items == null) return;
    final int[] indices = new int[items.size()];
    for (int i=0; i<indices.length; i++) indices[i] = items.get(i).index;
    final UserSettings settings = JPPFWebSession.get().getUserSettings();
    settings.getProperties().setString(propertyName, StringUtils.buildString(indices));
    settings.save();
  }
}
