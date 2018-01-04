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

package org.jppf.classloader;

import java.util.*;
import java.util.concurrent.Future;

/**
 * Instances of this class are intended for grouping multiple class loading requests together.
 * @author Laurent Cohen
 * @author Martin JANDA
 * @exclude
 */
public class CompositeResourceWrapper extends JPPFResourceWrapper {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Mapping of futures to corresponding resource requests.
   */
  private final transient Map<JPPFResourceWrapper, Future<JPPFResourceWrapper>> futureMap = new HashMap<>();

  /**
   *
   */
  public CompositeResourceWrapper() {
  }

  @SuppressWarnings("unchecked")
  @Override
  public JPPFResourceWrapper[] getResources() {
    synchronized (getMonitor()) {
      final Set<JPPFResourceWrapper> set = (Set<JPPFResourceWrapper>) getData(ResourceIdentifier.RESOURCES_KEY);
      if ((set == null) || set.isEmpty()) return EMPTY_RESOURCE_WRAPPER_ARRAY;
      else return set.toArray(new JPPFResourceWrapper[set.size()]);
    }
  }

  /**
   * Add or replace request to this composite request.
   * @param resource the request to add or replace.
   */
  @SuppressWarnings("unchecked")
  public void addOrReplaceResource(final JPPFResourceWrapper resource) {
    synchronized (getMonitor()) {
      Set<JPPFResourceWrapper> resources = (Set<JPPFResourceWrapper>) getData(ResourceIdentifier.RESOURCES_KEY);
      if (resources == null) {
        resources = new HashSet<>();
        setData(ResourceIdentifier.RESOURCES_KEY, resources);
      } else resources.remove(resource);
      resources.add(resource);
    }
  }

  /**
   * Add a request to this composite request.
   * @param resource the request to add.
   * @return a future for getting the response at a later time.
   */
  public Future<JPPFResourceWrapper> addResource(final JPPFResourceWrapper resource) {
    Future<JPPFResourceWrapper> f = futureMap.get(resource);
    if (f == null) {
      addOrReplaceResource(resource);
      f = new ResourceFuture<>();
      futureMap.put(resource, f);
    }
    return f;
  }

  /**
   * Get the mapping of futures to corresponding resource requests.
   * @return a map of resource definitions to their corresponding future.
   */
  public Map<JPPFResourceWrapper, Future<JPPFResourceWrapper>> getFutureMap() {
    return futureMap;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    synchronized (getMonitor()) {
      sb.append("resources=").append(getData(ResourceIdentifier.RESOURCES_KEY));
    }
    sb.append(']');
    return sb.toString();
  }
}
