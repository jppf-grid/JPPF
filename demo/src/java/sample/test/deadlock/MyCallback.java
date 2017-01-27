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

package sample.test.deadlock;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.client.*;
import org.jppf.location.*;
import org.jppf.node.protocol.ClassPath;

/**
 *
 */
class MyCallback extends JobStreamingCallback.Adapter {
  /**
   *
   */
  @SuppressWarnings("unused")
  private final JPPFClient client;
  /**
   *
   */
  @SuppressWarnings("unused")
  private final AtomicBoolean done = new AtomicBoolean(false);

  /**
   *
   * @param client .
   */
  public MyCallback(final JPPFClient client) {
    this.client = client;
  }

  @Override
  public void jobCreated(final JPPFJob job) {
    //job.getSLA().setBroadcastJob(true);
    //job.getSLA().setCancelUponClientDisconnect(false);
    addToClassPath(job, "dx-demo.jar");
  }

  @Override
  public void jobCompleted(final JPPFJob job, final JobStreamImpl jobStream) {
  }

  /**
   * Add a dexed file to the job's classpath.
   * @param job the job whose classpath to set.
   * @param filename the name of the dexed jar file.
   */
  private void addToClassPath(final JPPFJob job, final String filename) {
    ClassPath cp = job.getSLA().getClassPath();
    File file = new File(filename);
    try {
      Location<?> loc = new FileLocation(file).copyTo(new MemoryLocation(file.length()));
      cp.add(filename, loc);
    } catch (Exception e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }
}
