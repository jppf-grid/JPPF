/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package test.emin;

import java.util.List;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.ExceptionUtils;

/** */
public class EminRunner {
  /**
   * @param args not used.
   */
  public static void main(final String[] args) {
    try (JPPFClient client = new JPPFClient()) {
      JPPFJob job = new JPPFJob();
      job.setBlocking(false);
      job.setName("job name");
      //job.add(new MyFutureTask(new TaskData())).setId("1");
      job.add(new MyTask(new TaskData())).setId("1");
      //job.add(new MyTask(null)).setId("2");
      job.addJobListener(new JobListenerAdapter() {
        @Override
        public void jobReturned(final JobEvent event) {
          processResults(event.getJobTasks());
        }
      });
      while (client.getConnectionPool() == null) Thread.sleep(1L);
      client.submitJob(job);
      Thread.sleep(2000L);
      job.cancel();
      job.awaitResults();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * @param results .
   */
  private static void processResults(final List<Task<?>> results) {
    for (Task<?> task : results) {
      if (task.getThrowable() != null) System.out.println("task " + task.getId() + " got exception: " + ExceptionUtils.getStackTrace(task.getThrowable()));
      else {
        MyResult res = (MyResult) task.getResult();
        System.out.println("task " + task.getId() + " got result: " + res);
        if (res != null) System.out.println("res.exeception = " + res.getException() + ", res.result = " + res.getResult());
      }
    }
  }
}
