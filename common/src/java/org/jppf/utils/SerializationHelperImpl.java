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
package org.jppf.utils;

import org.jppf.classloader.*;
import org.slf4j.*;

/**
 * Collection of utility methods for serializing and deserializing to and from bytes buffers.
 * @author Laurent Cohen
 */
public class SerializationHelperImpl implements SerializationHelper
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(SerializationHelperImpl.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Determines whether dumping byte arrays in the log is enabled.
	 */
	private boolean dumpEnabled = JPPFConfiguration.getProperties().getBoolean("byte.array.dump.enabled", false);

	/**
	 * Used to serialize and deserialize objects to and from object streams.
	 */
	protected ObjectSerializer serializer = null;

	/**
	 * Default constructor.
	 */
	public SerializationHelperImpl()
	{
	}

	/**
	 * Get the object serializer for this helper.
	 * @return an <code>ObjectSerializer</code> instance.
	 * @throws Exception if the object serializer could not be instantiated.
	 * @see org.jppf.utils.SerializationHelper#getSerializer()
	 */
	public ObjectSerializer getSerializer() throws Exception
	{
		if (serializer == null)
		{
			ClassLoader cl = getClass().getClassLoader();
			if (cl instanceof AbstractJPPFClassLoader)
			{
				serializer = (ObjectSerializer)
					((AbstractJPPFClassLoader) cl).loadJPPFClass("org.jppf.utils.ObjectSerializerImpl").newInstance();
			}
			else if (cl instanceof NonDelegatingClassLoader)
			{
				serializer = (ObjectSerializer)
					((NonDelegatingClassLoader) cl).loadClassDirect("org.jppf.utils.ObjectSerializerImpl").newInstance();
			}
			else
			{
				serializer = (ObjectSerializer)
					cl.loadClass("org.jppf.utils.ObjectSerializerImpl").newInstance();
			}
		}
		return serializer;
	}
}
