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

package org.jppf.io;

import java.io.File;

import org.jppf.utils.*;
import org.slf4j.*;


/**
 * Collection of utility methods to create and manipulate IO objects.
 * @author Laurent Cohen
 */
public final class IOHelper
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(IOHelper.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Determines whether trace-level logging is enabled.
	 */
	private static boolean traceEnabled = log.isTraceEnabled();
	/**
	 * Size of temporary buffers used in I/O transfers.
	 */
	public static final int TEMP_BUFFER_SIZE = 32 * 1024;
	/**
	 * Free memory / requested allocation size ration threshold that triggers disk overflow.
	 */
	private static final double FREE_MEM_TO_SIZE_RATIO = JPPFConfiguration.getProperties().getDouble("jppf.disk.overflow.threshold", 2.0d);

	/**
	 * Instantiation of this class is not permitted.
	 */
	private IOHelper()
	{
	}

	/**
	 * Create a data location object based on a comparison of the available heap memory
	 * and the data location object size.
	 * @param size the requested size of the data location to create.
	 * @return a <code>DataLocation</code> object whose content may be stored in memory
	 * or on another medium, depending on the available memory.
	 * @throws Exception if an IO error occurs.
	 */
	public static DataLocation createDataLocationMemorySensitive(int size) throws Exception
	{
		long freeMem = SystemUtils.maxFreeHeap();
		if (traceEnabled) log.trace("free mem / requested size : " + freeMem + "/" + size);
		if ((long) (FREE_MEM_TO_SIZE_RATIO * size) < freeMem)
		{
			try
			{
				byte[] bytes = new byte[size];
				return new ByteBufferLocation(bytes, 0, size);
			}
			catch (OutOfMemoryError oome)
			{
				if (debugEnabled) log.debug("OOM when allocating in-memory data location", oome);
			}
		}
		File file = File.createTempFile("jppf", ".tmp");
		if (debugEnabled) log.debug("disk overflow: creating temp file '" + file.getCanonicalPath() + "' with size=" + size);
		file.deleteOnExit();
		return new FileLocation(file, size);
	}

	/**
	 * Read a provider or task data from an input source.
	 * The data may be stored in memory or on another medium depending on its size and the available memory.
	 * @param source the input source from which to read the data.
	 * @return A data location containing the data provider or task data.
	 * @throws Exception if an error occurs while deserializing.
	 */
	public static DataLocation readData(InputSource source) throws Exception
	{
		int n = source.readInt();
		DataLocation dl = createDataLocationMemorySensitive(n);
		dl.transferFrom(source, true);
		return dl;
	}
}
