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

package test.org.jppf.test.setup.jca;

import java.lang.reflect.Constructor;

import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;

/**
 * Helper methods for setting up and cleaning the environment before and after testing.
 * @author Laurent Cohen
 */
public class JCATestHelper
{
  /**
   * Message used for successful task execution.
   */
  public static final String EXECUTION_SUCCESSFUL_MESSAGE = "execution successful";

  /**
   * Create a task with the specified parameters.
   * The type of the task is specified via its class, and the constructor to
   * use is specified based on the number of parameters.
   * @param id the task id.
   * @param taskClass the class of the tasks to add to the job.
   * @param params the parameters for the tasks constructor.
   * @return an <code>Object</code> representing a task.
   * @throws Exception if any error occurs.
   */
  public static Object createTask(final String id, final Class<?> taskClass, final Object...params) throws Exception
  {
    int nbArgs = (params == null) ? 0 : params.length;
    Constructor constructor = findConstructor(taskClass, nbArgs);
    Object o = constructor.newInstance(params);
    if (o instanceof JPPFTask) ((JPPFTask) o).setId(id);
    return o;
  }

  /**
   * Find a constructor with the specfied number of parameters for the specified class.
   * @param taskClass the class of the tasks to add to the job.
   * @param nbParams the number of parameters for the tasks constructor.
   * @return a <code>constructor</code> instance.
   * @throws Exception if any error occurs if a construcotr could not be found.
   */
  public static Constructor findConstructor(final Class<?> taskClass, final int nbParams) throws Exception
  {
    Constructor[] constructors = taskClass.getConstructors();
    Constructor constructor = null;
    for (Constructor c: constructors)
    {
      if (c.getParameterTypes().length == nbParams)
      {
        constructor = c;
        break;
      }
    }
    if (constructor == null) throw new IllegalArgumentException("couldn't find a constructor for class " + taskClass.getName() + " with " + nbParams + " arguments");
    return constructor;
  }

  /**
   * Create a job with the specified parameters.
   * The type of the tasks is specified via their class, and the constructor to
   * use is specified based on the number of parameters.
   * @param name the job's name.
   * @param blocking specifies whether the job is blocking.
   * @param broadcast specifies whether the job is a broadcast job.
   * @param nbTasks the number of tasks to add to the job.
   * @param taskClass the class of the tasks to add to the job.
   * @param params the parameters for the tasks constructor.
   * @return a <code>JPPFJob</code> instance.
   * @throws Exception if any error occurs.
   */
  public static JPPFJob createJob(final String name, final boolean blocking, final boolean broadcast, final int nbTasks, final Class<?> taskClass, final Object...params) throws Exception
  {
    JPPFJob job = new JPPFJob();
    job.setName(name);
    int nbArgs = (params == null) ? 0 : params.length;
    Constructor constructor = findConstructor(taskClass, nbArgs);
    for (int i=1; i<=nbTasks; i++)
    {
      Object o = constructor.newInstance(params);
      if (o instanceof JPPFTask) ((JPPFTask) o).setId(job.getName() + " - task " + i);
      job.addTask(o);
    }
    job.setBlocking(blocking);
    job.getSLA().setBroadcastJob(broadcast);
    if (!blocking) job.setResultListener(new JPPFResultCollector(job));
    return job;
  }
}
