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

package sample.dist.taskcommunication;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.node.NodeRunner;
import org.jppf.server.protocol.JPPFTask;

import com.hazelcast.core.*;

/**
 * Sample task using a Hazelcast distributed queue.
 * @author Laurent Cohen
 */
public class MyTask extends JPPFTask implements ItemListener
{
  /**
   * Determines whether the task should stop processing.
   */
  private AtomicBoolean conditionReached = new AtomicBoolean(false);

  /**
   * Execute this task.
   * @see java.lang.Runnable#run()
   */
  @Override
  @SuppressWarnings("unchecked")
  public void run()
  {
    try
    {
      // add this task as an item listener on the queue
      getQueue().addItemListener(this, false);
      // if condition has been reached before, terminate
      if (!getQueue().isEmpty()) return;
      while (!conditionReached.get())
      {
        // ... your code here ...

        // if condition is reached by this task
        //if (<some condition>)
        {
          conditionReached.set(true);
          // put an item in the queue to notify other tasks and nodes
          getQueue().add("Condition reached");
        }
      }
    }
    finally
    {
      getQueue().removeItemListener(this);
    }
  }

  /**
   * Check that the distributed queue is empty.
   * This method lazily initializes the queue if required.
   * @return an IQueue instance (Hazel distributed queue).
   */
  @SuppressWarnings("unchecked")
  public IQueue getQueue()
  {
    String key = "MyDistyributedQueue";
    IQueue<Object> queue = (IQueue<Object>) NodeRunner.getPersistentData(key);
    // if the queue was not already present, we initialize it
    if (queue == null)
    {
      queue = Hazelcast.getQueue(key);
      // persist the queue reference in the node, so other tasks can access it.
      NodeRunner.setPersistentData(key, queue);
    }
    return queue;
  }

  /**
   * Called when an item is added to the queue.
   * @param item the item that was added.
   * @see com.hazelcast.core.ItemListener#itemAdded(java.lang.Object)
   */
  @Override
  public void itemAdded(final Object item)
  {
    conditionReached.set(true);
  }

  /**
   * This method does nothing.
   * @param item the item that was removed.
   * @see com.hazelcast.core.ItemListener#itemRemoved(java.lang.Object)
   */
  @Override
  public void itemRemoved(final Object item)
  {
  }

}
