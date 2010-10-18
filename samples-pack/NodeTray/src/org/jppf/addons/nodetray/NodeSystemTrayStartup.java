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

package org.jppf.addons.nodetray;

import java.lang.reflect.Method;

import org.jppf.startup.JPPFNodeStartupSPI;

/**
 * This is the startup part of the node system tray add-on.<br>
 * When the <code>run()</code> method is called, this class verifies that the <code>SystemTray</code> is available
 * (using reflection)
 * @author Laurent Cohen
 */
public class NodeSystemTrayStartup implements JPPFNodeStartupSPI
{
	/**
	 * This is a test of a node startup class.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			Class<?> clazz = Class.forName("java.awt.SystemTray");
			Method m = clazz.getDeclaredMethod("isSupported");
			boolean supported = (Boolean) m.invoke(null);
			if (!supported) throw new Exception("System tray not supported");
			clazz = Class.forName("org.jppf.addons.nodetray.NodeSystemTray");
			clazz.newInstance();
		}
		catch(Throwable t)
		{
			System.out.println("system tray is not supported for this node, please ensure that you are running with a JDK 1.6 or later");
			t.printStackTrace();
		}
		//System.out.println("I'm a node startup class");
	}
}
