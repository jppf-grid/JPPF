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

import java.io.Serializable;

import javax.resource.*;
import javax.resource.spi.*;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import org.slf4j.*;

/**
 * Implementation of the JPPF Resource Adapter for J2EE.
 * This class initiates a JPPF client with a pool of driver connections.
 * @author Laurent Cohen
 */
public class JPPFResourceAdapter implements ResourceAdapter, Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFResourceAdapter.class);

  @Override
  public void start(final BootstrapContext ctx) throws ResourceAdapterInternalException
  {
    log.info("Starting JPPF resource adapter");
  }

  @Override
  public void stop()
  {
    log.info("Stopping JPPF resource adapter");
  }

  /**
   * Not supported.
   * @param arg0 not used.
   * @param arg1 not used.
   * @throws ResourceException always.
   */
  @Override
  public void endpointActivation(final MessageEndpointFactory arg0, final ActivationSpec arg1) throws ResourceException
  {
    throw new NotSupportedException("Method not supported");
  }

  /**
   * This method does nothing.
   * @param arg0 not used.
   * @param arg1 not used.
   */
  @Override
  public void endpointDeactivation(final MessageEndpointFactory arg0, final ActivationSpec arg1)
  {
  }

  /**
   * This method does nothing.
   * @param arg0 not used.
   * @return null
   * @throws ResourceException .
   */
  @Override
  public XAResource[] getXAResources(final ActivationSpec[] arg0) throws ResourceException
  {
    return null;
  }
}
