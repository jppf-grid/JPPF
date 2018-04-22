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

package org.jppf.management.forwarding;

import java.util.*;

/**
 * A callback invoked by each submitted forwarding task to notify that results have arrived from a node.
 */
class ForwardCallback {
  /**
   * The map holding the results from all nodes.
   */
  private final Map<String, Object> resultMap;
  /**
   * The expected total number of results.
   */
  private final int expectedCount;
  /**
   * The current count of received results.
   */
  private int count;

  /**
   * Initialize with the specified expected total number of results.
   * @param expectedCount the expected total number of results.
   */
  ForwardCallback(final int expectedCount) {
    this.resultMap = new HashMap<>(expectedCount);
    this.expectedCount = expectedCount;
  }

  /**
   * Called when a result is received from a node.
   * @param uuid the uuid of the node.
   * @param result the result of exception returned by the JMX call.
   */
  void gotResult(final String uuid, final Object result) {
    synchronized(this) {
      resultMap.put(uuid, result);
      if (++count == expectedCount) notify();
    }
  }

  /**
   * Wait until the number of results reaches the expected count.
   * @return the results map;
   * @throws Exception if any error occurs.
   */
  Map<String, Object> await() throws Exception {
    synchronized(this) {
      while (count < expectedCount) wait();
    }
    return resultMap;
  }
}