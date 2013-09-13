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

package org.jppf.server.protocol.persistence;

import java.io.Serializable;

import org.jppf.node.protocol.Location;
import org.jppf.server.protocol.*;

/**
 * 
 * @author Laurent Cohen
 */
public class PersistentDispatchedTask implements Serializable
{
  /**
   * Client bundle that owns this task.
   */
  private long bundleId;
  /**
   * The position of this task within received bundle.
   */
  private int position;
  /**
   * The location of the serialized form of this task.
   */
  private Location dataLocation;
  /**
   * The state of this task.
   */
  private TaskState state;

  /**
   * Initialize this task from the specified server task.
   * @param serverTask the server task to get the data from.
   * @exclude
   */
  public PersistentDispatchedTask(final ServerTask serverTask)
  {
    this.state = serverTask.getState();
    this.position = serverTask.getPosition();
    this.dataLocation = PersistenceHelper.toLocation(serverTask.getDataLocation());
    this.bundleId = serverTask.getBundle().getId();
  }

  /**
   * Get the id of the lient bundle that owns this task.
   * @return the id as a long value.
   */
  public long getBundleId()
  {
    return bundleId;
  }

  /**
   * Set the id of the lient bundle that owns this task.
   * @param bundleId the id as a long value.
   */
  public void setBundleId(final long bundleId)
  {
    this.bundleId = bundleId;
  }

  /**
   * Get the position of this task within received bundle.
   * @return the position as an int value.
   */
  public int getPosition()
  {
    return position;
  }

  /**
   * Set the position of this task within received bundle.
   * @param position the position as an int value.
   */
  public void setPosition(final int position)
  {
    this.position = position;
  }

  /**
   * Get the location of the serialized form of this task.
   * @return a {@link Location} instance.
   */
  public Location getDataLocation()
  {
    return dataLocation;
  }

  /**
   * Set the location of the serialized form of this task.
   * @param dataLocation a {@link Location} instance.
   */
  public void setDataLocation(final Location dataLocation)
  {
    this.dataLocation = dataLocation;
  }

  /**
   * Get the state of this task.
   * @return a type safe {@link TaskState} enum value.
   */
  public TaskState getState()
  {
    return state;
  }

  /**
   * Set the state of this task.
   * @param state a type safe {@link TaskState} enum value.
   */
  public void setState(final TaskState state)
  {
    this.state = state;
  }
}
