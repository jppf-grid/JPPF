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

package org.jppf.ui.picklist;

import java.util.EventListener;

/**
 * Listener interface to receive notifications of changes in a pick list.
 * @param <T> the types of items in the pick list.
 * @author Laurent Cohen
 */
public interface PickListListener<T> extends EventListener {
  /**
   * Called when items are added to the list of picked items.
   * @param event encapsulates information about the pick list and added items.
   */
  void itemsAdded(PickListEvent<T> event);

  /**
   * Called when items are removed the list of picked items.
   * @param event encapsulates information about the pick list and removed items.
   */
  void itemsRemoved(PickListEvent<T> event);
}
