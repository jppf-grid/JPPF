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

import java.util.*;

import org.jppf.io.DataLocation;
import org.jppf.node.protocol.JPPFDistributedJob;

/**
 * This class wraps a task bundle to express it in terms of {@link org.jppf.io.DataLocation DataLocation}.
 * This allows the tasks data to be processed with the same semantics no matter where it is stored, comes from or goes to.
 * @author Laurent Cohen
 */
public class BundleWrapper implements ServerJob
{
  /**
   * The underlying task bundle.
   */
  private JPPFTaskBundle job = null;
  /**
   * The location of the data provider.
   */
  private DataLocation dataProvider = null;
  /**
   * The list of locations of the tasks.
   */
  private List<DataLocation> tasks = new LinkedList<DataLocation>();

  /**
   * Default constructor.
   */
  public BundleWrapper()
  {
  }

  /**
   * Initialize this bundle wrapper with the specified task bundle.
   * @param bundle the underlying task bundle for this wrapper.
   */
  public BundleWrapper(final JPPFTaskBundle bundle)
  {
    this.job = bundle;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JPPFDistributedJob getJob()
  {
    return job;
  }

  /**
   * Set the underlying task bundle.
   * @param job a <code>JPPFTaskBundle</code> instance.
   */
  public void setJob(final JPPFTaskBundle job)
  {
    this.job = job;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DataLocation getDataProvider()
  {
    return dataProvider;
  }

  /**
   * Get the location of the data provider.
   * @param dataProvider a <code>JPPFTaskBundle</code> instance.
   */
  public void setDataProvider(final DataLocation dataProvider)
  {
    this.dataProvider = dataProvider;
  }

  /**
   * Add a task to this bundle wrapper.
   * @param task the task to add.
   */
  public void addTask(final DataLocation task)
  {
    tasks.add(task);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<DataLocation> getTasks()
  {
    return tasks;
  }

  /**
   * Set the list of locations of the tasks.
   * @param tasks a list of <code>DataLocation</code> instances.
   */
  public void setTasks(final List<DataLocation> tasks)
  {
    this.tasks = tasks;
  }

  /**
   * Make a copy of this bundle wrapper.
   * @return a new <code>BundleWrapper</code> instance.
   */
  public BundleWrapper copy()
  {
    BundleWrapper wrapper = null;
    synchronized(this)
    {
      wrapper = new BundleWrapper(job.copy());
      for (DataLocation dl: tasks) wrapper.addTask(dl);
    }
    wrapper.setDataProvider(dataProvider.copy());
    return wrapper;
  }

  /**
   * Make a copy of this bundle wrapper containing only the first nbTasks tasks it contains.
   * @param nbTasks the number of tasks to include in the copy.
   * @return a new <code>BundleWrapper</code> instance.
   */
  public BundleWrapper copy(final int nbTasks)
  {
    BundleWrapper wrapper = null;
    synchronized(this)
    {
      wrapper = new BundleWrapper(job.copy(nbTasks));
      LinkedList<DataLocation> tmp = (LinkedList<DataLocation>) tasks;
      for (int i=0; i<nbTasks; i++) wrapper.addTask(tmp.removeFirst());
    }
    wrapper.setDataProvider(dataProvider.copy());
    return wrapper;
  }

  /**
   * Merge this bundle wrapper with another.
   * @param other the wrapper to merge with.
   * @param after determines whether the tasks from other should be added first or last.
   */
  public void merge(final ServerJob other, final boolean after)
  {
    int n = ((JPPFTaskBundle) other.getJob()).getTaskCount();
    job.setTaskCount(job.getTaskCount() + n);
    job.getSLA().setSuspended(other.getJob().getSLA().isSuspended());
    if (after)
    {
      for (DataLocation task: other.getTasks()) tasks.add(task);
    }
    else
    {
      for (int i=n-1; i>=0; i--) tasks.add(0, other.getTasks().get(i));
    }
  }
}
