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

import javax.resource.ResourceException;
import javax.resource.spi.*;

/**
 * Instances of this class represent a default connection manager for non-managed environments.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFConnectionManager implements ConnectionManager {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Get a connection from the pool.
   * @param mcf the managed connection factory that requested the connection.
   * @param cxRequestInfo the connection request information.
   * @return a <code>Connection</code> instance.
   * @throws ResourceException if the connection could not be obtained.
   * @see javax.resource.spi.ConnectionManager#allocateConnection(javax.resource.spi.ManagedConnectionFactory, javax.resource.spi.ConnectionRequestInfo)
   */
  @Override
  public Object allocateConnection(final ManagedConnectionFactory mcf, final ConnectionRequestInfo cxRequestInfo) throws ResourceException {
    final ManagedConnection conn = mcf.createManagedConnection(null, cxRequestInfo);
    return conn.getConnection(null, cxRequestInfo);
  }
}
