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

package org.jppf.server.queue;

import java.io.*;
import java.util.*;

import org.jppf.io.*;
import org.jppf.job.persistence.*;
import org.jppf.node.protocol.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.protocol.*;
import org.jppf.utils.*;
import org.jppf.utils.streams.*;
import org.slf4j.*;

/**
 * This class is a facade to the job persistence service defined in the configuration. 
 * @author Laurent Cohen
 */
public class PersistenceHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(PersistenceHandler.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The persistence service.
   */
  private final JobPersistence persistence;
  /**
   * The jobs queue.
   */
  private final JPPFPriorityQueue queue;
 
  /**
   * Initialize this persistence handler.
   * @param queue the jobs queue.
   */
  public PersistenceHandler(final JPPFPriorityQueue queue) {
    this.queue = queue;
    persistence = JobPersistenceFactory.getInstance().getPersistence();
  }

  /**
   * Store the specified job upon initial queuing.
   * @param job the job to store.
   * @param clientBundle contains the tasks to store.
   * @param tasksOnly whether to ony store the tasks and not the header and data provider.
   */
  void storeJob(final ServerJob job, final ServerTaskBundleClient clientBundle, final boolean tasksOnly) {
    if (!isPersistent(job)) return;
    final long start = System.nanoTime();
    try {
      if (debugEnabled) log.debug("persisting {} job {}", tasksOnly ? "existing" : "new", job);
      final List<ServerTask> taskList = clientBundle.getTaskList();
      final List<PersistenceInfo> infos = new ArrayList<>(taskList.size() + (tasksOnly ? 0 : 2));
      if (!tasksOnly) {
        infos.add(new PersistenceInfoImpl(job.getUuid(), job.getJob(), PersistenceObjectType.JOB_HEADER, -1, IOHelper.serializeData(job.getJob())));
        infos.add(new PersistenceInfoImpl(job.getUuid(), job.getJob(), PersistenceObjectType.DATA_PROVIDER, -1, clientBundle.getDataProvider()));
      }
      for (final ServerTask task: taskList) {
        final DataLocation dl = IOHelper.serializeData(task);
        infos.add(new PersistenceInfoImpl(job.getUuid(), job.getJob(), PersistenceObjectType.TASK, task.getJobPosition(), dl));
      }
      persistence.store(infos);
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
    final long elapsed = (System.nanoTime() - start) / 1_000_000L;
    if (debugEnabled) log.debug(String.format("took %,d ms to store job %s", elapsed, job.getName()));
  }

  /**
   * Store the specified job upon initial queuing.
   * @param job the job to store.
   */
  public void updateJobHeader(final ServerJob job) {
    if (!isPersistent(job)) return;
    try {
      if (debugEnabled) log.debug("updating header for job {}", job);
      //job.getJob().setParameter(BundleParameter.ALREADY_PERSISTED, true);
      final DataLocation data = IOHelper.serializeData(job.getJob());
      persistence.store(Arrays.asList((PersistenceInfo) new PersistenceInfoImpl(job.getUuid(), job.getJob(), PersistenceObjectType.JOB_HEADER, -1, data)));
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Store the specified job results.
   * @param job the job whose results to store.
   * @param tasks the task results to store.
   */
  public void storeResults(final ServerJob job, final Collection<ServerTask> tasks) {
    if (!isPersistent(job)) return;
    try {
      if (debugEnabled) log.debug("persisting {} results for job {}", tasks.size(), job);
      final List<PersistenceInfo> infos = new ArrayList<>(tasks.size());
      for (final ServerTask task: tasks) {
        infos.add(new PersistenceInfoImpl(job.getUuid(), job.getJob(), PersistenceObjectType.TASK_RESULT, task.getJobPosition(), task.getResult()));
      }
      persistence.store(infos);
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Remove the specified job from the persistence store.
   * @param job the job to remove.
   */
  void deleteJob(final ServerJob job) {
    if (isPersistent(job)) deleteJob(job.getUuid());
  }

  /**
   * Remove the specified job from the persistence store.
   * @param jobUuid uuid of the job to remove.
   */
  public void deleteJob(final String jobUuid) {
    if (!isPersistenceReady()) return;
    if (debugEnabled) log.debug("removing job {} from persistence store", jobUuid);
    try {
      persistence.deleteJob(jobUuid);
    } catch (final JobPersistenceException e) {
      log.error("error deleting persistent job {} : {}", jobUuid, ExceptionUtils.getStackTrace(e));
    }
  }

  /**
   * Load the job with the specified uuid from the persistence store.
   * @param jobUuid uuid of the job to load.
   * @param useAutoExecuteOnRestart whether o use the {@code autoExecuteOnRestart} attribute in the job SLA's peristence spec.
   * @return a reconstituted {@link ServerJob} instance.
   */
  private ServerTaskBundleClient loadJob(final String jobUuid, final boolean useAutoExecuteOnRestart) {
    if (persistence == null) return null;
    if (debugEnabled) log.debug("loading job with uuid = {}", jobUuid);
    try {
      final TaskBundle header = loadHeader(jobUuid);
      if (header == null) return null;
      if (useAutoExecuteOnRestart && !header.getSLA().getPersistenceSpec().isAutoExecuteOnRestart()) {
        if (debugEnabled) log.debug("job with uuid = {} has autoExecuteOnRestart=false, it will not be loaded", jobUuid);
        return null;
      }
      header.setParameter(BundleParameter.FROM_PERSISTENCE, true);
      final int[] taskPositions = persistence.getTaskPositions(jobUuid);
      Arrays.sort(taskPositions);
      final int[] resultPositions = persistence.getTaskResultPositions(jobUuid);
      Arrays.sort(resultPositions);
      if (Arrays.equals(taskPositions, resultPositions) && header.getSLA().getPersistenceSpec().isDeleteOnCompletion()) {
        if (debugEnabled) log.debug("job already has completed: {}", header);
        persistence.deleteJob(jobUuid);
        return null;
      }
      final int[] positionsToLoad = new int[taskPositions.length - resultPositions.length];
      int i = 0;
      for (int pos: taskPositions) {
        if (Arrays.binarySearch(resultPositions, pos) < 0) positionsToLoad[i++] = pos;
      }
      if (debugEnabled) log.debug("positions to load for jobUuid={} : {}", jobUuid, StringUtils.buildString(positionsToLoad));
      final List<PersistenceInfo> infos = new ArrayList<>(positionsToLoad.length + 1);
      infos.add(new PersistenceInfoImpl(jobUuid, header, PersistenceObjectType.DATA_PROVIDER, -1, null));
      for (int pos: positionsToLoad) {
        infos.add(new PersistenceInfoImpl(jobUuid, header, PersistenceObjectType.TASK, pos, null));
      }
      final List<InputStream> streams = persistence.load(infos);
      final DataLocation dataProvider = load(streams.get(0)); 
      final List<ServerTask> pendingTasks = new ArrayList<>(taskPositions.length - resultPositions.length);
      for (i=1; i<streams.size(); i++) {
        final DataLocation taskData = load(streams.get(i));
        final ServerTask task = (ServerTask) IOHelper.unwrappedData(taskData);
        pendingTasks.add(task);
      }
      return new ServerTaskBundleClient(pendingTasks, header, dataProvider);
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
    return null;
  }

  /**
   * Load all jobs in the persistence store.
   */
  public void loadPersistedJobs() {
    if (persistence == null) return;
    try {
      if (debugEnabled) log.debug("loading persisted jobs");
      final List<String> uuids = persistence.getPersistedJobUuids();
      for (final String uuid: uuids) {
        try {
          final ServerTaskBundleClient bundle = loadJob(uuid, true);
          if (bundle != null) queue.addBundle(bundle);
        } catch (final Exception e) {
          log.error(e.getMessage(), e);
        }
      }
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Load the specified job element from the persistence store.
   * @param info information on the element to load.
   * @return the job element as a {@link DataLocation} object.
   * @throws Exception if any error occurs.
   */
  public DataLocation load(final PersistenceInfo info) throws Exception {
    final List<DataLocation> list = load(Arrays.asList(info));
    return (list == null) || list.isEmpty() ? null : list.get(0);
  }

  /**
   * Load the specified job elements from the persistence store.
   * @param infos information on the elements to load.
   * @return the job element as a {@link DataLocation} object.
   * @throws Exception if any error occurs.
   */
  public List<DataLocation> load(final Collection<PersistenceInfo> infos) throws Exception {
    final long start = System.nanoTime();
    List<DataLocation> result = null;
    final List<InputStream> list = persistence.load(infos);
    if ((list != null) && !list.isEmpty()) {
      result = new ArrayList<>(infos.size());
      for (InputStream is: list) result.add(load(is));
    }
    final long elapsed = (System.nanoTime() - start) / 1_000_000L;
    if (debugEnabled) log.debug("took {} ms to load {} job elements", elapsed, infos.size());
    return result;
  }

  /**
   * Load the specified job element from the persistence store.
   * @param stream information on the element to load.
   * @return the job element as a {@link DataLocation} object.
   * @throws Exception if any error occurs.
   */
  private static DataLocation load(final InputStream stream) throws Exception {
    try (final InputStream is = stream; final MultipleBuffersOutputStream os = new MultipleBuffersOutputStream()) {
      if (is == null) return null;
      StreamUtils.copyStream(is, os, false);
      return new MultipleBuffersLocation(os.toBufferList());
    }
  }

  /**
   * Load and deserialize the header of the job with the sdpecified uuid.
   * @param jobUuid the uuid of the job whose header to load.
   * @return the deserialized header as  a {@link TaskBundle}.
   * @throws Exception if any error occurs.
   */
  public TaskBundle loadHeader(final String jobUuid) throws Exception {
    if (!isJobPersisted(jobUuid)) return null;
    return (TaskBundle) IOHelper.unwrappedData(load(new PersistenceInfoImpl(jobUuid, null, PersistenceObjectType.JOB_HEADER, -1, null)));
  }

  /**
   * Load the specified job element from the persistence store.
   * @param info information on the element to load.
   * @return the job element as a {@link DataLocation} object.
   * @throws Exception if any error occurs.
   */
  public DataLocation loadToDisk(final PersistenceInfo info) throws Exception {
    final File dir = FileUtils.getJPPFTempDir();
    final File file = File.createTempFile(info.getType().name(), ".tmp", dir);
    final List<InputStream> list = persistence.load(Arrays.asList(info));
    if ((list == null) || list.isEmpty()) return null;
    try (final InputStream is = list.get(0); BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
      StreamUtils.copyStream(is, os, false);
    }
    return new FileDataLocation(file);
  }

  /**
   * Determine whether the specified job should be persisted.
   * @param job the job to check.
   * @return {@code true} if the job is persistent and job persistence is active, {@code false} otherwise.
   */
  private boolean isPersistent(final ServerJob job) {
    return isPersistenceReady() && job.isPersistent() && job.getJob().getParameter(BundleParameter.ALREADY_PERSISTED, false)
      && !job.getJob().getParameter(BundleParameter.ALREADY_PERSISTED_P2P, false);
  }

  /**
   * Determine whether the specified job is persisted.
   * @param uuid the uuid of the job to check.
   * @return {@code true} if the job is persisted, {@code false} otherwise.
   * @throws JobPersistenceException if any error occurs while accessing the persistence store.
   */
  public boolean isJobPersisted(final String uuid) throws JobPersistenceException {
    return isPersistenceReady() && persistence.isJobPersisted(uuid);
  }

  /**
   * Determine whether the persistence is available.
   * @return {@code true} if the persistence is available, {@code false} otherwise.
   */
  private boolean isPersistenceReady() {
    return (persistence != null) && !JPPFDriver.getInstance().isShuttingDown();
  }

  /**
   * Get the persistence service.
   * @return a {@link JobPersistence} instance.
   */
  public JobPersistence getPersistence() {
    return persistence;
  }

  /**
   * Get the positions of all tasks and task results for the specified persisted job.
   * @param uuid the uuid of the persisted job to lookup.
   * @return an array of int arrays, where the first array represents the positions of all tasks before execution,
   * and the second array represents the positions of all task results. The second array may be empty if no task result was received.
   * @throws Exception if any error occurs.
   */
  public int[][] getPersistedJobPositions(final String uuid) throws Exception {
    if (!isPersistenceReady()) return null;
    if (debugEnabled) log.debug("requesting positions for uuid={}", uuid);
    int[] taskPositions = persistence.getTaskPositions(uuid);
    if (taskPositions == null) taskPositions = new int[0];
    else Arrays.sort(taskPositions);
    int[] resultPositions = persistence.getTaskResultPositions(uuid);
    if (resultPositions == null) resultPositions = new int[0];
    Arrays.sort(resultPositions);
    return new int[][] { taskPositions, resultPositions };
  }

  /**
   * Get the uuids of all persisted jobs.
   * @return a list of persisted job uuids, possibly emtpy if no job is persisted or persistence is not avaialble.
   * @throws JobPersistenceException if any error occurs while searching for persisted jobs.
   */
  public List<String> getPersistedJobUuids() throws JobPersistenceException {
    return isPersistenceReady() ? persistence.getPersistedJobUuids() : new ArrayList<String>();
  }
}
