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

package org.jppf.io;

import java.io.*;

import org.jppf.comm.socket.SocketWrapper;
import org.jppf.data.transform.*;
import org.jppf.utils.*;
import org.jppf.utils.streams.*;
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
		if (fitsInMemory(size))
		{
			try
			{
				return new MultipleBuffersLocation(size);
			}
			catch (OutOfMemoryError oome)
			{
				if (debugEnabled) log.debug("OOM when allocating in-memory data location", oome);
			}
		}
		File file = createTempFile(size);
		return new FileDataLocation(file, size);
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
		if (traceEnabled) log.trace("read data size = " + n);
		DataLocation dl = createDataLocationMemorySensitive(n);
		dl.transferFrom(source, true);
		return dl;
	}

	/**
	 * Create a temporary file.
	 * @param size the file size (for logging purposes only).
	 * @return the created <code>File</code>.
	 * @throws Exception if an IO error occurs.
	 */
	public static File createTempFile(int size) throws Exception
	{
		File file = File.createTempFile("jppf", ".tmp");
		if (debugEnabled) log.debug("disk overflow: creating temp file '" + file.getCanonicalPath() + "' with size=" + size);
		file.deleteOnExit();
		return file;
	}

	/**
	 * Determines whether the data of the specified size would fit in memory.
	 * @param size the data size to check.
	 * @return true if the data would fit in memory, false otherwise.
	 */
	public static boolean fitsInMemory(int size)
	{
		long freeMem = SystemUtils.maxFreeHeap();
		if (traceEnabled) log.trace("free mem / requested size : " + freeMem + "/" + size);
		return (long) (FREE_MEM_TO_SIZE_RATIO * size) < freeMem;
	}

	/**
	 * Deserialize the next object available via a network connection.
	 * @param socketWrapper the network connection used to read data.
	 * @param ser the object serializer to use.
	 * @return the transformed result as an object.
	 * @throws Exception if an error occurs while preparing the data.
	 */
	public static Object unwrappedData(SocketWrapper socketWrapper, ObjectSerializer ser) throws Exception
	{
		if (traceEnabled) log.trace("unwrapping from network connection");
		InputSource sis = new SocketWrapperInputSource(socketWrapper);
		DataLocation dl = IOHelper.readData(sis);
		return unwrappedData(dl, ser);
	}

	/**
	 * Deserialize the specified data into an object.
	 * @param dl the data, stored in a memory-aware location.
	 * @param ser the object serializer to use.
	 * @return the transformed result as an object.
	 * @throws Exception if an error occurs while preparing the data.
	 */
	public static Object unwrappedData(DataLocation dl, ObjectSerializer ser) throws Exception
	{
		if (traceEnabled) log.trace("unwrapping " + dl);
		JPPFDataTransform transform = JPPFDataTransformFactory.getInstance();
		InputStream is = null;
		if (transform != null)
		{
			is = fitsInMemory(dl.getSize()) ? unwrapData(transform, dl) : unwrapDataToFile(transform, dl);
		}
		else is = dl.getInputStream();
		return ser.deserialize(is);
	}

	/**
	 * Apply a {@link JPPFDataTransform} to the specified source and store the results in memory. 
	 * @param transform the {@link JPPFDataTransform} to apply.
	 * @param source the source data to transform.
	 * @return the transformed data as an <code>InputStream</code>.
	 * @throws Exception if an error occurs while preparing the data.
	 */
	public static InputStream unwrapData(JPPFDataTransform transform, DataLocation source) throws Exception
	{
		if (traceEnabled) log.trace("unwrapping to memory " + source);
		MultipleBuffersOutputStream mbos = new MultipleBuffersOutputStream();
		transform.unwrap(source.getInputStream(), mbos);
		return new MultipleBuffersInputStream(mbos.toBufferList());
	}

	/**
	 * Apply a {@link JPPFDataTransform} to the specified source and store the results in a temporary file. 
	 * @param transform the {@link JPPFDataTransform} to apply.
	 * @param source the source data to transform.
	 * @return the transformed data as a <code>File</code>.
	 * @throws Exception if an error occurs while preparing the data.
	 */
	public static InputStream unwrapDataToFile(JPPFDataTransform transform, DataLocation source) throws Exception
	{
		if (traceEnabled) log.trace("unwrapping to file " + source);
		File file = IOHelper.createTempFile(-1);
		OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
		transform.unwrap(source.getInputStream(), os);
		return new BufferedInputStream(new FileInputStream(file));
	}

	/**
	 * Serialize an object and send it to the server.
	 * @param socketWrapper the socket client used to send data to the server.
	 * @param o the object to serialize.
	 * @param ser the object serializer.
	 * @throws Exception if any error occurs.
	 */
	public static void sendData(SocketWrapper socketWrapper, Object o, ObjectSerializer ser) throws Exception
	{
		DataLocation dl = null;
		if (traceEnabled) log.trace("sending object " + o);
		try
		{
			dl = serializeDataToMemory(o, ser);
		}
		catch(OutOfMemoryError e)
		{
			dl = serializeDataToFile(o, ser);
		}
		socketWrapper.writeInt(dl.getSize());
		OutputDestination od = new SocketWrapperOutputDestination(socketWrapper);
		dl.transferTo(od, true);
	}

	/**
	 * Serialize an object and send it to the server.
	 * @param o the object to serialize.
	 * @param ser the object serializer.
	 * @return a {@link DataLocation} instance.
	 * @throws Exception if any error occurs.
	 */
	public static DataLocation serializeData(Object o, ObjectSerializer ser) throws Exception
	{
		if (traceEnabled) log.trace("serializing object " + o);
		DataLocation dl = null;
		try
		{
			dl = serializeDataToMemory(o, ser);
		}
		catch(OutOfMemoryError e)
		{
			dl = serializeDataToFile(o, ser);
		}
		return dl;
	}

	/**
	 * Serialize an object to a bugffer in memory.
	 * @param o the object to serialize.
	 * @param ser the object serializer.
	 * @return an instance of {@link MultipleBuffersOutputStream}.
	 * @throws Exception if any error occurs.
	 */
	public static DataLocation serializeDataToMemory(Object o, ObjectSerializer ser) throws Exception
	{
		if (traceEnabled) log.trace("serializing object to memory " + o);
		JPPFDataTransform transform = JPPFDataTransformFactory.getInstance();
		MultipleBuffersOutputStream mbos = new MultipleBuffersOutputStream();
		ser.serialize(o, mbos);
		if (transform != null)
		{
			MultipleBuffersInputStream mbis = new MultipleBuffersInputStream(mbos.toBufferList());
			mbos = new MultipleBuffersOutputStream();
			transform.wrap(mbis, mbos);
		}
		return new MultipleBuffersLocation(mbos.toBufferList(), mbos.size());
	}

	/**
	 * Serialize an object and send it to the server.
	 * @param o the object to serialize.
	 * @param ser the object serializer.
	 * @return an instance of {@link FileDataLocation}.
	 * @throws Exception if any error occurs.
	 */
	public static DataLocation serializeDataToFile(Object o, ObjectSerializer ser) throws Exception
	{
		if (traceEnabled) log.trace("serializing object to file " + o);
		File file = IOHelper.createTempFile(-1);
		OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
		ser.serialize(o, os);
		DataLocation dl = null;
		JPPFDataTransform transform = JPPFDataTransformFactory.getInstance();
		if (transform != null)
		{
			InputStream is = new BufferedInputStream(new FileInputStream(file));
			File file2 = IOHelper.createTempFile(-1);
			os = new BufferedOutputStream(new FileOutputStream(file2));
			transform.wrap(is, os);
			dl = new FileDataLocation(file2);
		}
		else dl = new FileDataLocation(file);
		return dl;
	}
}
