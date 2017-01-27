/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import java.util.List;

import javax.naming.*;
import javax.resource.ResourceException;
import javax.resource.cci.*;
import javax.resource.spi.ConnectionManager;

import org.jppf.client.*;
import org.jppf.jca.spi.*;

/**
 * Implementation of the {@link javax.resource.cci.ConnectionFactory ConnectionFactory} interface for
 * the JPPF resource adapter.
 * @author Laurent Cohen
 */
public class JPPFConnectionFactory implements ConnectionFactory {
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
  public JPPFConnectionFactory() {
  }

  /**
   * Initialize this connection factory with a specified managed connection factory and connection manager..
   * @param factory the managed factory to use.
   * @param manager the connection manager to use.
   * @exclude
   */
  public JPPFConnectionFactory(final JPPFManagedConnectionFactory factory, final ConnectionManager manager) {
    this.factory = factory;
    this.manager = manager;
  }

  @Override
  public Connection getConnection() throws ResourceException {
    JPPFConnection conn = (JPPFConnection) manager.allocateConnection(factory, null);
    if (conn == null) return null;
    return conn;
  }

  @Override
  public Connection getConnection(final ConnectionSpec spec) throws ResourceException {
    return getConnection();
  }

  @Override
  public ResourceAdapterMetaData getMetaData() throws ResourceException {
    return new JPPFResourceAdapterMetaData();
  }

  @Override
  public RecordFactory getRecordFactory() throws ResourceException {
    return new JPPFRecordFactory();
  }

  @Override
  public void setReference(final Reference ref) {
    this.ref = ref;
  }

  @Override
  public Reference getReference() throws NamingException {
    return ref;
  }

  /**
   * Determine whether there is a least one working connection to a remote JPPF driver.
   * @return {@code true} if there is a working connection, {@code false} otherwise.
   */
  public boolean isJPPFDriverAvailable() {
    if (factory == null) return false;
    AbstractGenericClient client = factory.retrieveJppfClient();
    if (client != null) {
      List<JPPFConnectionPool> list = client.findConnectionPools(JPPFClientConnectionStatus.ACTIVE, JPPFClientConnectionStatus.EXECUTING);
      return (list != null) && !list.isEmpty();
    }
    return false;
  }

  /**
   * Enable or disable local (in-JVM) execution of the jobs.
   * @param enabled {@code true} to enable local execution, {@code false} false ot disable it.
   */
  public void enableLocalExecution(final boolean enabled) {
    if (factory != null) {
      AbstractGenericClient client = factory.retrieveJppfClient();
      if (client != null) client.setLocalExecutionEnabled(enabled);
    }
  }
}
