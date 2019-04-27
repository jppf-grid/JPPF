/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package test.org.jppf.job.persistence;

import static org.junit.Assert.fail;

import java.util.*;

import org.apache.log4j.Level;
import org.jppf.client.JPPFJob;
import org.jppf.io.*;
import org.jppf.job.persistence.*;
import org.jppf.job.persistence.impl.DefaultFilePersistence;
import org.jppf.node.protocol.*;
import org.jppf.utils.ExceptionUtils;
import org.jppf.utils.concurrent.DebuggableThread;
import org.junit.*;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Non-regression test for bug <a href="https://www.jppf.org/tracker/tbg/jppf/issues/JPPF-588">JPPF-588 Concurrent operations with DefaultFilePersistence job persistence result in exceptions</a>. 
 * @author Laurent Cohen
 */
public class TestConcurrentFilePersistence extends BaseTest {
  /**
   * The default file persistence manager
   */
  DefaultFilePersistence persistence;
  /**
   * number of threads and tasks per thread.
   */
  final int taskPerThread = 30, nbThreads = 10, nbTasks = taskPerThread * nbThreads;
  /**
   * The job to persist.
   */
  JPPFJob job;
  /**
   * The job tasks to persist and their results.
   */
  final List<PersistenceInfo> taskInfos = new ArrayList<>(nbTasks), resultInfos = new ArrayList<>(nbTasks);
  /**
   * The job header expected by the persistence (normally sent by the client to the driver).
   */
  final TaskBundle header = new JPPFTaskBundle();
  /**
   * The job header and DataProvider to persist.
   */
  PersistenceInfo headerInfo, dpInfo;

  /**
   * 
   * @throws Exception if any error occurs.
   */
  @Before
  public void setup() throws Exception {
    BaseSetup.setLoggerLevel(Level.DEBUG, "org.jppf.job.persistence");
    persistence = new DefaultFilePersistence("persistence");
    job = BaseTestHelper.createJob("testFilePersistence", false, nbTasks, LifeCycleTask.class, 0L);
    header.setUuid(job.getUuid());
    header.setParameter(BundleParameter.CLIENT_BUNDLE_ID, 1L);
    header.setInitialTaskCount(job.getTaskCount());
    
    headerInfo = new PersistenceInfoImpl(job.getUuid(), header, PersistenceObjectType.JOB_HEADER, -1, IOHelper.serializeData(header));
    dpInfo = new PersistenceInfoImpl(job.getUuid(), header, PersistenceObjectType.DATA_PROVIDER, -1, IOHelper.serializeData(null));

    for (int i=0; i<nbTasks; i++) {
      final DataLocation taskData = IOHelper.serializeData(job.getJobTasks());
      taskInfos.add(new PersistenceInfoImpl(job.getUuid(), header, PersistenceObjectType.TASK, i, taskData));
      resultInfos.add(new PersistenceInfoImpl(job.getUuid(), header, PersistenceObjectType.TASK_RESULT, i, taskData));
    }
  }

  /**
   * Test that concurrent requests to delete a job do not result in exceptions due to the underlying file system.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testConcurrentDeletion() throws Exception {
    final BaseThread[] threads = new BaseThread[2 * nbThreads];
    final List<PersistenceInfo> list = new ArrayList<>();
    list.add(headerInfo);
    list.add(dpInfo);
    list.addAll(taskInfos);
    persistence.store(list);

    for (int i=0; i<nbThreads; i++) {
      final List<PersistenceInfo> storeList = resultInfos.subList(i * taskPerThread, (i + 1) * taskPerThread);
      threads[i] = new StoreThread("DeleteThread-" + i, storeList);
    }
    for (int i=0; i<nbThreads; i++) threads[nbThreads + i] = new DeleteThread("StoreThread-" + i, job.getUuid());
    for (final BaseThread thread: threads) thread.start();
    for (final BaseThread thread: threads) thread.join();
    for (final BaseThread thread: threads) {
      if (thread.exception != null) fail(String.format("thread '%s' failed with %s", thread.getName(), ExceptionUtils.getMessage(thread.exception)));
    }
  }

  /** */
  abstract static class BaseThread extends DebuggableThread {
    /** */
    public Exception exception;

    /**
     * @param name the name of this thread.
     */
    public BaseThread(final String name) {
      super(name);
    }

    @Override
    public void run() {
      try {
        execute();
      } catch (final Exception e) {
        exception = e;
        print(false, false, "thread '%s' failed with exception:\n%s", getName(), ExceptionUtils.getStackTrace(e));
      }
    }

    /**
     * @throws Exception if any error occurs.
     */
    protected abstract void execute() throws Exception;
  }

  /** */
  class StoreThread extends BaseThread {
    /** */
    final List<PersistenceInfo> infos;

    /**
     * @param name the name of this thread.
     * @param infos the job elements to store.
     */
    public StoreThread(final String name, final List<PersistenceInfo> infos) {
      super(name);
      this.infos = infos;
    }

    @Override
    public void execute() throws Exception {
      persistence.store(infos);
    }
  }

  /** */
  class DeleteThread extends BaseThread {
    /** */
    final String uuid;

    /**
     * @param name the name of this thread.
     * @param uuid uuid of the job to delete.
     */
    public DeleteThread(final String name, final String uuid) {
      super(name);
      this.uuid = uuid;
    }

    @Override
    public void execute() throws Exception {
      persistence.deleteJob(uuid);
    }
  }
}
