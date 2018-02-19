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

package org.jppf.client;

import java.io.ByteArrayInputStream;
import java.util.*;

import org.jppf.job.*;
import org.jppf.job.persistence.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.protocol.*;
import org.jppf.serialization.JPPFSerialization;
import org.jppf.utils.StringUtils;
import org.slf4j.*;

/**
 * Instances of this class allow monitoring and managing, on the client side, the jobs persisted in a remote driver.
 * In particular, it allows to retrieve jobs from the driver's persistence store and either process their results if they have completed,
 * or resubmit them vith a {@link JPPFClient}.
 * <p>The communication with the driver is performed via JMX, thus a working JMX connection to the driver must be provided in the constructor.
 * @author Laurent Cohen
 * @since 6.0
 */
public class JPPFDriverJobPersistence {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFDriverJobPersistence.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether the trace level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * A JMX connection wrapper to a JPPF driver.
   */
  private final JMXDriverConnectionWrapper jmx;
  /**
   * A proxy to the persisted jobs manager MBean.
   */
  private final PersistedJobsManagerMBean persistedJobsManager;

  /**
   * Initialize this persisted job manager with the specified driver JMX connection.
   * @param jmx a JMX connection wrapper to a JPPF driver.
   * @throws IllegalStateException if the connection to the driver isn't working for any reason. The actual exception is set as root cause.
   */
  public JPPFDriverJobPersistence(final JMXDriverConnectionWrapper jmx) {
    this.jmx = jmx;
    try {
      this.persistedJobsManager = this.jmx.getPersistedJobsManager();
      if (this.persistedJobsManager == null) throw new IllegalStateException("persistedJobsManager is null");
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * List the persisted jobs that match the provided job selector.
   * @param selector the selector used to filter persisted jobs, a {@code null} selector is equivalent to {@link JobSelector#ALL_JOBS}.
   * @return a list of the uuids of the matching jobs, possibly empty if no job was found.
   * @throws Exception if any error occurs while communicating with the driver.
   */
  public List<String> listJobs(final JobSelector selector) throws Exception {
    final List<String> result = persistedJobsManager.getPersistedJobUuids(selector);
    if (debugEnabled) log.debug("found jobs: {}", result);
    return result;
  }

  /**
   * Delete the persisted job with the sdpecified uuid. This method is equivalent to calling {@link #deleteJobs(JobSelector) deleteJobs(new JobUuidSelector(uuid))}.
   * @param uuid the UUID of the job to delete.
   * @return {@code true} if the job was successfully deleted, {@code false}.otherwise.
   * @throws Exception if any error occurs while communicating with the driver.
   */
  public boolean deleteJob(final String uuid) throws Exception {
    final List<String> result = persistedJobsManager.deletePersistedJobs(new JobUuidSelector(uuid));
    return (result != null) && result.contains(uuid);
  }

  /**
   * Delete the persisted jobs that match the provided job selector.
   * @param selector the selector used to filter persisted jobs, a {@code null} selector is equivalent to {@link JobSelector#ALL_JOBS}.
   * @return a list of the uuids of the matching jobs that were actually deleted, possibly empty if no job was found.
   * @throws Exception if any error occurs while communicating with the driver.
   */
  public List<String> deleteJobs(final JobSelector selector) throws Exception {
    final List<String> result = persistedJobsManager.deletePersistedJobs(selector);
    if (debugEnabled) log.debug("deleted jobs: {}", result);
    return result;
  }

  /**
   * Retieve and rebuild the persisted job with the specified uuid.
   * @param uuid the UUID of the job to delete.
   * @return a {@link JPPFJob} instance, or {@code null} if the job could not be found.
   * @throws Exception if any error occurs while communicating with the driver.
   */
  public JPPFJob retrieveJob(final String uuid) throws Exception {
    final TaskBundle header = load(uuid, PersistenceObjectType.JOB_HEADER, -1);
    if (debugEnabled) log.debug("got job header for uuid={} : {}", uuid, header);
    if (header == null) return null;
    final JPPFJob job = new JPPFJob(header.getUuid());
    job.setName(header.getName());
    job.setSLA(header.getSLA());
    job.setMetadata(header.getMetadata());
    final int[][] positions = persistedJobsManager.getPersistedJobPositions(uuid);
    if (debugEnabled) log.debug("got task positions for uuid={} : {}", uuid, StringUtils.buildString(", ", "{", "}", positions[0]));
    if (debugEnabled) log.debug("got result positions for uuid={} : {}", uuid, StringUtils.buildString(", ", "{", "}", positions[1]));
    for (int i=0; i<2; i++) Arrays.sort(positions[i]);
    final List<PersistenceInfo> toLoad = new ArrayList<>(1 + positions[0].length + positions[1].length);
    toLoad.add(new PersistenceInfoImpl(uuid, null, PersistenceObjectType.DATA_PROVIDER, -1, null));
    for (int i=0; i<positions[0].length; i++) toLoad.add(new PersistenceInfoImpl(uuid, null, PersistenceObjectType.TASK, positions[0][i], null));
    for (int i=0; i<positions[1].length; i++) toLoad.add(new PersistenceInfoImpl(uuid, null, PersistenceObjectType.TASK_RESULT, positions[1][i], null));
    long requestId = -1L;
    try {
      requestId = persistedJobsManager.requestLoad(toLoad);
      final DataProvider dataProvider = load(requestId, uuid, PersistenceObjectType.DATA_PROVIDER, -1);
      if (traceEnabled) log.trace("got dataprovider for uuid={} : {}", uuid, dataProvider);
      job.setDataProvider(dataProvider);
      for (int i=0; i<positions[0].length; i++) {
        final Task<?> task = load(requestId, uuid, PersistenceObjectType.TASK, positions[0][i]);
        if (traceEnabled) log.trace(String.format("got task at position %d for uuid=%s : %s", positions[0][i], uuid, task));
        job.add(task);
      }
      final List<Task<?>> results = new ArrayList<>(positions[1].length);
      for (int i=0; i<positions[1].length; i++) {
        final Task<?> task = load(requestId, uuid, PersistenceObjectType.TASK_RESULT, positions[1][i]);
        if (traceEnabled) log.trace(String.format("got task result at position %d for uuid=%s : %s", positions[1][i], uuid, task));
        results.add(task);
      }
      job.getResults().addResults(results);
      if (job.unexecutedTaskCount() <= 0) job.setStatus(JobStatus.COMPLETE);
    } finally {
      if (requestId >= 0L) persistedJobsManager.deleteLoadRequest(requestId);
    }
    return job;
  }

  /**
   * Get the description of the job with the specified uuid. This method retrieves the job's uuid, name, number of tasks, SLA and metadata.
   * @param uuid the uuid of the job to retrieve.
   * @return the job descirption as a {@link JPPFDistributedJob} instance.
   * @throws Exception if any error occurs while communicating with the driver.
   */
  public JPPFDistributedJob getJobDescription(final String uuid) throws Exception {
    return load(uuid, PersistenceObjectType.JOB_HEADER, -1);
  }

  /**
   * Determines whether the job has completed and all execution results are available.
   * @param uuid the UUID of the jonb to check.
   * @return {@code true} if the job has completed, {@code false} otherwise.
   * @throws Exception if any error occurs while communicating with the driver.
   */
  public boolean isJobComplete(final String uuid) throws Exception {
    return persistedJobsManager.isJobComplete(uuid);
  }

  /**
   * Load an object that is part of a job from the driver's pereistence store.
   * @param <T> the runtime tpe of the object to retrieve.
   * @param uuid the the job uuid.
   * @param type the type of object to load.
   * @param position the position of the object, if applicable.
   * @return the loaded object.
   * @throws Exception if any error occurs while communicating with the driver.
   */
  @SuppressWarnings("unchecked")
  private <T> T load(final String uuid, final PersistenceObjectType type, final int position) throws Exception {
    final byte[] bytes = (byte[]) persistedJobsManager.getPersistedJobObject(uuid, type, position);
    if (bytes == null) return null;
    if (traceEnabled) log.trace("got byte[{}]", bytes.length);
    try (final ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
      return (T) JPPFSerialization.Factory.getSerialization().deserialize(is);
    }
  }

  /**
   * Load an object that is part of a job from the driver's pereistence store.
   * @param <T> the runtime tpe of the object to retrieve.
   * @param requestId id of the preload request.
   * @param uuid the the job uuid.
   * @param type the type of object to load.
   * @param position the position of the object, if applicable.
   * @return the loaded object.
   * @throws Exception if any error occurs while communicating with the driver.
   */
  @SuppressWarnings("unchecked")
  private <T> T load(final long requestId, final String uuid, final PersistenceObjectType type, final int position) throws Exception {
    final byte[] bytes = (byte[]) persistedJobsManager.getPersistedJobObject(requestId, uuid, type, position);
    if (bytes == null) return null;
    if (traceEnabled) log.trace("got byte[{}]", bytes.length);
    try (final ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
      return (T) JPPFSerialization.Factory.getSerialization().deserialize(is);
    }
  }
}
