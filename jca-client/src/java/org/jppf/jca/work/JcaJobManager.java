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

package org.jppf.jca.work;

import java.util.*;

import org.jppf.client.*;
import org.jppf.client.balancer.JobManagerClient;
import org.jppf.client.event.JobStatusListener;
import org.slf4j.*;

/**
 * This task provides asynchronous management of tasks submitted through the resource adapter.
 * It relies on a queue where job are first added, then submitted when a driver connection becomes available.
 * It also provides methods to check the status of a job and retrieve the results.
 * @author Laurent Cohen
 */
public class JcaJobManager extends JobManagerClient {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JcaJobManager.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Mapping of jobs to their uuid.
   */
  @SuppressWarnings("deprecation")
  private Map<String, JPPFJob> jobMap = new Hashtable<>();

  /**
   * Initialize this job worker with the specified JPPF client.
   * @param client the JPPF client that manages connections to the JPPF drivers.
   * @throws Exception if any error occurs.
   */
  public JcaJobManager(final AbstractGenericClient client) throws Exception {
    super(client);
  }

  /**
   * Add a job to the execution queue.
   * @param job encapsulation of the execution data.
   * @param listener an optional listener to receive job status change notifications, may be null.
   * @return the unique id of the job.
  */
  @Override
  public String submitJob(final JPPFJob job, final JobStatusListener listener) {
    jobMap.put(job.getUuid(), job);
    return super.submitJob(job, listener);
  }

  /**
   * Get a job given its uuid, without removing it from this job manager.
   * @param uuid the uuid of the job to find.
   * @return the job corresponding to the id, or {@code null} if the job could not be found.
   */
  public JPPFJob peekJob(final String uuid) {
    return jobMap.get(uuid);
  }

  /**
   * Get a job given its id, and remove it from this job manager.
   * @param id the id of the job to find.
   * @return the job corresponding to the uuid, or {@code null} if the job could not be found.
   */
  public JPPFJob pollJob(final String id) {
    return jobMap.remove(id);
  }

  /**
   * Get the ids of all currently available jobs.
   * @return a collection of uuids as strings.
   */
  public Collection<String> getAllJobUuids() {
    return Collections.unmodifiableSet(new HashSet<>(jobMap.keySet()));
  }
}
