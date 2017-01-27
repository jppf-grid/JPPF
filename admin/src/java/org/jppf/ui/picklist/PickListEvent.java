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

package org.jppf.ui.picklist;

import java.util.*;

/**
 * Event sent by a {@link PickList} when the list of picked items changes. 
 * @param <T> The type of the items in the pick list.
 * @author Laurent Cohen
 */
public class PickListEvent<T> extends EventObject {
  /**
   * The items that were added to the selection.
   */
  private final List<T> addedItems;
  /**
   * The items that were removed from the selection.
   */
  private final List<T> removedItems;

  /**
   * Initialize this event with the specified source {@link PickList} and picked items.
   * @param pickList the {@link PickList} from which this event originates.
   * @param addedItems the items that were added to the selection.
   * @param removedItems the items that were removed from the selection.
   */
  public PickListEvent(final PickList<T> pickList, final List<T> addedItems, final List<T> removedItems) {
    super(pickList);
    this.addedItems = (addedItems != null) ? addedItems : Collections.<T>emptyList();
    this.removedItems = (removedItems != null) ? removedItems : Collections.<T>emptyList();
  }

  /**
   * Get the pick list component source of this event.
   * @return a {@link PickList} object.
   */
  @SuppressWarnings("unchecked")
  public PickList<T> getPickList() {
    return (PickList<T>) getSource();
  }

  /**
   * Get the items that were added to the selection.
   * @return a list of items which may be empty but never {@code null}.
   */
  public List<T> getAddedItems() {
    return addedItems;
  }

  /**
   * Get the items that were removed from the selection.
   * @return a list of items which may be empty but never {@code null}.
   */
  public List<T> getRemovedItems() {
    return removedItems;
  }
}
