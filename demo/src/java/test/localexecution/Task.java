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
package test.localexecution;

import java.util.Map;

import org.jppf.server.protocol.JPPFTask;

import com.hazelcast.core.Hazelcast;

/**
 * Test task.
 * @author Laurent Cohen
 */
public class Task extends JPPFTask
{
	/**
	 * Explicit serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * {@inheritDoc}
	 */
	public void run()
	{
		try
		{
			//System.out.println("starting task");
			//JPPFClassLoader cl = (JPPFClassLoader) getClass().getClassLoader();
			/*
			ClassLoaderWrapper wrapper = new ClassLoaderWrapper(cl);
			System.out.println("downloading jar file");
			URL url = cl.getResource("../samples-pack/shared/lib/hazelcast-1.9.3.jar");
			System.out.println("got URL: " + url);
			wrapper.addURL(url);
			*/
			//Class c = cl.loadClass("com.hazelcast.core.Hazelcast");
			//System.out.println("found class " + c);
			Map map = Hazelcast.getMap("lolo");
			//setResult(c.getName());
			setResult("ok");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			setException(e);
		}
	}
}
