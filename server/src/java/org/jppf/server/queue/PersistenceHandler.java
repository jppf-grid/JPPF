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

import java.io.InputStream;
import java.util.*;

import org.jppf.io.*;
import org.jppf.job.persistence.*;
import org.jppf.node.protocol.*;
import org.jppf.server.protocol.*;
import org.jppf.utils.*;
import org.jppf.utils.streams.*;
import org.slf4j.*;

/**
 * 
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
   * The perisistence service.
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
  public void storeJob(final ServerJob job, final ServerTaskBundleClient clientBundle, final boolean tasksOnly) {
    if ((persistence == null) || !job.getSLA().isPersistent()) return;
    try {
      if (debugEnabled) log.debug("persisting {} job {}", tasksOnly ? "existing" : "new", job);
      if (!tasksOnly) {
        persistence.store(new PersistenceInfoImpl(job.getUuid(), job.getJob(), PersistenceObjectType.JOB_HEADER, -1, clientBundle.getJobDataLocation()));
        persistence.store(new PersistenceInfoImpl(job.getUuid(), job.getJob(), PersistenceObjectType.DATA_PROVIDER, -1, clientBundle.getDataProvider()));
      }
      for (ServerTask task: clientBundle.getTaskList()) {
        DataLocation dl = IOHelper.serializeData(task, IOHelper.getDefaultserializer());
        persistence.store(new PersistenceInfoImpl(job.getUuid(), job.getJob(), PersistenceObjectType.TASK, task.getJobPosition(), dl));
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Store the specified job results.
   * @param job the job whose results to store.
   * @param tasks the task results to store.
   */
  public void storeResults(final ServerJob job, final Collection<ServerTask> tasks) {
    if ((persistence == null) || !job.getSLA().isPersistent()) return;
    try {
      if (debugEnabled) log.debug("persisting {} results for job {}", tasks.size(), job);
      for (ServerTask task: tasks) {
        persistence.store(new PersistenceInfoImpl(job.getUuid(), job.getJob(), PersistenceObjectType.TASK_RESULT, task.getJobPosition(), task.getResult()));
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Remove the specified job from the persistence store.
   * @param job the job to remove.
   */
  public void deleteJob(final ServerJob job) {
    if ((persistence == null) || !job.getSLA().isPersistent()) return;
    if (debugEnabled) log.debug("removing job {} from persistence store", job);
    try {
      persistence.deleteJob(job.getUuid());
    } catch (JobPersistenceException e) {
      log.error("error deleting persistent job {} : {}", job, ExceptionUtils.getStackTrace(e));
    }
  }

  /**
   * Load the job with the specified uuid from the persistence store.
   * @param jobUuid uuid of the job to load.
   * @return a reconstituted {@link ServerJob} instance.
   */
  public ServerTaskBundleClient loadJob(final String jobUuid) {
    if (persistence == null) return null;
    if (debugEnabled) log.debug("loading job with uuid = {}", jobUuid);
    try {
      DataLocation headerData = load(new PersistenceInfoImpl(jobUuid, null, PersistenceObjectType.JOB_HEADER, -1, null));
      TaskBundle header = (TaskBundle) IOHelper.unwrappedData(headerData);
      header.setParameter(BundleParameter.FROM_PERSISTENCE, true);
      DataLocation dataProvider = load(new PersistenceInfoImpl(jobUuid, header, PersistenceObjectType.DATA_PROVIDER, -1, null)); 
      int[] taskPositions = persistence.getTaskPositions(jobUuid);
      Arrays.sort(taskPositions);
      int[] resultPositions = persistence.getTaskResultPositions(jobUuid);
      Arrays.sort(resultPositions);
      if (Arrays.equals(taskPositions, resultPositions)) {
        if (debugEnabled) log.debug("job already has completed: {}", header);
        persistence.deleteJob(jobUuid);
        return null;
      }

      int[] positionsToLoad = new int[taskPositions.length - resultPositions.length];
      int i = 0;
      for (int pos: taskPositions) {
        if (Arrays.binarySearch(resultPositions, pos) < 0) positionsToLoad[i++] = pos;
      }
      /*
      Map<Integer, ServerTask> tasks = new HashMap<>();
      for (int pos: taskPositions) {
        DataLocation taskData = load(new PersistenceInfoImpl(jobUuid, header, PersistenceObjectType.TASK, pos, null));
        ServerTask task = (ServerTask) IOHelper.unwrappedData(taskData);
        tasks.put(pos, task);
      }
      for (int pos: resultPositions) {
        DataLocation result = load(new PersistenceInfoImpl(jobUuid, header, PersistenceObjectType.TASK_RESULT, pos, null));
        tasks.get(pos).setResult(result);
      }
      List<ServerTask> pendingTasks = new ArrayList<>(taskPositions.length - resultPositions.length);
      for (ServerTask task: tasks.values()) {
        if (Arrays.binarySearch(resultPositions, task.getJobPosition()) < 0) {
          task.setState(TaskState.RESULT);
          pendingTasks.add(task);
        }
      }
      */
      List<ServerTask> pendingTasks = new ArrayList<>(taskPositions.length - resultPositions.length);
      for (int pos: positionsToLoad) {
        DataLocation taskData = load(new PersistenceInfoImpl(jobUuid, header, PersistenceObjectType.TASK, pos, null));
        ServerTask task = (ServerTask) IOHelper.unwrappedData(taskData);
        pendingTasks.add(task);
      }
      return new ServerTaskBundleClient(pendingTasks, header, headerData, dataProvider);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return null;
  }

  /**
   * Load all jobs in the persistence store.
   */
  public void loadPersistedJobs() {
    try {
      List<String> uuids = persistence.getPersistedJobUuids();
      for (String uuid: uuids) {
        try {
          ServerTaskBundleClient bundle = loadJob(uuid);
          if (bundle != null) queue.addBundle(bundle);
        } catch (Exception e) {
          log.error(e.getMessage(), e);
        }
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Load the specified job element from the persistence store.
   * @param info information on the element to load.
   * @return the job element as a {@link DataLocation} object.
   * @throws Exception if any error occurs.
   */
  private DataLocation load(final PersistenceInfo info) throws Exception {
    List<JPPFBuffer> buffers = null;
    try (InputStream is = persistence.load(info); MultipleBuffersOutputStream os = new MultipleBuffersOutputStream()) {
      StreamUtils.copyStream(is, os, false);
      buffers = os.toBufferList();
    }
    /*
    try (InputSource source = new StreamInputSource(is)) {
      return IOHelper.readData(source, info.getSize());
    }
    */
    return new MultipleBuffersLocation(buffers);
  }
}
