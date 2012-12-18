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

package org.jppf.server.protocol;

import java.io.Serializable;

/**
 * Represents a serialized task used to transport tasks between clients, servers and nodes.
 * @author Laurent Cohen
 */
public class SerializedTask implements Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The position of the task in the original <code>JPPFJob</code>.
   */
  private final int position;
  /**
   * The serialized JPPF task.
   */
  private byte[] task = null;

  /**
   * Initialized this serialized tasks with the specified position and data.
   * @param position the position of the task in the original <code>JPPFJob</code>.
   * @param task the serialized JPPF task.
   */
  public SerializedTask(final int position, final byte[] task)
  {
    this.position = position;
    this.task = task;
  }

  /**
   * Get the serialized JPPF task.
   * @return the serialized task as an array of bytes.
   */
  public byte[] getTask()
  {
    return task;
  }

  /**
   * Set the serialized JPPF task.
   * @param task the serialized task as an array of bytes.
   */
  public void setTask(final byte[] task)
  {
    this.task = task;
  }

  /**
   * Get the position of the task in the original <code>JPPFJob</code>.
   * @return the position as an int.
   */
  public int getPosition()
  {
    return position;
  }
}
