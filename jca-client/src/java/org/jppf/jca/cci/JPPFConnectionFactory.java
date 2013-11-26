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

package org.jppf.jca.cci;

import javax.naming.*;
import javax.resource.*;
import javax.resource.cci.*;
import javax.resource.spi.ConnectionManager;

import org.jppf.jca.spi.*;
import org.jppf.jca.util.JPPFAccessorImpl;

/**
 * Implementation of the {@link javax.resource.cci.ConnectionFactory ConnectionFactory} interface for
 * the JPPF resource adapter.
 * @author Laurent Cohen
 */
public class JPPFConnectionFactory extends JPPFAccessorImpl implements ConnectionFactory
{
  /**
   * The default managed factory.
   */
  private JPPFManagedConnectionFactory factory = new JPPFManagedConnectionFactory();
  /**
   * The default connection manager.
   */
  private ConnectionManager manager = new JPPFConnectionManager();
  /**
   * 
   */
  private Reference ref;

  /**
   * Default constructor.
   * @exclude
   */
  public JPPFConnectionFactory()
  {
  }

  /**
   * Initialize this connection factory with a specified managed connection factory and connection manager..
   * @param factory the managed factory to use.
   * @param manager the connection manager to use.
   * @exclude
   */
  public JPPFConnectionFactory(final JPPFManagedConnectionFactory factory, final ConnectionManager manager)
  {
    this.factory = factory;
    if (factory.retrieveJppfClient() == null) assignJppfClient(factory.retrieveJppfClient());
    this.manager = manager;
  }

  @Override
  public Connection getConnection() throws ResourceException
  {
    JPPFConnection conn = (JPPFConnection) manager.allocateConnection(factory, null);
    if (conn == null) return null;
    if (conn.retrieveJppfClient() == null) conn.assignJppfClient(retrieveJppfClient());
    if (conn.isClosed()) conn.setAvailable();
    return conn;
  }

  @Override
  public Connection getConnection(final ConnectionSpec spec) throws ResourceException
  {
    return getConnection();
  }

  @Override
  public ResourceAdapterMetaData getMetaData() throws ResourceException
  {
    return new JPPFResourceAdapterMetaData();
  }

  @Override
  public RecordFactory getRecordFactory() throws ResourceException
  {
    return new JPPFRecordFactory();
  }

  @Override
  public void setReference(final Reference ref)
  {
    this.ref = ref;
  }

  @Override
  public Reference getReference() throws NamingException
  {
    return ref;
  }
}
