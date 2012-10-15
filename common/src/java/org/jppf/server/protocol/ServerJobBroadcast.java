/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
package org.jppf.server.protocol;

import org.jppf.io.DataLocation;
import org.jppf.job.JobNotificationEmitter;
import org.jppf.server.submission.SubmissionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 *
 * @author Martin JANDA
 * @exclude
 */
public class ServerJobBroadcast extends ServerJob {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ServerJobBroadcast.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Instance of parent broadcast job.
   */
  private transient ServerJobBroadcast parentJob;
  /**
   * The broadcast UUID.
   */
  private transient String broadcastUUID = null;
  /**
   * Map of all dispatched broadcast jobs.
   */
  private final Map<String, ServerJobBroadcast> broadcastMap;
  /**
   * Map of all pending broadcast jobs.
   */
  private final Set<ServerJobBroadcast> broadcastSet = new LinkedHashSet<ServerJobBroadcast>();

  /**
   * Initialized broadcast job with task bundle and data provider.
   * @param notificationEmitter an <code>JobNotificationEmitter</code> instance that fires job notifications.
   * @param job   underlying task bundle.
   * @param dataProvider the data location of the data provider.
   */
  public ServerJobBroadcast(final JobNotificationEmitter notificationEmitter, final JPPFTaskBundle job, final DataLocation dataProvider) {
    this(notificationEmitter, job, dataProvider, null, null);
  }

  /**
   * Initialized broadcast job with task bundle and data provider.
   * @param notificationEmitter an <code>JobNotificationEmitter</code> instance that fires job notifications.
   * @param job   underlying task bundle.
   * @param dataProvider the data location of the data provider.
   * @param parentJob instance of parent broadcast job.
   * @param broadcastUUID the broadcast UUID.
   */
  protected ServerJobBroadcast(final JobNotificationEmitter notificationEmitter, final JPPFTaskBundle job, final DataLocation dataProvider, final ServerJobBroadcast parentJob, final String broadcastUUID) {
    super(notificationEmitter, job, dataProvider);
    if (!job.getSLA().isBroadcastJob()) throw new IllegalStateException("Not broadcast job");

    this.parentJob = parentJob;
    this.broadcastUUID = broadcastUUID;
    if (broadcastUUID == null) {
      this.broadcastMap = new LinkedHashMap<String, ServerJobBroadcast>();
    } else {
      this.broadcastMap = Collections.emptyMap();
    }
  }

  @Override
  public String getBroadcastUUID() {
    return broadcastUUID;
  }

  /**
   * Make a copy of this client job wrapper.
   * @param broadcastUUID the broadcast UUID.
   * @return a new <code>ServerJob</code> instance.
   */
  public ServerJobBroadcast createBroadcastJob(final String broadcastUUID) {
    if (broadcastUUID == null || broadcastUUID.isEmpty()) throw new IllegalArgumentException("broadcastUUID is blank");
    ServerJobBroadcast clientJob;
    synchronized (broadcastSet) {
      clientJob = new ServerJobBroadcast(notificationEmitter, job, getDataProvider(), this, broadcastUUID);
      for (ServerTaskBundleClient bundle : getBundleList()) {
        clientJob.addBundle(bundle);
      }
      broadcastSet.add(clientJob);
    }
    return clientJob;
  }

  @Override
  public void jobDispatched(final ServerTaskBundleNode bundle) {
    super.jobDispatched(bundle);
    if (parentJob != null) parentJob.broadcastDispatched(this);
  }

  /**
   * Called when all or part of broadcast job is dispatched to a driver.
   * @param broadcastJob    the dispatched job.
   */
  protected void broadcastDispatched(final ServerJobBroadcast broadcastJob) {
    if (broadcastJob == null) throw new IllegalArgumentException("broadcastJob is null");
    boolean empty;
    synchronized (broadcastSet) {
      broadcastSet.remove(broadcastJob);
      empty = broadcastMap.isEmpty();
      broadcastMap.put(broadcastJob.getBroadcastUUID(), broadcastJob);
    }
    if (empty) {
      updateStatus(NEW, EXECUTING);
      setSubmissionStatus(SubmissionStatus.EXECUTING);
    }
  }

  /**
   * Called to notify that the execution of broadcast job has completed.
   * @param broadcastJob    the completed job.
   */
  protected void broadcastCompleted(final ServerJobBroadcast broadcastJob) {
    if (broadcastJob == null) throw new IllegalArgumentException("broadcastJob is null");
    //    if (debugEnabled) log.debug("received " + n + " tasks for node uuid=" + uuid);
    boolean empty;
    synchronized (broadcastSet) {
      if (broadcastMap.remove(broadcastJob.getBroadcastUUID()) != broadcastJob && !broadcastSet.contains(broadcastJob)) throw new IllegalStateException("broadcast job not found");
      empty = broadcastMap.isEmpty();
    }
    if (empty) taskCompleted(null, null);
  }

  @Override
  protected void fireTaskCompleted(final ServerJob result) {
    if (parentJob == null) {
      super.fireTaskCompleted(result);
    } else {
      fireJobEnded();
      parentJob.broadcastCompleted(this);
    }
  }

  @Override
  public void taskCompleted(final ServerTaskBundleNode bundle, final Exception exception) {
    if (isCancelled()) {
      List<ServerJobBroadcast> list;
      synchronized (broadcastSet) {
        list = new ArrayList<ServerJobBroadcast>(broadcastSet.size() + broadcastMap.size());
        list.addAll(broadcastMap.values());
        list.addAll(broadcastSet);
        broadcastSet.clear();
      }
      for (ServerJobBroadcast broadcastJob : list) broadcastJob.cancel(false);
    }
    super.taskCompleted(bundle, exception);
  }

  @Override
  public void addBundle(final ServerTaskBundleClient bundle) {
    if (parentJob == null) {
      super.addBundle(bundle);
      synchronized (broadcastSet) {
        for (ServerJobBroadcast item : broadcastSet) {
          item.addBundle(bundle);
        }
      }
    } else {
      super.addBundle(new ServerTaskBundleClient(bundle.getJob().copy(), bundle.getDataProvider(), bundle.getDataLocationList()));
    }
  }
}
