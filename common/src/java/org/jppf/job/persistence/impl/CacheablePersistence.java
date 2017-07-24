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

package org.jppf.job.persistence.impl;

import java.io.InputStream;
import java.util.*;

import org.jppf.io.MultipleBuffersLocation;
import org.jppf.job.persistence.*;
import org.jppf.utils.ReflectionHelper;
import org.jppf.utils.collections.SoftLRUCache;
import org.jppf.utils.streams.*;
import org.slf4j.*;

/**
 * A caching wrapper for any other implementation of {@link JobPersistence}.
 * <p>The cached artifacts are those handled by the {@link #load(Collection) load()} and {@link #store(Collection) store()} methods,
 * that is, job headers, data providers, tasks and task results. The cache is an LRU cache of soft references to the artifacts.
 * It guarantees that all its entries will be cleared before an out of memory error is raised. Additionally the cache has a capacity which can be
 * specified in the configuration and which defaults to 1024.
 * <p>This cacheable persistence is configured as follows:
 * <pre style="padding: 5px 5px 5px 0px; display: inline-block; background-color: #E0E0F0">
 * <span style="color: green"># shorten the configuration value for clarity</span>
 * wrapper = org.jppf.job.persistence.impl.CacheablePersistence
 * <span style="color: green"># cacheable persistence with default capacity of 1024</span>
 * jppf.job.persistence = ${wrapper} &lt;actual_persistence&gt; &lt;param1&gt; ... &lt;paramN&gt;
 * <span style="color: green"># cacheable persistence with a specified capacity</span>
 * jppf.job.persistence = ${wrapper} &lt;capacity&gt; &lt;actual_persistence&gt; &lt;param1&gt; ... &lt;paramN&gt;</pre>
 * <p>Here is a concrete example wrapping a default database persistence:
 * <pre style="padding: 5px 5px 5px 0px; display: inline-block; background-color: #E0E0F0">
 * <span style="color: green"># shortcut for the package name</span>
 * pkg = org.jppf.job.persistence.impl
 * <span style="color: green"># cacheable database persistence with a capacity of 10000,</span>
 * <span style="color: green"># a table named 'JPPF_TEST' and datasource named 'JobDS'</span>
 * jppf.job.persistence = ${pkg}.CacheablePersistence 10000 ${pkg}.DefaultDatabasePersistence JPPF_TEST JobDS</pre>
 * @author Laurent Cohen
 */
public class CacheablePersistence implements JobPersistence {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(CacheablePersistence.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * A cache of persisted objects.
   */
  private final Map<PersistenceInfoKey, PersistenceInfo> cache;
  /**
   * The actual persistence implementation to which operations are delegated.
   */
  private final JobPersistence delegate;

  /**
   * Initialize this persistence with the specified parameters.
   * @param params if the first parameter is a number, then it represents the cache size, and the remaining parameters represent the wrapped
   * persistence implementation. Otherwise it represents the wrapped persistence and the remaining parameters are those of the wrapped persistence. 
   * @throws JobPersistenceException if any error occurs.
   */
  public CacheablePersistence(final String... params) throws JobPersistenceException {
    if ((params == null) || (params.length < 1) || (params[0] == null)) throw new JobPersistenceException("too few parameters");
    int n = 1024;
    String[] forwardParams = null;
    try {
      n = Integer.valueOf(params[0]);
      forwardParams = new String[params.length - 1];
      System.arraycopy(params, 1, forwardParams, 0, params.length - 1);
    } catch (@SuppressWarnings("unused") NumberFormatException e) {
      forwardParams = params;
    }
    if (n < 1) n = 1024;
    this.delegate = ReflectionHelper.invokeDefaultOrStringArrayConstructor(JobPersistence.class, getClass().getSimpleName(), forwardParams);
    if (delegate == null) throw new JobPersistenceException("could not create job persistence " + Arrays.asList(params));
    cache = new SoftLRUCache<>(n);
  }

  @Override
  public void store(final Collection<PersistenceInfo> infos) throws JobPersistenceException {
    if (debugEnabled) log.debug("storing {}", infos);
    try  {
      delegate.store(infos);
      synchronized (cache) {
        for (PersistenceInfo info: infos) cache.put(new PersistenceInfoKey(info), info);
      }
    } catch(JobPersistenceException e) {
      throw e;
    } catch(Exception e) {
      throw new JobPersistenceException(e);
    }
  }

  @Override
  public List<InputStream> load(final Collection<PersistenceInfo> infos) throws JobPersistenceException {
    try {
      Map<PersistenceInfoKey, PersistenceInfo> map = new LinkedHashMap<>();
      Map<PersistenceInfoKey, PersistenceInfo> toLoad = new LinkedHashMap<>();
      for (PersistenceInfo info: infos) {
        PersistenceInfoKey key = new PersistenceInfoKey(info);
        map.put(key, info);
      }
      synchronized (cache) {
        for (Map.Entry<PersistenceInfoKey, PersistenceInfo> entry: map.entrySet()) {
          PersistenceInfoKey key = entry.getKey();
          PersistenceInfo cachedInfo = cache.get(key);
          if (cachedInfo != null) map.put(key, cachedInfo);
          else toLoad.put(key, entry.getValue());
        }
        if (!toLoad.isEmpty()) {
          List<InputStream> streamList = delegate.load(toLoad.values());
          Iterator<InputStream> it = streamList.iterator();
          for (Map.Entry<PersistenceInfoKey, PersistenceInfo> entry: toLoad.entrySet()) {
            InputStream is = it.next();
            if (is != null) {
              PersistenceInfoKey key = entry.getKey();
              JPPFByteArrayOutputStream baos = new JPPFByteArrayOutputStream();
              StreamUtils.copyStream(is, baos, true);
              PersistenceInfo cachedInfo = new PersistenceInfoImpl(key.uuid, null, key.type, key.position, new MultipleBuffersLocation(baos.toByteArray()));
              cache.put(key, cachedInfo);
              map.put(key, cachedInfo);
            }
          }
        }
      }
      List<InputStream> result = new ArrayList<>(map.size());
      for (Map.Entry<PersistenceInfoKey, PersistenceInfo> entry: map.entrySet()) result.add(entry.getValue().getInputStream());
      return result;
    } catch (JobPersistenceException e) {
      throw e;
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new JobPersistenceException(e);
    }
  }

  @Override
  public List<String> getPersistedJobUuids() throws JobPersistenceException {
    return delegate.getPersistedJobUuids();
  }

  @Override
  public int[] getTaskPositions(final String jobUuid) throws JobPersistenceException {
    return delegate.getTaskPositions(jobUuid);
  }

  @Override
  public int[] getTaskResultPositions(final String jobUuid) throws JobPersistenceException {
    return delegate.getTaskResultPositions(jobUuid);
  }

  @Override
  public void deleteJob(final String jobUuid) throws JobPersistenceException {
    PersistenceInfoKey key = new PersistenceInfoKey(jobUuid, PersistenceObjectType.JOB_HEADER, -1);
    synchronized (cache) {
      cache.remove(key);
    }
    delegate.deleteJob(jobUuid);
  }

  @Override
  public boolean isJobPersisted(final String jobUuid) throws JobPersistenceException {
    PersistenceInfoKey key = new PersistenceInfoKey(jobUuid, PersistenceObjectType.JOB_HEADER, -1);
    synchronized (cache) {
      if (cache.get(key) != null) return true;
    }
    return delegate.isJobPersisted(jobUuid);
  }
}
