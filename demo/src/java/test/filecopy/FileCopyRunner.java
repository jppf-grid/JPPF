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

package test.filecopy;

import java.util.List;

import org.jppf.client.*;
import org.jppf.location.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;

/**
 *
 * @author Laurent Cohen
 */
public class FileCopyRunner {
  /**
   *
   * @param args not used.
   */
  public static void main(final String[] args) {
    try (JPPFClient client = new JPPFClient()) {
      final JPPFJob job = new JPPFJob();
      job.setName("copy file");
      job.add(new FileCopyTask());
      final List<Task<?>> results = client.submitJob(job);
      final Task<?> task = results.get(0);
      if (task.getThrowable() != null) System.out.println("got exception: " + ExceptionUtils.getStackTrace(task.getThrowable()));
      else System.out.println("result: " + task.getResult());
      final TimeMarker marker = new TimeMarker().start();
      final Location<?> in = new FileLocation("client.txt");
      in.copyTo(new FileLocation("node.txt"));
      System.out.printf("local copy done in %s%n", marker.stop().getLastElapsedAsString());
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
}
