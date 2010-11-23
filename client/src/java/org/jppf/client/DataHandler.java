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

package org.jppf.client;

import java.io.*;
import java.util.List;

import org.jppf.comm.socket.SocketWrapper;
import org.jppf.data.transform.*;
import org.jppf.io.*;
import org.jppf.utils.*;

/**
 * Handle data serialization and deserialization to and from the server.
 * @author Laurent Cohen
 */
public class DataHandler
{
	/**
	 * Serialize an object and send it to the server.
	 * @param socketWrapper the socket client used to send data to the server.
	 * @param o the object to serialize.
	 * @param ser the object serializer.
	 * @throws Exception if any error occurs.
	 */
	void sendData(SocketWrapper socketWrapper, Object o, ObjectSerializer ser) throws Exception
	{
		try
		{
			serializeData(socketWrapper, o, ser);
		}
		catch(OutOfMemoryError e)
		{
			serializeDataToFile(socketWrapper, o, ser);
		}
	}

	/**
	 * Serialize an object and send it to the server.
	 * @param socketWrapper the socket client used to send data to the server.
	 * @param o the object to serialize.
	 * @param ser the object serializer.
	 * @throws Exception if any error occurs.
	 */
	void serializeData(SocketWrapper socketWrapper, Object o, ObjectSerializer ser) throws Exception
	{
		List<JPPFBuffer> list = null;
		JPPFDataTransform transform = JPPFDataTransformFactory.getInstance();
		MultipleBuffersOutputStream mbos = new MultipleBuffersOutputStream();
		ser.serialize(o, mbos);
		int size = mbos.size();
		if (transform != null)
		{
			MultipleBuffersInputStream mbis = new MultipleBuffersInputStream(mbos.toBufferList());
			mbos = new MultipleBuffersOutputStream();
			transform.wrap(mbis, mbos);
			list = mbos.toBufferList();
			size = mbos.size();
		}
		else list = mbos.toBufferList();
		socketWrapper.writeInt(size);
		for (JPPFBuffer buf: list) socketWrapper.write(buf.buffer, 0, buf.length);
	}

	/**
	 * Serialize an object and send it to the server.
	 * @param socketWrapper the socket client used to send data to the server.
	 * @param o the object to serialize.
	 * @param ser the object serializer.
	 * @throws Exception if any error occurs.
	 */
	private void serializeDataToFile(SocketWrapper socketWrapper, Object o, ObjectSerializer ser) throws Exception
	{
		File file = IOHelper.createTempFile(-1);
		OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
		ser.serialize(o, os);
		int size = (int) file.length();
		DataLocation dl = null;
		JPPFDataTransform transform = JPPFDataTransformFactory.getInstance();
		if (transform != null)
		{
			InputStream is = new BufferedInputStream(new FileInputStream(file));
			File file2 = IOHelper.createTempFile(-1);
			os = new BufferedOutputStream(new FileOutputStream(file2));
			transform.wrap(is, os);
			size = (int) file2.length();
			dl = new FileLocation(file2, size);
		}
		else dl = new FileLocation(file, size);
		socketWrapper.writeInt(size);
		OutputDestination od = new SocketWrapperOutputDestination(socketWrapper);
		dl.transferTo(od, true);
	}
}
