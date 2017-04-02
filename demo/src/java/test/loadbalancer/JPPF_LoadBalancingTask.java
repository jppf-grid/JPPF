/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
package test.loadbalancer;

import org.jppf.node.protocol.AbstractTask;

/**
 * @author Tomas
 *
 */
public class JPPF_LoadBalancingTask extends AbstractTask<String> {
  /** index of this task */
  private int count;
  /** Whether to simulate CPU usage */
  private boolean useCPU = true;
  /** duration of the task */
  private long duration = 200L;

  /**
   * @param count index of this task.
   */
  public JPPF_LoadBalancingTask(final int count) {
    this.count = count;
  }

  @Override
  public void run() {
    try {
      long taskStart = System.nanoTime();
      if (!useCPU) {
        Thread.sleep(duration);
      } else {
        for (long elapsed = 0L; elapsed < duration; elapsed = (System.nanoTime() - taskStart) / 1_000_000L) {
          String s = "";
          for (int i=0; i<10; i++) s += "A10";
        }
      }
      long elapsed = (System.nanoTime() - taskStart) / 1_000_000L;
      setResult(String.format("Task %d calculated in %,d ms in %s", count, elapsed, (isInNode() ? "a node" : "the client")));
    } catch (Exception e) {
      System.err.println(e.getMessage());

      setThrowable(e);
    } catch (Error e) {
      System.err.println(e.getMessage());
    }
  }
}
