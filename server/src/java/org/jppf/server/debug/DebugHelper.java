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

package org.jppf.server.debug;

import java.util.*;

import org.jppf.server.JPPFDriver;
import org.jppf.server.protocol.ServerTask;
import org.jppf.utils.collections.*;

/**
 * Collection of utility methods to help investigate issues.
 * @author Laurent Cohen
 */
public final class DebugHelper {
  /**
   * Map of job uuids to the positions of the tasks whose results have been sent back to the client.
   */
  private static CollectionMap<String, Integer> resultsMap = new SetHashMap<>();

  /**
   * Add the specified completed tasks for the specified job uuid.
   * @param jobUuid the job uuid.
   * @param results the completed tasks.
   * @return a list of tasks that were already stored in this helper (duplicates).
   */
  public static List<ServerTask> addResults(final String jobUuid, final Collection<ServerTask> results) {
    if (!JPPFDriver.JPPF_DEBUG) return null;
    List<ServerTask> list = null;
    synchronized(resultsMap) {
      Collection<Integer> positions = resultsMap.getValues(jobUuid);
      if (positions == null) {
        for (ServerTask task: results) resultsMap.putValue(jobUuid, task.getJobPosition());
      } else {
        for (ServerTask task: results) {
          if (positions.contains(task.getJobPosition())) {
            if (list == null) list = new ArrayList<>(results.size());
            list.add(task);
          } else {
            resultsMap.putValue(jobUuid, task.getJobPosition());
          }
        }
      }
    }
    return list;
  }

  /**
   * Remove all results for the specified job.
   * @param jobUuid the job uuid.
   */
  public static void clearResults(final String jobUuid) {
    if (!JPPFDriver.JPPF_DEBUG) return;
    synchronized(resultsMap) {
      resultsMap.removeKey(jobUuid);
    }
  }

  /**
   * Show all the mapped results.
   * @return a string representing all the entries in the multimap.
   */
  public static String showResults() {
    if (!JPPFDriver.JPPF_DEBUG) return null;
    StringBuilder sb = new StringBuilder(resultsMap.getClass().getSimpleName()).append("[");
    synchronized(resultsMap) {
      for (Map.Entry<String, Collection<Integer>> entry: resultsMap.entrySet()) {
        sb.append("\n  ").append(entry.getKey()).append('=').append(entry.getValue());
      }
      sb.append("\n]");
      return sb.toString();
    }
  }

  /**
   * Check whether the specified tasks for the specified job have already completed.
   * @param jobUuid the job uuid.
   * @param results the completed tasks.
   * @return a list of tasks that were already stored in this helper (duplicates).
   */
  public static List<ServerTask> checkResults(final String jobUuid, final Collection<ServerTask> results) {
    if (!JPPFDriver.JPPF_DEBUG) return null;
    List<ServerTask> list = null;
    synchronized(resultsMap) {
      Collection<Integer> positions = resultsMap.getValues(jobUuid);
      if (positions != null) {
        for (ServerTask task: results) {
          if (positions.contains(task.getJobPosition())) {
            if (list == null) list = new ArrayList<>(results.size());
            list.add(task);
          }
        }
      }
    }
    return list;
  }
}
