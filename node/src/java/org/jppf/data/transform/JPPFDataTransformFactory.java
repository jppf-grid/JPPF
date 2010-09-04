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

import java.io.*;

import org.jppf.utils.*;
import org.slf4j.*;

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
	private static Logger log = LoggerFactory.getLogger(JPPFDataTransformFactory.class);
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

	/**
	 * Transform the specified data using the specified data transformation. 
	 * @param transform the data transformation to use.
	 * @param normal true to wrap the data, false to unwrap it.
	 * @param data the data to transform.
	 * @param offset the position to start a in the data.
	 * @param len the number of bytes to process, starting at the offset, in the data.
	 * @return the result of the transformation as an array of bytes.
	 * @throws Exception if any error occurs while transforming the data.
	 */
	public static byte[] transform(JPPFDataTransform transform, boolean normal, byte[] data, int offset, int len) throws Exception
	{
		InputStream is = new ByteArrayInputStream(data, offset, len);
		MultipleBuffersOutputStream mbos = new MultipleBuffersOutputStream();
		if (normal) transform.wrap(is, mbos);
		else transform.unwrap(is, mbos);
		return mbos.toByteArray();
	}

	/**
	 * Transform the specified data using the specified data transformation. 
	 * @param transform the data transformation to use.
	 * @param normal true to wrap the data, false to unwrap it.
	 * @param data the data to transform.
	 * @return the result of the transformation as an array of bytes.
	 * @throws Exception if any error occurs while transforming the data.
	 */
	public static byte[] transform(JPPFDataTransform transform, boolean normal, byte[] data) throws Exception
	{
		return transform(transform, normal, data, 0, data.length);
	}

	/**
	 * Transform the specified data using a new data transformation instance.
	 * @param normal true to wrap the data, false to unwrap it.
	 * @param data the data to transform.
	 * @param offset the position to start a in the data.
	 * @param len the number of bytes to process, starting at the offset, in the data.
	 * @return the result of the transformation as an array of bytes, or the original data if no data transform is configured.
	 * @throws Exception if any error occurs while transforming the data.
	 * @throws Exception if any error occurs while tranforming the data.
	 */
	public static byte[] transform(boolean normal, byte[] data, int offset, int len) throws Exception
	{
		JPPFDataTransform dataTransform = createInstance();
		return dataTransform == null ? data : transform(dataTransform, normal, data, offset, len);
	}

	/**
	 * Transform the specified data using a new data transformation instance.
	 * @param normal true to wrap the data, false to unwrap it.
	 * @param data the data to transform.
	 * @return the result of the transformation as an array of bytes, or the original data if no data transform is configured.
	 * @throws Exception if any error occurs while transforming the data.
	 */
	public static byte[] transform(boolean normal, byte[] data) throws Exception
	{
		JPPFDataTransform dataTransform = createInstance();
		return dataTransform == null ? data : transform(dataTransform, normal, data, 0, data.length);
	}

	/**
	 * Transform the specified data using a new data transformation instance.
	 * @param normal true to wrap the data, false to unwrap it.
	 * @param is the data to transform.
	 * @return the result of the transformation as an array of bytes, or the original data if no data transform is configured.
	 * @throws Exception if any error occurs while transforming the data.
	 */
	public static byte[] transform(boolean normal, InputStream is) throws Exception
	{
		JPPFDataTransform dataTransform = createInstance();
		if (dataTransform == null) return FileUtils.getInputStreamAsByte(is);
		MultipleBuffersOutputStream mbos = new MultipleBuffersOutputStream();
		if (normal) dataTransform.wrap(is, mbos);
		else dataTransform.unwrap(is, mbos);
		return mbos.toByteArray();
	}
}
