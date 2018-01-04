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

package org.jppf.test.addons.jobtaskslistener;

import java.io.*;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.jppf.job.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.ExceptionUtils;

/**
 * This job task listener desrializes the tasks for each notification and prints a summary of
 * each task in a text file. There is one separate file per notification method.
 * @author Laurent Cohen
 */
public class MyJobTasksListener implements JobTasksListener {
  /**
   * Singleton instance of this class.
   */
  private static final MyJobTasksListener INSTANCE = new MyJobTasksListener();
  /** */
  public static final File DISPATCHED_FILE = new File("MyJobTasksListener.dispatched.tmp");
  /** */
  public static final File RETURNED_FILE = new File("MyJobTasksListener.returned.tmp");
  /** */
  public static final File RESULTS_FILE = new File("MyJobTasksListener.results.tmp");
  /** */
  public static final File[] ALL_FILES = { DISPATCHED_FILE, RESULTS_FILE, RETURNED_FILE };
  /** */
  public static final File LOCK_FILE = new File("MyJobTasksListener.lock");
  static {
    for (File f: ALL_FILES) f.deleteOnExit();
    LOCK_FILE.deleteOnExit();
  }

  /**
   * Default constructor, required for SPI.
   */
  public MyJobTasksListener() {
  }

  @Override
  public void tasksDispatched(final JobTasksEvent event) {
    printEventToFile(DISPATCHED_FILE, event);
  }

  @Override
  public void tasksReturned(final JobTasksEvent event) {
    printEventToFile(RETURNED_FILE, event);
  }

  @Override
  public void resultsReceived(final JobTasksEvent event) {
    printEventToFile(RESULTS_FILE, event);
  }

  /**
   * Print a message to the output console.
   * @param format the message format.
   * @param params the message parameters.
   */
  private static void print(final String format, final Object...params) {
    System.out.printf(format + "%n", params);
  }

  /**
   * Print the specified evnt data to the specified file.
   * @param file the file to write to.
   * @param event contains the information to write.
   */
  private static void printEventToFile(final File file, final JobTasksEvent event) {
    if ((file == null) || (event == null)) return;
    final List<ServerTaskInformation> tasksInfo = event.getTasks();
    final String name = event.getJobName();
    final String uuid = event.getJobUuid();
    synchronized(file) {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
        acquireLock(10_000L);
        for (final ServerTaskInformation sti: tasksInfo) {
          final Task<?> task = sti.getResultAsTask();
          // print message in format "<job uuid>;<job name>;<task id>;<task result>;<expiration count>;<resubmit count>;<max resubmits>"
          writer.append(String.format("%s;%s;%s;%s;%d;%d;%d%n", uuid, name, task.getId(), task.getResult(), sti.getExpirationCount(), sti.getResubmitCount(), sti.getMaxResubmits()));
        }
      } catch (final Exception e) {
        print("MyJobTasksListener: error wiritng tasks to file '%s': %s", file, ExceptionUtils.getStackTrace(e));
      } finally {
        releaseLock();
      }
    }
  }

  /**
   * @return the singleton instnce of this class.
   */
  public static MyJobTasksListener getInstance() {
    return INSTANCE;
  }

  /**
   * 
   * @param timeout the maximum time in millis after which this method throws an exception.
   * @throws Exception if any error occurs.
   */
  public static void acquireLock(final long timeout) throws Exception {
    final long start = System.currentTimeMillis();
    long elapsed = 0L;
    while (((elapsed = System.currentTimeMillis() - start) < timeout) && LOCK_FILE.exists()) Thread.sleep(50L);
    if (elapsed >= timeout) throw new TimeoutException(String.format("exceeded the timeout of %,d", timeout));
    LOCK_FILE.createNewFile();
  }

  /**
   * Delete the lock file to emulate releasing the lock.
   * @return {@code true} if the lock file could be deleted, {@code false} otherwise.
   */
  public static boolean releaseLock() {
    return LOCK_FILE.exists() && LOCK_FILE.delete();
  }
}
