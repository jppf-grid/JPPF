/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2009 JPPF Team.
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

package sample.misc;

import org.jppf.node.NodeRunner;
import org.jppf.server.JPPFDriver;

/**
 * 
 * @author Laurent Cohen
 */
public class DriverAndNodeLauncher
{
	/**
	 * Entry point for this test application.
	 * @param args not used.
	 */
	public static void main(final String...args)
	{
		try
		{
			Runnable driver = new Runnable()
			{
				public void run()
				{
					JPPFDriver.main("noLauncher");
				}
			};
			Runnable node = new Runnable()
			{
				public void run()
				{
					NodeRunner.main("noLauncher");
				}
			};
			new Thread(driver).start();
			new Thread(node).start();
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
	}
}
