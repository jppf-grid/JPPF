/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
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
import org.jppf.jca.util.JPPFAccessor;

/**
 * Implementation of the {@link javax.resource.cci.ConnectionFactory ConnectionFactory} interface for
 * the JPPF resource adapter.
 * @author Laurent Cohen
 */
public class JPPFConnectionFactory extends JPPFAccessor implements ConnectionFactory
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
   */
  public JPPFConnectionFactory()
  {
  }

  /**
   * Initialize this connection factorywith a specified managed connection factory and connection manager..
   * @param factory the managed factory to use.
   * @param manager the connection manager to use.
   */
  public JPPFConnectionFactory(JPPFManagedConnectionFactory factory, ConnectionManager manager)
  {
    this.factory = factory;
    if (factory.getJppfClient() == null) setJppfClient(factory.getJppfClient());
    this.manager = manager;
  }
	/**
	 * Get a connection through the application server.
	 * @return a <code>Connection</code> instance.
	 * @throws ResourceException if a connection could not be obtained.
	 * @see javax.resource.cci.ConnectionFactory#getConnection()
	 */
	public Connection getConnection() throws ResourceException
	{
		JPPFConnection conn = (JPPFConnection) manager.allocateConnection(factory, null);
		if (conn == null) return null;
		if (conn.getJppfClient() == null) conn.setJppfClient(getJppfClient());
		if (conn.isClosed()) conn.setAvailable();
		return conn;
	}

	/**
	 * This method does nothing.
	 * @param spec not used.
	 * @return nothing.
	 * @throws ResourceException this method always throws a NotSupportedException.
	 * @see javax.resource.cci.ConnectionFactory#getConnection(javax.resource.cci.ConnectionSpec)
	 */
	public Connection getConnection(ConnectionSpec spec) throws ResourceException
	{
		throw new NotSupportedException("getConnection(ConnectionSpec) is not supported");
	}

	/**
	 * Get the resource adapter's metadata.
	 * @return a <code>ResourceAdapterMetaData</code> instance.
	 * @throws ResourceException if the metadata could not be obtained.
	 * @see javax.resource.cci.ConnectionFactory#getMetaData()
	 */
	public ResourceAdapterMetaData getMetaData() throws ResourceException
	{
		return new JPPFResourceAdapterMetaData();
	}

	/**
	 * Get a record factory.
	 * @return a <code>RecordFactory</code> instance.
	 * @throws ResourceException if the record factory could not be obtained.
	 * @see javax.resource.cci.ConnectionFactory#getRecordFactory()
	 */
	public RecordFactory getRecordFactory() throws ResourceException
	{
		return new JPPFRecordFactory();
	}

	/**
	 * Set the naming reference4 for this connection factory.
	 * @param ref a <code>Referenceable</code> instance.
	 * @see javax.resource.Referenceable#setReference(javax.naming.Reference)
	 */
	public void setReference(Reference ref)
	{
		this.ref = ref;
	}

	/**
	 * Get the naming reference4 for this connection factory.
	 * @return ref a <code>Referenceable</code> instance.
	 * @throws NamingException if the reference could not be obtained.
	 * @see javax.naming.Referenceable#getReference()
	 */
	public Reference getReference() throws NamingException
	{
		return ref;
	}
}
