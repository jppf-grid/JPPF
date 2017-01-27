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

package test.serialization;

import java.util.List;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.ExceptionUtils;

/**
 * 
 * @author Laurent Cohen
 */
public class NonSerializableTaskRunner {
  /**
   *
   * @param args not used.
   */
  public static void main(final String[] args) {
    try (JPPFClient client = new JPPFClient()) {
      JPPFJob job = new JPPFJob();
      job.setName("job name");
      job.add(new NonSerializableTask(false));
      job.add(new NonSerializableTask(false));
      List<Task<?>> results = client.submitJob(job);
      for (Task<?> task : results) {
        if (task.getThrowable() != null) System.out.printf("task %d got exception: %s%n", task.getPosition(), ExceptionUtils.getStackTrace(task.getThrowable()));
        else System.out.printf("task %d got result: %s%n", task.getPosition(), task.getResult());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
