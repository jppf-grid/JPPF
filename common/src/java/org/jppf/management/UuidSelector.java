/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.management;

import java.util.*;

/**
 * Selects nodes based on their uuids.
 * @author Laurent Cohen
 */
public class UuidSelector implements NodeSelector {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The list of uuids of the nodes to select. This list is immutable.
   */
  private final Collection<String> uuids;

  /**
   * Initialize this selector with the specified list of node UUIDs.
   * @param uuids the uuids of the nodes to select.
   */
  public UuidSelector(final Collection<String> uuids) {
    this.uuids = (uuids == null) ? Collections.<String>emptyList() : new ArrayList<>(uuids);
  }

  /**
   * Initialize this selector with the specified array of node UUIDs.
   * @param uuids the uuids of the nodes to select.
   */
  public UuidSelector(final String...uuids) {
    this.uuids = (uuids == null) ? Collections.<String>emptyList() : Arrays.asList(uuids);
  }

  /**
   * Get the list of uuids of the nodes to select. This list is immutable.
   * @return a collection of uuids as strings.
   */
  public Collection<String> getUuids() {
    return uuids;
  }
}
