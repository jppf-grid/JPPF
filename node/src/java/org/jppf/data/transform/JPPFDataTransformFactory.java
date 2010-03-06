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

package org.jppf.data.transform;

import org.apache.commons.logging.*;
import org.jppf.utils.JPPFConfiguration;

/**
 * Factory class for data transform.
 * The class of the actual JPPFDataTransform implementation is specified via the configuration property
 * "jppf.data.transform.class = <i>fully qualified class name</i>". If the class cannot be found, or none is specified,
 * then no transformation takes place.
 * @author Laurent Cohen
 */
public class JPPFDataTransformFactory
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFDataTransformFactory.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Create the singleton data transform instance.
	 * @return an instance of <code>JPPFDataTransform</code>.
	 */
	private static JPPFDataTransform createInstance()
	{
		JPPFDataTransform result = null;
		String s = JPPFConfiguration.getProperties().getString("jppf.data.transform.class", null);
		if (s != null)
		{
			try
			{
				Class clazz = Class.forName(s);
				result = (JPPFDataTransform) clazz.newInstance();
			}
			catch(Exception e)
			{
				log.error(e.getMessage(), e);
			}
		}
		return result;
	}

	/**
	 * Get an instance of the configured data transform. This method creates a new instance every time it is invoked.
	 * @return an instance of <code>JPPFDataTransform</code>.
	 */
	public static JPPFDataTransform getInstance()
	{
		return createInstance();
	}
}
