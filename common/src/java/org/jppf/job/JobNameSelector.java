/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.job;

import java.util.*;

import org.jppf.node.protocol.JPPFDistributedJob;

/**
 * A job selector which accepts all jobs whose names are in the set specified in one of its constructors.
 * @author Laurent Cohen
 * @since 6.2
 */
public class JobNameSelector implements JobSelector {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The set of accepted job uuids.
   */
  private final Set<String> names;

  /**
   * Initiialize this selector with the specified collection of accepted job uuids.
   * @param names a collection of job names.
   */
  public JobNameSelector(final Collection<String> names) {
    this.names = (names == null) ? Collections.<String>emptySet() : new HashSet<>(names);
  }

  /**
   * Initiialize this selector with the specified array of accepted job uuids.
   * @param names an array of job names.
   */
  public JobNameSelector(final String...names) {
    this.names = new HashSet<>(Arrays.asList(names));
  }

  @Override
  public boolean accepts(final JPPFDistributedJob job) {
    return names.contains(job.getName());
  }

  /**
   * Get the set of job uuids accepted by this selector.
   * @return a set of uuids, possibly empty.
   */
  public Set<String> getNames() {
    return names;
  }
}
