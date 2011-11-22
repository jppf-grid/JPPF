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

package org.jppf.process;

import java.net.Socket;

/**
 * Instances of this class isten to a socket connection setup in the ProcessLauncher, to handle the situation when the Launcher dies unexpectedly.<br>
 * In that situation, the connection is broken and this process knows that it must exit.
 * @author Laurent Cohen
 */
public class LauncherListener extends Thread
{
	/**
	 * The port on which to listen for the launcher signals.
	 */
	private int port = -1;

	/**
	 * Initialize this LauncherListener with the specified port.
	 * @param port - the port to listen to.
	 */
	public LauncherListener(int port)
	{
		super("LauncherListener thread");
		this.port = port;
	}

	/**
	 * Create a socket connection and listen to it, and exit this process when the conneciton is broken.
	 * @see java.lang.Thread#run()
	 */
	public void run()
	{
		try
		{
			Socket s = new Socket("localhost", port);
			s.getInputStream().read();
		}
		catch(Throwable t)
		{
			System.exit(0);
		}
	}
}
