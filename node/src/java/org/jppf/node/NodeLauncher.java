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
package org.jppf.node;

import org.jppf.process.ProcessLauncher;

/**
 * Bootstrap class for lauching a JPPF node. The node class is dynamically loaded from a remote server.
 * @author Laurent Cohen
 */
public class NodeLauncher
{
	/**
	 * Start this application, then the JPPF driver as a subprocess.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			ProcessLauncher launcher = new ProcessLauncher("org.jppf.node.NodeRunner");
			launcher.start();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		System.exit(0);
	}
}
