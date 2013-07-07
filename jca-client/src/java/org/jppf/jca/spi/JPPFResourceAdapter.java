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

import org.jppf.client.submission.SubmissionManager;
import org.jppf.jca.util.JPPFAccessorImpl;
import org.jppf.jca.work.JPPFJcaClient;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Implementation of the JPPF Resource Adapter for J2EE.
 * This class initiates a JPPF client with a pool of driver connections.
 * @author Laurent Cohen
 */
public class JPPFResourceAdapter extends JPPFAccessorImpl implements ResourceAdapter, Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFResourceAdapter.class);
  /**
   * A string holding the client configuration, specified as a property in the ra.xml descriptor.
   * @deprecated
   */
  private String clientConfiguration = "";
  /**
   * Defines how the configuration is to be located.<br>
   * This property is defined in the format "<i>type</i>|<i>path</i>", where <i>type</i> can be one of:<br>
   * <ul>
   * <li>"classpath": in this case <i>path</i> is a path to a properties file in one of the jars of the .rar file, for instance "resources/config/jppf.properties"</li>
   * <li>"url": <i>path</i> is a url that points to a properties file, for instance "file:///home/me/jppf/jppf.properties" (could be a http:// or ftp:// url as well)</li>
   * <li>"file": <i>path</i> is considered a path on the file system, for instance "/home/me/jppf/config/jppf.properties"</li>
   * </ul>
   */
  private String configurationSource = "";
  /**
   * The submission manager.
   */
  private transient SubmissionManager submissionManager;

  /**
   * Start this resource adapter with the specified bootstrap context.
   * This method is invoked by the application server exactly once for each resource adapter instance.
   * @param ctx bootstrap context provided by the application server.
   * @throws ResourceAdapterInternalException if an error occurred while starting this resource adapter.
   * @see javax.resource.spi.ResourceAdapter#start(javax.resource.spi.BootstrapContext)
   */
  @Override
  public void start(final BootstrapContext ctx) throws ResourceAdapterInternalException
  {
    try
    {
      log.info("Starting JPPF resource adapter");
      TypedProperties config = new JPPFConfigurationParser(getConfigurationSource(), getClientConfiguration()).parse();
      jppfClient = new JPPFJcaClient(new JPPFUuid().toString(), config);
      if (log.isDebugEnabled()) log.debug("Starting JPPF resource adapter: jppf client="+jppfClient);
      log.info("JPPF resource adapter started");
    }
    catch (Exception e)
    {
      throw new ResourceAdapterInternalException(e);
    }
  }

  /**
   * Called when a resource adapter instance is undeployed or during application server shutdown.
   * @see javax.resource.spi.ResourceAdapter#stop()
   */
  @Override
  public void stop()
  {
    if (jppfClient != null) jppfClient.close();
  }

  /**
   * Not supported.
   * @param arg0 not used.
   * @param arg1 not used.
   * @throws ResourceException always.
   * @see javax.resource.spi.ResourceAdapter#endpointActivation(javax.resource.spi.endpoint.MessageEndpointFactory, javax.resource.spi.ActivationSpec)
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
   * @see javax.resource.spi.ResourceAdapter#endpointDeactivation(javax.resource.spi.endpoint.MessageEndpointFactory, javax.resource.spi.ActivationSpec)
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
   * @see javax.resource.spi.ResourceAdapter#getXAResources(javax.resource.spi.ActivationSpec[])
   */
  @Override
  public XAResource[] getXAResources(final ActivationSpec[] arg0) throws ResourceException
  {
    return null;
  }

  /**
   * Get the string holding the client configuration.
   * @return the configuration as a string.
   * @deprecated
   */
  public String getClientConfiguration()
  {
    return clientConfiguration;
  }

  /**
   * Set the string holding the client configuration.
   * @param clientConfiguration the configuration as a string.
   * @deprecated
   */
  public void setClientConfiguration(final String clientConfiguration)
  {
    this.clientConfiguration = clientConfiguration;
  }

  /**
   * Get the property which defines how the configuration is to be located.
   * @return the property as a string.
   */
  public String getConfigurationSource()
  {
    return configurationSource;
  }

  /**
   * Set the property which defines how the configuration is to be located.
   * @param configurationSource the property as a string.
   */
  public void setConfigurationSource(final String configurationSource)
  {
    this.configurationSource = configurationSource;
  }
}
