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

package test.encryption;

import java.io.*;

import org.jppf.data.transform.JPPFDataTransform;
import org.jppf.example.dataencryption.SecureKeyCipherTransform;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;

import sample.datasize.DataTask;

/**
 * 
 * @author Laurent Cohen
 */
public class TestEncryption
{
	/**
	 * Entry point.
	 * @param args not used.
	 */
	public static void main(String[] args)
	{
		try
		{
			JPPFTask task = new DataTask(10);
			ObjectSerializer ser = new ObjectSerializerImpl();
			byte[] data = ser.serialize(task).buffer;
			System.out.println("data size = " + data.length);
			JPPFDataTransform transform = new SecureKeyCipherTransform();

			InputStream is = new ByteArrayInputStream(data);
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			transform.wrap(is, os);
			byte[] data2 = os.toByteArray();
			System.out.println("data2 size = " + data2.length);
			
			is = new ByteArrayInputStream(data2);
			os = new ByteArrayOutputStream();
			transform.unwrap(is, os);
			byte[] data3 = os.toByteArray();
			System.out.println("data3 size = " + data3.length);

			Object o = ser.deserialize(data3);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
