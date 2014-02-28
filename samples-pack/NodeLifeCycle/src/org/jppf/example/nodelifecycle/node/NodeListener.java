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

package org.jppf.example.nodelifecycle.node;

import java.sql.Connection;
import java.util.Properties;
import java.util.concurrent.*;

import javax.sql.DataSource;
import javax.transaction.Status;

import org.jppf.node.event.*;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.atomikos.jdbc.nonxa.AtomikosNonXADataSourceBean;

/**
 * A {@link NodeLifeCycleListener} implementation that starts a transaction just before
 * the execution of a job in the node, and commits it upon job completion.
 * If the node is terminated cleanly before the job completes (e.g. via the JPPF console or
 * management APIs), or if the node gets disconnected from the server, the transaction will be
 * rolled back. If the node crashes suddenly, rollback will be performed at its next startup.
 * @author Laurent Cohen
 */
public class NodeListener implements NodeLifeCycleListener
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeListener.class);
  /**
   * The XA data source to use.
   */
  private static DataSource dataSource = null;
  /**
   * Single thread in which all transaction-related operations are performed.
   * This is necessary due to the transaction / thread context association, and allows us
   * to have multiple threads writing to the DB within the same transaction.
   */
  private static ExecutorService executor = Executors.newSingleThreadExecutor();

  @Override
  public void nodeStarting(final NodeLifeCycleEvent event)
  {
    output("node ready to process jobs");
    // start a transaction to activate recovery
    // so we can process pending transactions after a node crash
    startTransaction(false);
    // force transaction rollback, since the job is resubmitted by JPPF.
    endTransaction(true);
  }

  @Override
  public void nodeEnding(final NodeLifeCycleEvent event)
  {
    output("node ending");
    endTransaction(true);
  }

  @Override
  public void jobHeaderLoaded(final NodeLifeCycleEvent event)
  {
  }

  @Override
  public void jobStarting(final NodeLifeCycleEvent event)
  {
    output("node starting job '" + event.getJob().getName() + "'");
    startTransaction(false);
  }

  @Override
  public void jobEnding(final NodeLifeCycleEvent event)
  {
    output("node finished job '" + event.getJob().getName() + "'");
    endTransaction(false);
  }

  /**
   * Get or initialize the XA data source.
   * @return a {@link DataSource} instance.
   */
  private static DataSource createXADataSource()
  {
    AtomikosDataSourceBean ds =  new AtomikosDataSourceBean();
    Properties props = ds.getXaProperties();
    /*
		// PostgreSQL Properties
		// !!! on PostgreSQL, the server configuration property "maxPreparedConnections"
		// must be set to a value > 0
		ds.setXaDataSourceClassName("org.postgresql.xa.PGXADataSource");
		props.setProperty("user", "jppf");
		props.setProperty("password", "jppf");
		props.setProperty("databaseName", "jppf_samples");
		props.setProperty("serverName", "localhost");
		props.setProperty("portNumber", "5432");
		// MySQL Properties
		ds.setXaDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlXADataSource");
		props.setProperty("user", "jppf");
		props.setProperty("password", "jppf");
		props.setProperty("serverName", "localhost");
		props.setProperty("port", "3306");
		props.setProperty("databaseName", "jppf_samples");
		props.setProperty("pinGlobalTxToPhysicalConnection", "true");
     */
    // H2 Properties
    ds.setXaDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
    props.setProperty("user", "jppf");
    props.setProperty("password", "jppf");
    props.setProperty("URL", "jdbc:h2:tcp://localhost:9092/./jppf_samples;SCHEMA=PUBLIC");

    // common properties
    ds.setTestQuery("select count(id) from task_result");
    ds.setUniqueResourceName("jppf_samples_ds");
    ds.setPoolSize(10);
    return ds;
  }

  /**
   * Get or initialize the non-XA data source.
   * @return a {@link DataSource} instance.
   */
  private static DataSource createNonXADataSource()
  {
    AtomikosNonXADataSourceBean ds =  new AtomikosNonXADataSourceBean();
    ds.setUser("jppf");
    ds.setPassword("jppf");

    /*
		// MySQL Properties
		ds.setDriverClassName("com.mysql.jdbc.Driver");
		ds.setUrl("jdbc:mysql://localhost:3306/jppf_samples");
     */
    // H2 Properties
    ds.setDriverClassName("org.h2.Driver");
    ds.setUrl("jdbc:h2:tcp://localhost:9092/./jppf_samples;SCHEMA=PUBLIC");

    ds.setUniqueResourceName("jppf_samples_ds");
    ds.setPoolSize(10);
    return ds;
  }

  /**
   * Get or initialize the XA data source.
   * @return a {@link DataSource} instance.
   */
  public synchronized static DataSource getDataSource()
  {
    if (dataSource == null)
    {
      //dataSource = createNonXADataSource();
      dataSource = createXADataSource();
    }
    return dataSource;
  }

  /**
   * Utility method to get a connection. The connection is obtained from the transaction thread.
   * @return Connection the connection.
   * @throws Exception if any error occurs.
   */
  public static synchronized Connection getConnection() throws Exception
  {
    return getDataSource().getConnection();
  }

  /**
   * Utility method to start a transaction.
   * The transaction is created/started from the transaction thread.
   * @param rollbackOnly determines whether the transaction should be in rollback only mode.
   * This is used to discard logged transactions that would remain after a node crash.
   */
  public static void startTransaction(final boolean rollbackOnly)
  {
    Exception e = submit(new StartTransactionTask(rollbackOnly));
    if (e != null) output(ExceptionUtils.getStackTrace(e));
  }

  /**
   * Utility method to terminate the transaction. The transaction is committed/rolled back from the transaction thread.
   * Upon return from this method, the transaction will be terminated.
   * @param rollback indicates if an error has occurred or not. If true, the transaction will be rolled back.
   * If false, the transaction will be committed.
   */
  public synchronized static void endTransaction(final boolean rollback)
  {
    Exception e = submit(new EndTransactionTask(rollback));
    if (e != null) output(ExceptionUtils.getStackTrace(e));
  }

  /**
   * Submit a task for execution by the exceutor.
   * @param <T> the type of result returned by the task.
   * @param callable the task to execute.
   * @return the task's result.
   */
  public static <T> T submit(final Callable<T> callable)
  {
    T result = null;
    try
    {
      Future<T> f = executor.submit(callable);
      result = f.get();
    }
    catch(Exception e)
    {
      output(ExceptionUtils.getStackTrace(e));
    }
    return result;
  }

  /**
   * Starts a transaction.
   */
  private static class StartTransactionTask implements Callable<Exception>
  {
    /**
     * Indicates whether the transaction should only be rolled back.
     * This is used for recovery after a node crash.
     * If false, then the transaction may be either committed or rolled back.
     */
    private boolean rollbackOnly;

    /**
     * Initialize this task with the specified error flag.
     * @param rollbackOnly rollback / commit flag.
     */
    public StartTransactionTask(final boolean rollbackOnly)
    {
      this.rollbackOnly = rollbackOnly;
    }

    /**
     * Terminate the transaction and return an exception if any occurred.
     * @return Exception if any error occurred.
     */
    @Override
    public Exception call()
    {
      try
      {
        // create the datasource; it will be automatically enlisted in the transaction
        getDataSource();
        // start the atomikos transaction manager
        UserTransactionImp utx = new UserTransactionImp();
        utx.setTransactionTimeout(60);
        if (rollbackOnly) utx.setRollbackOnly();
        utx.begin();
        Connection c = getConnection();
        c.close();
        return null;
      }
      catch (Exception e)
      {
        return e;
      }
    }
  }

  /**
   * Performs a rollback or commit of the current transaction.
   */
  private static class EndTransactionTask implements Callable<Exception>
  {
    /**
     * Indicates if an error has occurred or not. If true, the transaction will be rolled back.
     * If false, the transaction will be committed.
     */
    private boolean rollback;

    /**
     * Initialize this task with the specified error flag.
     * @param rollback rollback / commit flag.
     */
    public EndTransactionTask(final boolean rollback)
    {
      this.rollback = rollback;
    }

    /**
     * Terminate the transaction and return an exception if any occurred.
     * @return Exception if any error occurred.
     */
    @Override
    public Exception call()
    {
      try
      {
        UserTransactionImp utx = new UserTransactionImp();
        if (utx.getStatus() == Status.STATUS_NO_TRANSACTION) output("WARNING: endTransaction() called outside a tx");
        else
        {
          output("INFO: transaction " + (rollback ? "rollback" : "commit"));
          if (rollback) utx.rollback();
          else utx.commit();
        }
        return null;
      }
      catch (Exception e)
      {
        return e;
      }
    }
  }

  /**
   * Print a message to the console and/or log file.
   * @param message - the message to print.
   */
  public static void output(final String message)
  {
    System.out.println(message);
    log.info(message);
  }
}
