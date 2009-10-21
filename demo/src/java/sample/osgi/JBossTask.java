/*
 * JPPF.
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

package sample.osgi;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import org.jboss.system.server.*;
import org.jppf.JPPFException;
import org.jppf.server.protocol.JPPFTask;

/**
 * This task runs an instance of JBoss.
 * @author Laurent Cohen
 */
public class JBossTask extends JPPFTask
{
	/**
	 * Run the task.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			String path = "C:/Tools/jboss-4.2.2.GA";
			URL url = new File(path).toURI().toURL();
			Properties props = new Properties();
			props.setProperty("jboss.home.dir", path);
			props.setProperty("jboss.home.url", url.toString());
			props.setProperty("jboss.server.name", "default");
			props.setProperty("jboss.bind.address", "localhost");
			System.setProperty("jboss.bind.address", "localhost");

			Server server = new ServerImpl();
			server.init(props);
			server.start();
			synchronized(this)
			{
				wait();
			}
		}
		catch(Throwable e)
		{
			setException(new JPPFException(e));
			e.printStackTrace();
		}
	}
}
