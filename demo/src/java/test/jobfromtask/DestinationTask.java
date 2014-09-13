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
package test.jobfromtask;

import org.jppf.server.protocol.JPPFTask;

/**
 * Instances of this class are defined as tasks with a predefined execution length, specified at their creation.
 * @author Laurent Cohen
 */
public class DestinationTask extends JPPFTask
{
  /**
   * The input string.
   */
  private final String input;

  /**
   * Initialize this task.
   * @param input the input string.
   */
  public DestinationTask(final String input)
  {
    this.input = input;
  }

  /**
   * Perform the execution of this task.
   * @see sample.BaseDemoTask#doWork()
   */
  @Override
  public void run()
  {
    System.out.println("Starting destination task '" + getId() + "' : input = " + input);
    String s = "task '" + getId() + "' completed";
    System.out.println(s);
    setResult(s);
  }

  /**
   * Called when this task is cancelled.
   * @see org.jppf.server.protocol.JPPFTask#onCancel()
   */
  @Override
  public void onCancel()
  {
    String s = "task '" + getId() + "' has been cancelled";
    setResult(s);
    System.out.println(s);
  }
}
