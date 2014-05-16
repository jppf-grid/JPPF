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

package org.jppf.example.nodelifecycle.client;

import java.sql.*;
import java.util.concurrent.Callable;

import org.jppf.example.nodelifecycle.node.NodeListener;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.ExceptionUtils;

/**
 * A sample task that writes a row into the database via a datasource.
 * @author Laurent Cohen
 */
public class DBTask extends JPPFTask
{
  /**
   * The time to wait after inserting a row in the database.
   */
  private long sleepTime;

  /**
   * Initialize this task with the specified sleep time.
   * @param sleepTime the time to wait after inserting a row in the database (in milliseconds).
   */
  public DBTask(final long sleepTime)
  {
    this.sleepTime = sleepTime;
  }

  /**
   * Execute this JPPF task.
   * The actual database operations are executed on the single transaction thread.
   * This means they are sequentialized and we loose some of the parallelism.
   * However, any non DB-related operation can be executed safely in a different thread.
   */
  @Override
  public void run()
  {
    try
    {
      // submit the SQL update as a task in the transaction's worker thread
      SQLCallable callable = new SQLCallable();
      Integer n = NodeListener.submit(callable);
      Throwable t = callable.throwable;
      // if the SQL update failed, we store the exception into the JPPF task
      if (t != null)
      {
        setThrowable(t);
        NodeListener.output(ExceptionUtils.getStackTrace(t));
      }
      // otherwise we set the execution result
      else
      {
        setResult("task " + getId() + " execution successful, sql return code = " + n);
        NodeListener.output(getResult().toString());
      }
      // sleep to allow enough time to kill the node and test the recovery mechanism.
      Thread.sleep(sleepTime);
    }
    catch (Exception e)
    {
      setThrowable(e);
    }
  }

  /**
   * This task is executed on the transaction thread and performs the database update.
   */
  public class SQLCallable implements Callable<Integer>
  {
    /**
     * An eventual throwable that may occur while executing the DB update.
     */
    public Throwable throwable = null;

    /**
     * Insert a row into the database.
     * @return the number of updated rows, or null if the operation failed.
     */
    @Override
    public Integer call()
    {
      Connection c = null;
      PreparedStatement ps = null;
      try
      {
        c = NodeListener.getDataSource().getConnection();
        String sql = "INSERT INTO task_result (task_id, message) VALUES(?, ?)";
        ps = c.prepareStatement(sql);
        ps.setString(1, getId());
        ps.setString(2, getId() + ": task execution successful");
        //NodeListener.output("before executing prepared statement: " + ps);
        int n = ps.executeUpdate();
        return n;
      }
      catch (Throwable t)
      {
        throwable = t;
        return null;
      }
      finally
      {
        try
        {
          if (ps != null) ps.close();
        }
        catch (Throwable t)
        {
          if (throwable == null) throwable = t;
        }
        try
        {
          if (c != null) c.close();
        }
        catch (Throwable t)
        {
          if (throwable == null) throwable = t;
        }
      }
    }
  }
}
