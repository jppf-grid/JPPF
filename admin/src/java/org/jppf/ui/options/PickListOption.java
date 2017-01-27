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

package org.jppf.ui.options;

import java.util.List;

import org.jppf.ui.picklist.*;

/**
 * A pick-list option.
 * {@link #getValue()} returns the list of picked items.
 * @author Laurent Cohen
 */
public class PickListOption extends AbstractOption {
  /**
   * The pick list UI component.
   */
  private PickList<Object> pickList;
  /**
   *
   */
  private PickListListener<Object> pickListListener;

  @Override
  public void createUI() {
    pickList = new PickList<>();
    setUIComponent(pickList);
  }

  @Override
  public void setEnabled(final boolean enabled) {
  }

  @Override
  protected void setupValueChangeNotifications() {
    pickListListener = new PickListListener<Object>() {
      @Override
      public void itemsAdded(final PickListEvent<Object> event) {
        setValue(event.getPickList().getPickedItems());
      }

      @Override
      public void itemsRemoved(final PickListEvent<Object> event) {
        setValue(event.getPickList().getPickedItems());
      }
    };
    pickList.addPickListListener(pickListListener);
  }

  /**
   * Populate the pocik list.
   * @param allItems the list of all possible items.
   * @param pickedItems the selected items.
   */
  public void populate(final List<Object> allItems, final List<Object> pickedItems) {
    pickList.resetItems(allItems, pickedItems);
    if ((pickListListener == null) || !isEventsEnabled()) {
      setValue(pickedItems);
    }
  }

  /**
   * Get the pick list.
   * @return a {@link PickList} object.
   */
  public PickList<Object> getPickList() {
    return pickList;
  }
}
