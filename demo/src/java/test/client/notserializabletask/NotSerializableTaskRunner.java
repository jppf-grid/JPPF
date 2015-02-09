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

package test.client.notserializabletask;

import java.util.List;

import org.jppf.client.*;
import org.jppf.node.protocol.*;
import org.jppf.utils.ExceptionUtils;

/**
 * 
 * @author Laurent Cohen
 */
public class NotSerializableTaskRunner {

  /**
   *
   * @param args not used.
   */
  public static void main(final String[] args) {
    try (JPPFClient client = new JPPFClient()) {
      for (int i=1; i<=10_000; i++) {
        JPPFJob job = new JPPFJob();
        job.setName("NotSerializableTask-" + i);
        job.add(new NotSerializableTask());
        List<Task<?>> results = client.submitJob(job);
        System.out.printf("**** results for job %s:\n", job.getName());
        for (Task<?> task : results) {
          if (task.getThrowable() != null) System.out.println("got exception: " + ExceptionUtils.getStackTrace(task.getThrowable()));
          else System.out.println("got result: " + task.getResult());
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 
   */
  public static class NotSerializableTask extends AbstractTask<String> {
    /**
     * 
     */
    private NotSerializableObject nso = new NotSerializableObject();

    @Override
    public void run() {
    }
  }

  /**
   * 
   */
  public static class NotSerializableObject {
    /**
     * 
     */
    public String s = "some string";
  }
}
