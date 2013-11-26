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

import javax.resource.ResourceException;
import javax.resource.cci.*;

/**
 * Implementation of the Interaction interface.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFInteraction implements Interaction
{
  /**
   * The connection associated with this interaction
   */
  private JPPFConnectionImpl conn;

  /**
   * Initialize this interaction with a specified connection.
   * @param conn a <code>Connection</code> instance.
   */
  public JPPFInteraction(final JPPFConnectionImpl conn)
  {
    this.conn = conn;
  }

  @Override
  public void clearWarnings()
  {
  }

  @Override
  public void close()
  {
  }

  /**
   * This method does nothing.
   * @param ispec not used.
   * @param input not used.
   * @return <code>null</code>.
   * @throws ResourceException .
   */
  @Override
  public Record execute(final InteractionSpec ispec, final Record input) throws ResourceException
  {
    return null;
  }

  /**
   * This method does nothing.
   * @param ispec not used.
   * @param input not used.
   * @param output not used.
   * @return <code>false</code>.
   * @throws ResourceException .
   */
  @Override
  public boolean execute(final InteractionSpec ispec, final Record input, final Record output) throws ResourceException
  {
    return false;
  }

  @Override
  public Connection getConnection()
  {
    return conn;
  }

  /**
   * This method does nothing.
   * @return <code>null</code>;
   */
  @Override
  public ResourceWarning getWarnings()
  {
    return null;
  }
}
