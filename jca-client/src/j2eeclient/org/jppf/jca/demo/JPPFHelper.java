/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.jca.demo;

import java.util.*;

import javax.naming.*;
import javax.resource.ResourceException;
import javax.resource.cci.ConnectionFactory;

import org.jppf.client.*;
import org.jppf.jca.cci.*;
import org.jppf.node.protocol.Task;

/**
 * Utility class for obtaining and releasing Resource adapter connections.
 * @author Laurent Cohen
 */
public class JPPFHelper
{
  /**
   * 
   */
  private static Map<String, JPPFJob> statusMap = new Hashtable<>();
  /**
   * JNDI name of the JPPFConnectionFactory.
   * <p>This value is dependent on the application server used:
   * <ul>
   * <li>On Apache Geronimo: "<b>jca:/JPPF/jca-client/JCAManagedConnectionFactory/eis/JPPFConnectionFactory</b>"</li>
   * <li>On JBoss: "<b>java:eis/JPPFConnectionFactory</b>"</li>
   * <li>On Websphere: "<b>java:comp/env/eis/JPPFConnectionFactory</b>"</li>
   * <li>All other supported servers: "<b>eis/JPPFConnectionFactory</b>"</li>
   * <ul>
   */
  public static final String JNDI_NAME = "eis/JPPFConnectionFactory";

  /**
   * Obtain a JPPF connection from the resource adapter's connection pool.
   * The obtained connection must be closed by the caller of this method, once it is done using it.
   * @return a <code>JPPFConnection</code> instance.
   * @throws NamingException if the connection factory lookup failed.
   * @throws ResourceException if a connection could not be obtained.
   */
  public static JPPFConnection getConnection() throws NamingException, ResourceException
  {
    return getConnection(JNDI_NAME);
  }

  /**
   * Obtain a JPPF connection from the resource adapter's connection pool.
   * The obtained connection must be closed by the caller of this method, once it is done using it.
   * @param jndiName the jNDI name of the JPPF connection factory (application server-dependent).
   * @return a <code>JPPFConnection</code> instance.
   * @throws NamingException if the connection factory lookup failed.
   * @throws ResourceException if a connection could not be obtained.
   */
  public static JPPFConnection getConnection(final String jndiName) throws NamingException, ResourceException
  {
    InitialContext context = new InitialContext();
    Object objref = context.lookup(jndiName);
    JPPFConnectionFactory cf;
    if (objref instanceof JPPFConnectionFactory) cf = (JPPFConnectionFactory) objref;
    else cf = (JPPFConnectionFactory) javax.rmi.PortableRemoteObject.narrow(objref, ConnectionFactory.class);
    return (JPPFConnection) cf.getConnection();
  }

  /**
   * Close (release) the specified connection.
   * @param connection the connection to close.
   * @throws ResourceException if the connection could not be closed.
   */
  public static void closeConnection(final JPPFConnection connection) throws ResourceException
  {
    connection.close();
  }

  /**
   * Get the map used to lookup the jobs status.
   * @return a mapping of jobs to their uuid.
   */
  public static Map<String, JPPFJob> getStatusMap()
  {
    return statusMap;
  }

  /**
   * Get the status of the specified job.
   * @param uuid the job uuid.
   * @return a mapping of jobs to their uuid.
   */
  public static String getStatus(final String uuid)
  {
    JPPFJob job = statusMap.get(uuid);
    if (job == null) return "no job with this uuid";
    JobStatus status = job.getStatus();
    return status == null ? "unknown" : status.toString();
  }

  /**
   * Get the name of the specified job.
   * @param uuid the job uuid.
   * @return a mapping of jobs to their uuid.
   */
  public static String getJobName(final String uuid)
  {
    JPPFJob job = statusMap.get(uuid);
    if (job == null) return "no job with this uuid";
    return job.getName();
  }

  /**
   * Format the results of the specified job.
   * @param uuid the uuid of the job.
   * @return the formatted resutls as a string.
   */
  public static String getMessage(final String uuid)
  {
    JPPFConnection connection = null;
    String msg = null;
    JPPFJob job = statusMap.remove(uuid);
    if (job == null) return "no job with this id";
    List<Task<?>> results = job.getAllResults();
    if (results == null) msg = "job is not in queue anymore";
    else
    {
      StringBuilder sb = new StringBuilder();
      for (Task task: results)
      {
        if (task.getThrowable() == null) sb.append(task.getResult());
        else sb.append("task [").append(task.getId()).append("] ended in error: ").append(task.getThrowable().getMessage());
        sb.append("<br/>");
      }
      msg = sb.toString();
    }
    return msg;
  }
}
