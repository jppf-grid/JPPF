/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.nio;

import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.configuration.ConfigurationHelper;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 * @exclude
 */
public class NioConstants
{
	/**
	 * Logger for this class.
	 */
	static Logger log = LoggerFactory.getLogger(NioConstants.class);
  /**
   * Size of the pool of threads for the state transition executor.
   * It is defined as the value of the configuration property
   * &quot;jppf.transition.thread.pool.size&quot;, with a default value of 1.
   */
  public static final int THREAD_POOL_SIZE = new ConfigurationHelper(JPPFConfiguration.getProperties()).getInt(
    "jppf.transition.thread.pool.size", "transition.thread.pool.size", Runtime.getRuntime().availableProcessors(), 1, 32 * 1024);
  /**
   * Name of the class server.
   */
  public static final String CLASS_SERVER = "ClassServer";
  /**
   * Name of the client class server.
   */
  public static final String CLIENT_CLASS_SERVER = "ClientClassServer";
  /**
   * Name of the node class server.
   */
  public static final String NODE_CLASS_SERVER = "NodeClassServer";
  /**
   * Name of the class server.
   */
  public static final String NODE_SERVER = "NodeJobServer";
  /**
   * Name of the client task server.
   */
  public static final String CLIENT_SERVER = "ClientJobServer";
  /**
   * Name of the acceptor server server.
   */
  public static final String ACCEPTOR = "Acceptor";
  /**
   * Default timeout for <code>Selector.select(long)</code> operations.
   */
  public static final long DEFAULT_SELECT_TIMEOUT = JPPFConfiguration.getProperties().getLong("jppf.nio.select.timeout", 1000L);
	/**
	 * Workaround for the issue described in <a href="http://www.jppf.org/forums/index.php/topic,1626.0.html">this forum thread</a>.
	 */
	public static final boolean CHECK_CONNECTION = getCheckConnection();
	
	/**
	 * Determine whether nio checks are enabled, and log accordingly.
	 * @return <code>true</code> if NIO checks are enabled, <code>false</code> otherwise.
	 */
	private static boolean getCheckConnection()
	{
		boolean b = JPPFConfiguration.getProperties().getBoolean("jppf.nio.check.connection", true);
		log.info("NIO checks are " + (b ? "enabled" : "disabled"));
		return b;
	}
}
