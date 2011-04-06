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

package test.serialization;

import java.io.*;

import org.jppf.JPPFException;
import org.jppf.client.JPPFJob;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.streams.serialization.*;

import sample.dist.tasklength.LongTask;

/**
 * 
 * @author Laurent Cohen
 */
public class Test
{
	/**
	 * Main entry point.
	 * @param args not used.
	 */
	public static void main(String[] args)
	{
		try
		{
			test();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Perform a test.
	 * @throws Exception if any error occurs.
	 */
	public static void test() throws Exception
	{
		JPPFJob job = new JPPFJob();
		job.setId("my job id");
		JPPFTask task = new LongTask(1000);
		task.setId("someID");
		task.setResult("result");
		task.setException(new JPPFException("exception"));
		job.addTask(task);
		Test test = new Test();
		byte[] data = test.serialize(task);
		JPPFTask task2 = (JPPFTask) test.deserialize(data);
		print("the end");
	}

	/**
	 * Print a string.
	 * @param s the string to print.
	 */
	private static void print(String s)
	{
		System.out.println(s);
	}

	/**
	 * Serialize an object.
	 * @param o the object to serialize.
	 * @return the qseuialized object as a byte array.
	 * @throws Exception if any error occurs.
	 */
	public byte[] serialize(Object o) throws Exception
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JPPFObjectOutputStream joos = new JPPFObjectOutputStream(baos);
		joos.writeObject(o);
		return baos.toByteArray();
	}

	/**
	 * Desrialize an object.
	 * @param data the byte array to deserialize from.
	 * @return an Object.
	 * @throws Exception if any error occurs.
	 */
	public Object deserialize(byte[] data) throws Exception
	{
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		JPPFObjectInputStream jois = new JPPFObjectInputStream(bais);
		return jois.readObject();
	}
}
