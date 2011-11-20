/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.jca.spi;

import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.*;
import javax.security.auth.Subject;

import org.jppf.jca.cci.JPPFConnectionFactory;
import org.jppf.jca.util.JPPFAccessorImpl;
import org.jppf.jca.work.JPPFJcaClient;


/**
 * Implementation of the ManagedConnectionFactory interface.
 * @author Laurent Cohen
 */
public class JPPFManagedConnectionFactory extends JPPFAccessorImpl implements ManagedConnectionFactory, ResourceAdapterAssociation
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Handle to the resource adapter.
   */
  private transient ResourceAdapter resourceAdapter = null;

  /**
   * Default constructor.
   */
  public JPPFManagedConnectionFactory()
  {
    //System.out.println("creating managed connection factory, call stack:");
    //System.out.println(StringUtils.getStackTrace(new Exception()));
  }

  /**
   * Create a jca connection factory. This method is called by the application server.
   * @return a JPPFConnectionFactory instance.
   * @throws ResourceException if the connection factory could not be created.
   * @see javax.resource.spi.ManagedConnectionFactory#createConnectionFactory()
   */
  @Override
  public Object createConnectionFactory() throws ResourceException
  {
    JPPFConnectionFactory jcf = new JPPFConnectionFactory();
    if (jcf.getJppfClient() == null) jcf.setJppfClient(getJppfClient());
    return jcf;
  }

  /**
   * Create a jca connection factory using a specified connection manager.
   * @param manager the connection manager to use.
   * @return a JPPFConnectionFactory instance.
   * @throws ResourceException if the connection factory could not be created.
   * @see javax.resource.spi.ManagedConnectionFactory#createConnectionFactory(javax.resource.spi.ConnectionManager)
   */
  @Override
  public Object createConnectionFactory(final ConnectionManager manager) throws ResourceException
  {
    JPPFConnectionFactory jcf = new JPPFConnectionFactory(this, manager);
    if (jcf.getJppfClient() == null) jcf.setJppfClient(getJppfClient());
    return jcf;
  }

  /**
   * Create a managed connection.
   * @param subject not used.
   * @param cri not used.
   * @return a <code>JPPFManagedConnection</code> instance.
   * @throws ResourceException if the managed connection could not be created.
   * @see javax.resource.spi.ManagedConnectionFactory#createManagedConnection(javax.security.auth.Subject, javax.resource.spi.ConnectionRequestInfo)
   */
  @Override
  public ManagedConnection createManagedConnection(final Subject subject, final ConnectionRequestInfo cri) throws ResourceException
  {
    JPPFManagedConnection conn = new JPPFManagedConnection();
    if (conn.getJppfClient() == null) conn.setJppfClient(getJppfClient());
    return conn;
  }

  /**
   * Returns a matched connection from the candidate set of connections.
   * @param set not used
   * @param subject not used
   * @param cri not used
   * @return a <code>JPPFManagedConnection</code> instance, or null if none is available.
   * @throws ResourceException always.
   * @see javax.resource.spi.ManagedConnectionFactory#matchManagedConnections(java.util.Set, javax.security.auth.Subject, javax.resource.spi.ConnectionRequestInfo)
   */
  @Override
  public ManagedConnection matchManagedConnections(final Set set, final Subject subject, final ConnectionRequestInfo cri)
  throws ResourceException
  {
    if (!set.isEmpty()) return (ManagedConnection) set.iterator().next();
    return null;
  }

  /**
   * Get the handle to the resource adapter.
   * @return a <code>ResourceAdapter</code>.
   * @see javax.resource.spi.ResourceAdapterAssociation#getResourceAdapter()
   */
  @Override
  public ResourceAdapter getResourceAdapter()
  {
    return resourceAdapter;
  }

  /**
   * Set the handle to the resource adapter.
   * @param resourceAdapter a <code>ResourceAdapter</code>.
   * @throws ResourceException if the resource adapter could not be set.
   * @see javax.resource.spi.ResourceAdapterAssociation#setResourceAdapter(javax.resource.spi.ResourceAdapter)
   */
  @Override
  public void setResourceAdapter(final ResourceAdapter resourceAdapter) throws ResourceException
  {
    this.resourceAdapter = resourceAdapter;
    setJppfClient(((JPPFResourceAdapter) resourceAdapter).getJppfClient());
  }

  /**
   * Determine whether 2 objects are equal.
   * @param obj the other object to compare to.
   * @return true if the objects are equal, false otherwise.
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final Object obj)
  {
    return super.equals(obj);
  }

  /**
   * Get this managed connection factory's hashcode.
   * @return the hashcode as an int.
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode()
  {
    return super.hashCode();
  }

  /**
   * Get the JPPF client used to submit tasks.
   * @return a <code>JPPFJcaClient</code> instance.
   */
  @Override
  public JPPFJcaClient getJppfClient()
  {
    if (jppfClient == null)
    {
      if (resourceAdapter != null) jppfClient = ((JPPFResourceAdapter) resourceAdapter).getJppfClient();
    }
    return jppfClient;
  }
}
