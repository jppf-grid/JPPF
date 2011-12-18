/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.server.nio;

import org.jppf.utils.JPPFConfiguration;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class NioChecks
{
	/**
	 * Logger for this class.
	 */
	static Logger log = LoggerFactory.getLogger(NioChecks.class);

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
