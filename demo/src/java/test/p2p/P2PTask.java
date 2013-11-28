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
package test.p2p;

import org.jppf.node.NodeRunner;
import org.jppf.server.protocol.JPPFTask;

/**
 * This class is a template for a standard JPPF task.
 * @author Laurent Cohen
 */
public class P2PTask extends JPPFTask
{
  /**
   * 
   */
  private final long duration;
  /**
   * Uuid of the node in which this task is executted.
   */
  private String nodeUuid = null;

  /**
   * Perform initializations.
   * @param duration the duration.
   */
  public P2PTask(final long duration)
  {
    this.duration = duration;
  }

  @Override
  public void run()
  {
    try
    {
      nodeUuid = NodeRunner.getUuid();
      Thread.sleep(duration);
      setResult("the execution was performed successfully");
    }
    catch (Exception e)
    {
      setException(e);
    }
  }

  /**
   * Get the uuid of the node in which this task is executted.
   * @return th euuid as a string.
   */
  public String getNodeUuid()
  {
    return nodeUuid;
  }
}
