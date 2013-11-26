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

import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.*;
import javax.security.auth.Subject;

import org.jppf.client.*;
import org.jppf.client.submission.SubmissionManager;
import org.jppf.jca.cci.*;
import org.jppf.jca.util.JPPFAccessorImpl;
import org.jppf.jca.work.JcaSubmissionManager;
import org.jppf.utils.TypedProperties;
import org.slf4j.*;


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
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFManagedConnectionFactory.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Handle to the resource adapter.
   */
  private transient ResourceAdapter resourceAdapter = null;
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
   * Default constructor.
   */
  public JPPFManagedConnectionFactory()
  {
    if (debugEnabled) log.debug("instatiating JPPFManagedConnectionFactory");
  }

  @Override
  public Object createConnectionFactory() throws ResourceException
  {
    if (debugEnabled) log.debug("creating connection factory with default connection manager");
    return createConnectionFactory(new JPPFConnectionManager());
  }

  @Override
  public Object createConnectionFactory(final ConnectionManager manager) throws ResourceException
  {
    if (debugEnabled) log.debug("creating connection factory with connection manager");
    JPPFConnectionFactory jcf = new JPPFConnectionFactory(this, manager);
    if (jcf.retrieveJppfClient() == null) jcf.assignJppfClient(jppfClient);
    return jcf;
  }

  @Override
  public ManagedConnection createManagedConnection(final Subject subject, final ConnectionRequestInfo cri) throws ResourceException
  {
    if (debugEnabled) log.debug("creating managed connection");
    JPPFManagedConnection conn = new JPPFManagedConnection(this);
    if (conn.retrieveJppfClient() == null) conn.assignJppfClient(jppfClient);
    return conn;
  }

  @Override
  public ManagedConnection matchManagedConnections(final Set set, final Subject subject, final ConnectionRequestInfo cri)
  throws ResourceException
  {
    if (!set.isEmpty()) return (ManagedConnection) set.iterator().next();
    return null;
  }

  @Override
  public ResourceAdapter getResourceAdapter()
  {
    return resourceAdapter;
  }

  @Override
  public void setResourceAdapter(final ResourceAdapter resourceAdapter) throws ResourceException
  {
    this.resourceAdapter = resourceAdapter;
  }

  @Override
  public boolean equals(final Object obj)
  {
    return super.equals(obj);
  }

  @Override
  public int hashCode()
  {
    return super.hashCode();
  }

  @Override
  public AbstractGenericClient retrieveJppfClient()
  {
    return jppfClient;
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
    log.info("Starting JPPF managed connection factory");
    TypedProperties config = new JPPFConfigurationParser(getConfigurationSource()).parse();
    jppfClient = new JPPFClient(null, config) {
      @Override
      protected SubmissionManager createSubmissionManager() {
        SubmissionManager submissionManager = null;
        try {
          submissionManager = new JcaSubmissionManager(this);
        } catch (Exception e) {
          log.error("Can't initialize Submission Manager", e);
        }
        return submissionManager;
      }

      @Override
      protected String getSerializationHelperClassName(){
        return AbstractJPPFClient.JCA_SERIALIZATION_HELPER;
      }
    };
    if (log.isDebugEnabled()) log.debug("Starting JPPF resource adapter: jppf client=" + jppfClient);
    log.info("JPPF connection factory started");
  }

  /**
   * Reset the JPPF client.
   */
  void resetClient()
  {
    if (jppfClient == null) return;
    TypedProperties config = new JPPFConfigurationParser(getConfigurationSource()).parse();
    ((JPPFClient) jppfClient).reset(config);
  }
}
