/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.startup;

import java.util.Iterator;

import org.jppf.utils.ServiceFinder;
import org.slf4j.*;

/**
 * Loader for the JPPF startup SPI implementations.
 * @param <S> the type of startup SPI.
 * @author Laurent Cohen
 */
public class JPPFStartupLoader<S extends JPPFStartup>
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(JPPFStartupLoader.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Load all instances found in the class path.
	 * @param clazz the type of startup classes to lookup and run. 
	 */
	public void load(Class<S> clazz)
	{
		Iterator<S> it = ServiceFinder.lookupProviders(clazz);
		while (it.hasNext())
		{
			try
			{
				S s = it.next();
				s.run();
			}
			catch(Error e)
			{
				log.error(e.getMessage(), e);
			}
		}
	}
}
