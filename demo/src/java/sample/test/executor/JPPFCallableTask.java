/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package sample.test.executor;

import java.util.concurrent.*;

import org.jppf.client.JPPFClient;
import org.jppf.client.concurrent.JPPFExecutorService;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.server.protocol.JPPFTask;

/**
 * 
 * @param <V>
 * @author Laurent Cohen
 */
public abstract class JPPFCallableTask<V> extends JPPFTask implements Callable<V>
{
  /**
   * {@inheritDoc}
   */
  @Override
  public void run()
  {
    try
    {
      setResult(call());
    }
    catch(Exception e)
    {
      setThrowable(e);
    }
  }

  /**
   * 
   * @param args not used.
   */
  public static void main(final String[] args)
  {
    JPPFClient jppfClient = null;
    ExecutorService executor = null;
    try
    {
      jppfClient = new JPPFClient();
      executor = new JPPFExecutorService(jppfClient);
      MyTask myTask = new MyTask();
      myTask.setTimeoutSchedule(new JPPFSchedule(5000L));
      Future<String> future = executor.submit((Callable<String>) myTask);
      System.out.println("result: " + future.get());
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      if (executor != null) executor.shutdown();
      if (jppfClient != null) jppfClient.close();
    }
  }
}
