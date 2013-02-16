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

package org.jppf.jca.spi;

import java.util.*;

import javax.resource.*;
import javax.resource.spi.*;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import org.jppf.jca.cci.*;
import org.jppf.jca.util.JPPFAccessorImpl;


/**
 * Implementation of the ManagedConnection interface.
 * @author Laurent Cohen
 */
public class JPPFManagedConnection extends JPPFAccessorImpl implements ManagedConnection
{
  /**
   * This managed connection's associated connection handle.
   */
  private JPPFConnectionImpl connection = null;
  /**
   * List of connection event listeners for this managed connection.
   */
  private List<ConnectionEventListener> listeners = new ArrayList<ConnectionEventListener>();

  /**
   * Default constructor.
   */
  public JPPFManagedConnection()
  {
    //System.out.println("creating managed connection, call stack:");
    //System.out.println(StringUtils.getStackTrace(new Exception()));
  }

  /**
   * Add a listener to the list of connection event listeners for this managed connection.
   * @param listener the listener to add.
   * @see javax.resource.spi.ManagedConnection#addConnectionEventListener(javax.resource.spi.ConnectionEventListener)
   */
  @Override
  public void addConnectionEventListener(final ConnectionEventListener listener)
  {
    listeners.add(listener);
  }

  /**
   * Remove a listener from the list of connection event listeners for this managed connection.
   * @param listener the listener to remove.
   * @see javax.resource.spi.ManagedConnection#removeConnectionEventListener(javax.resource.spi.ConnectionEventListener)
   */
  @Override
  public void removeConnectionEventListener(final ConnectionEventListener listener)
  {
    listeners.remove(listener);
  }

  /**
   * Fire a connection event with the specified parameters.
   * @param connection the connection from which the event originates.
   * @param eventId the type of event.
   * @param exception an eventual exception that should be part of the event. May be null.
   */
  public void fireConnectionEvent(final JPPFConnection connection, final int eventId, final Exception exception)
  {
    ConnectionEvent event;
    if (exception == null) event = new ConnectionEvent(this, eventId);
    else event = new ConnectionEvent(this, eventId, exception);
    event.setConnectionHandle(connection);
    for (ConnectionEventListener listener: listeners)
    {
      switch(eventId)
      {
        case ConnectionEvent.CONNECTION_CLOSED:
          listener.connectionClosed(event);
          break;

        case ConnectionEvent.CONNECTION_ERROR_OCCURRED:
          listener.connectionErrorOccurred(event);
          break;

        case ConnectionEvent.LOCAL_TRANSACTION_COMMITTED:
          listener.localTransactionCommitted(event);
          break;

        case ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK:
          listener.localTransactionRolledback(event);
          break;

        case ConnectionEvent.LOCAL_TRANSACTION_STARTED:
          listener.localTransactionStarted(event);
          break;
      }
    }
  }

  /**
   * Used by the container to change the association of an application-level connection handle
   * with a ManagedConnection instance.
   * @param conn the new connection to associate.
   * @throws ResourceException if the association raised an error.
   * @see javax.resource.spi.ManagedConnection#associateConnection(java.lang.Object)
   */
  @Override
  public void associateConnection(final Object conn) throws ResourceException
  {
    connection = (JPPFConnectionImpl) conn;
    connection.setManagedConnection(this);
  }

  /**
   * This method does nothing.
   * @throws ResourceException .
   * @see javax.resource.spi.ManagedConnection#cleanup()
   */
  @Override
  public void cleanup() throws ResourceException
  {
  }

  /**
   * This method does nothing.
   * @throws ResourceException .
   * @see javax.resource.spi.ManagedConnection#destroy()
   */
  @Override
  public void destroy() throws ResourceException
  {
  }

  /**
   * Get a connection.
   * @param subject not used.
   * @param cri not used.
   * @return a <code>JPPFConnection</code> instance.
   * @throws ResourceException if the connection could not be obtained.
   * @see javax.resource.spi.ManagedConnection#getConnection(javax.security.auth.Subject, javax.resource.spi.ConnectionRequestInfo)
   */
  @Override
  public Object getConnection(final Subject subject, final ConnectionRequestInfo cri) throws ResourceException
  {
    if (connection == null)
    {
      connection = new JPPFConnectionImpl(this);
      if (connection.getJppfClient() == null) connection.setJppfClient(getJppfClient());
    }
    if (connection.isClosed()) connection.setAvailable();
    return connection;
  }

  /**
   * Not supported.
   * @return nothing.
   * @throws ResourceException always.
   * @see javax.resource.spi.ManagedConnection#getLocalTransaction()
   */
  @Override
  public LocalTransaction getLocalTransaction() throws ResourceException
  {
    throw new NotSupportedException("Method not supported");
  }

  /**
   * Get this managed connection's metadata.
   * @return a <code>JPPFManagedConnectionMetaData</code> instance.
   * @throws ResourceException if the metadata could not be obtained.
   * @see javax.resource.spi.ManagedConnection#getMetaData()
   */
  @Override
  public ManagedConnectionMetaData getMetaData() throws ResourceException
  {
    return new JPPFManagedConnectionMetaData(null);
  }

  /**
   * Not supported.
   * @return nothing.
   * @throws ResourceException always.
   * @see javax.resource.spi.ManagedConnection#getXAResource()
   */
  @Override
  public XAResource getXAResource() throws ResourceException
  {
    throw new NotSupportedException("Method not supported");
  }

  /**
   * Determine whether this managed connection is available and can be reused by the application server.
   * @return true if this managed connection is available, false otherwise.
   */
  public boolean isAvailable()
  {
    synchronized(this)
    {
      return (connection != null) && connection.isClosed();
    }
  }
}
