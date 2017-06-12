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

package org.jppf.job;

import java.util.*;

import org.jppf.node.protocol.JPPFDistributedJob;

/**
 * A job selector which accepts all jobs whose uuids are in the set specified in one of its constructors.
 * @author Laurent Cohen
 * @since 5.1
 */
public class JobUuidSelector implements JobSelector {
  /**
   * The set of accepted job uuids.
   */
  private final Set<String> uuids;

  /**
   * Initiialize this selector with the specified collection of accepted job uuids.
   * @param uuids a collection of string uuids.
   */
  public JobUuidSelector(final Collection<String> uuids) {
    this.uuids = (uuids == null) ? Collections.<String>emptySet() : new HashSet<>(uuids);
  }

  /**
   * Initiialize this selector with the specified array of accepted job uuids.
   * @param uuids a collection of string uuids.
   */
  public JobUuidSelector(final String...uuids) {
    this.uuids = new HashSet<>(Arrays.asList(uuids));
  }

  @Override
  public boolean accepts(final JPPFDistributedJob job) {
    return uuids.contains(job.getUuid());
  }

  /**
   * Get the set of job uuids accepted by this selector.
   * @return a set of uuids, possibly empty.
   */
  public Set<String> getUuids() {
    return uuids;
  }
}
