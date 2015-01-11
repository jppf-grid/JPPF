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

package test.org.jppf.node.protocol;

import static org.junit.Assert.*;

import java.util.List;

import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.junit.Test;

import test.org.jppf.test.setup.Setup1D1N1C;

/**
 * Unit tests for the {@link ScriptedTask} class.
 * @author Laurent Cohen
 */
public class TestScriptedTask extends Setup1D1N1C
{
  /** */
  private static final String MSG_PREFIX = "Hello JPPF ";

  /**
   * Test the execution of a scripted task with a simple groovy script.
   * The test includes verifcation that the groovy classes are loaded from the client or server classpath.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testSimpleGroovyScript() throws Exception {
    int nbTasks = 10;
    String msg = "Hello JPPF ";
    String script = "return '" + msg + "' + jppfTask.getId()";
    JPPFJob job = new JPPFJob();
    job.setName(ReflectionUtils.getCurrentMethodName());
    for (int i=0; i<nbTasks; i++) {
      job.add(new ScriptedTask<String>("groovy", script, "someId", null)).setId("(" + (i+1) + ")");
    }
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    for (int i=0; i<nbTasks; i++) {
      Task<?> task = results.get(i);
      Throwable t = task.getThrowable();
      assertNull("task has throwable: " + ExceptionUtils.getStackTrace(t), t);
      assertNotNull(task.getResult());
      assertEquals(msg + '(' + (i+1) + ')', task.getResult());
    }
  }
  /**
   * Test the execution of a scripted task with a simple javascript script.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testSimpleJavascript() throws Exception {
    int nbTasks = 10;
    String msg = "Hello JPPF ";
    String script = "function myFunc() { return '" + msg + "' + jppfTask.getId(); } myFunc();";
    JPPFJob job = new JPPFJob();
    job.setName(ReflectionUtils.getCurrentMethodName());
    for (int i=0; i<nbTasks; i++) {
      job.add(new ScriptedTask<String>("javascript", script, "someId", null)).setId("(" + (i+1) + ")");
    }
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    for (int i=0; i<nbTasks; i++) {
      Task<?> task = results.get(i);
      Throwable t = task.getThrowable();
      assertNull("task has throwable: " + ExceptionUtils.getStackTrace(t), t);
      assertNotNull(task.getResult());
      assertEquals(msg + '(' + (i+1) + ')', task.getResult());
    }
  }
}
