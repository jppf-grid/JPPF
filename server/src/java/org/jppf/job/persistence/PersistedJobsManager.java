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

package org.jppf.job.persistence;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jppf.io.*;
import org.jppf.job.*;
import org.jppf.job.persistence.impl.PersistenceInfoKey;
import org.jppf.node.protocol.TaskBundle;
import org.jppf.server.JPPFDriver;
import org.jppf.server.protocol.ServerTask;
import org.jppf.server.queue.PersistenceHandler;
import org.jppf.utils.ExceptionUtils;
import org.jppf.utils.streams.*;
import org.slf4j.*;

/**
 * Implementation of the {@link PersistedJobsManagerMBean} interface.
 * @author Laurent Cohen
 * @exclude
 */
public class PersistedJobsManager implements PersistedJobsManagerMBean {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(PersistedJobsManager.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether the trace level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * The persistence handler held by the driver queue.
   */
  private final PersistenceHandler handler;
  /**
   *
   */
  private final Map<Long, Map<PersistenceInfoKey, DataLocation>> loadRequests = new ConcurrentHashMap<>();
  /**
   *
   */
  private final AtomicLong loadRequestSequence = new AtomicLong(0L);

  /**
   * Initialize this MBean.
   */
  public PersistedJobsManager() {
    handler = JPPFDriver.getInstance().getQueue().getPersistenceHandler();
  }

  @Override
  public List<String> getPersistedJobUuids(final JobSelector selector) throws Exception {
    return getMatchingUuids(selector);
  }

  @Override
  public Object getPersistedJobObject(final String uuid, final PersistenceObjectType type, final int position) throws Exception {
    if (debugEnabled) log.debug(String.format("requesting object with uuid=%s, type=%s, pos=%d", uuid, type, position));
    final DataLocation data = handler.load(new PersistenceInfoImpl(uuid, null, type, position, null));
    if (debugEnabled) {
      if (data != null) log.debug(String.format("got object=%s for uuid=%s, type=%s, pos=%d", data, uuid, type, position));
      else log.debug(String.format("could not find persisted object for uuid=%s, type=%s, pos=%d", uuid, type, position));
    }
    return deserialize(data, type);
  }

  @Override
  public int[][] getPersistedJobPositions(final String uuid) throws Exception {
    return handler.getPersistedJobPositions(uuid);
  }

  @Override
  public List<String> deletePersistedJobs(final JobSelector selector) throws Exception {
    final List<String> persisted = getMatchingUuids(selector);
    final List<String> result = new ArrayList<>();
    for (String uuid: persisted) {
      try {
        handler.deleteJob(uuid);
        result.add(uuid);
      } catch (final Exception e) {
        if (debugEnabled) log.debug("error deleting job with uuid={} : {}", uuid, ExceptionUtils.getStackTrace(e));
        else log.warn("error deleting job with uuid={} : {}", uuid, ExceptionUtils.getMessage(e));
      }
    }
    if (debugEnabled) log.debug("request to delete jobs matching {}, result = {}", selector, result);
    return result;
  }

  @Override
  public boolean isJobersisted(final String uuid) throws Exception {
    return handler.isJobPersisted(uuid);
  }

  @Override
  public boolean isJobComplete(final String uuid) throws Exception {
    final TaskBundle header = handler.loadHeader(uuid);
    if (header == null) return false;
    final int n = header.getTaskCount();
    final int[][] positions = getPersistedJobPositions(uuid);
    if ((positions == null) || (positions.length < 2)) return false;
    for (int i=0; i<2; i++) {
      if ((positions[i] == null) || (positions[i].length == 0) || positions[i].length < n) return false;
      Arrays.sort(positions[i]);
    }
    return Arrays.equals(positions[0], positions[1]);
  }

  /**
   * Get the uuids of the persisdted jobs that match the specified job selector.
   * @param selector the job selector to use.
   * @return a list of matching job uuids, possibly empty if none matched the selector.
   * @throws Exception if any error occurs.
   */
  private List<String> getMatchingUuids(final JobSelector selector) throws Exception {
    final JobSelector sel = (selector == null) ? JobSelector.ALL_JOBS : selector;
    final List<String> uuids = handler.getPersistedJobUuids();
    final List<String> result = new ArrayList<>(uuids.size());
    for (String uuid: uuids) {
      if ((sel instanceof AllJobsSelector) || ((sel instanceof JobUuidSelector) && ((JobUuidSelector) sel).getUuids().contains(uuid))) result.add(uuid);
      else {
        final DataLocation headerData = handler.load(new PersistenceInfoImpl(uuid, null, PersistenceObjectType.JOB_HEADER, -1, null));
        final TaskBundle header = (TaskBundle) IOHelper.unwrappedData(headerData);
        if (sel.accepts(header)) result.add(uuid);
      }
    }
    if (debugEnabled) log.debug("uuids matching {} : {}", sel, result);
    return result;
  }

  @Override
  public long requestLoad(final Collection<PersistenceInfo> infos) throws Exception {
    final long id = loadRequestSequence.incrementAndGet();
    final List<DataLocation> list = handler.load(infos);
    final Map<PersistenceInfoKey, DataLocation> map = new HashMap<>(list.size());
    int i = 0;
    for (PersistenceInfo info: infos) map.put(new PersistenceInfoKey(info), list.get(i++));
    loadRequests.put(id, map);
    return id;
  }

  @Override
  public Object getPersistedJobObject(final long requestId, final String uuid, final PersistenceObjectType type, final int position) throws Exception {
    if (debugEnabled) log.debug(String.format("requesting object with uuid=%s, type=%s, pos=%d, requestId=%d", uuid, type, position, requestId));
    final Map<PersistenceInfoKey, DataLocation> map = loadRequests.get(requestId);
    if (map == null) return null;
    final PersistenceInfoKey key = new PersistenceInfoKey(uuid, type, position);
    final DataLocation data = map.remove(key);
    if (map.isEmpty()) loadRequests.remove(requestId);
    if (traceEnabled) {
      if (data != null) log.trace(String.format("got object=%s for uuid=%s, type=%s, pos=%d, requestId=%d", data, uuid, type, position, requestId));
      else log.trace(String.format("could not find persisted object for uuid=%s, type=%s, pos=%d, requestId=%d", uuid, type, position, requestId));
    }
    return deserialize(data, type);
  }

  /**
   * Deserialize the object of the psecified type.
   * @param data holds the serialized object.
   * @param type the type of the object.
   * @return the deserialized object.
   * @throws Exception if any error occurs.
   */
  private static Object deserialize(final DataLocation data, final PersistenceObjectType type) throws Exception {
    if (data == null) return null;
    DataLocation dl = data;
    if (type == PersistenceObjectType.TASK) {
      final ServerTask task = (ServerTask) IOHelper.unwrappedData(data);
      dl = task.getInitialTask();
    }
    final JPPFByteArrayOutputStream baos = new JPPFByteArrayOutputStream(dl.getSize());
    final long l = StreamUtils.copyStream(dl.getInputStream(), baos, true);
    if (debugEnabled) log.debug("copied {} bytes", l);
    return baos.toByteArray();
  }

  @Override
  public boolean deleteLoadRequest(final long requestId) throws Exception {
    if (debugEnabled) log.debug(String.format("deleting load request with id=%d", requestId));
    return loadRequests.remove(requestId) != null;
  }
}
