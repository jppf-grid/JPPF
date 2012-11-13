/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package test.org.jppf.client;

import static org.junit.Assert.assertNotNull;

import org.jppf.client.JPPFJob;
import org.jppf.server.protocol.JPPFTask;
import org.junit.Test;

import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for <code>JPPFJob</code>.
 * @author Laurent Cohen
 */
public class TestJPPFJob
{
  /**
   * Invocation of the <code>JPPFClient()</code> constructor.
   * @throws Exception if any error occurs
   */
  @Test(timeout=5000)
  public void testGetTaskObject() throws Exception
  {
    JPPFJob job = new JPPFJob();
    JPPFTask task = (JPPFTask) job.addTask(new SimpleRunnable());
    assertNotNull(task);
    assertNotNull(task.getTaskObject());
    JPPFTask task2 = (JPPFTask) job.addTask(new SimpleTask());
    assertNotNull(task2);
    assertNotNull(task2.getTaskObject());
  }
}
