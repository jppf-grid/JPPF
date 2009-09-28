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

package sample.osgi;

import java.io.*;

import org.jppf.server.protocol.CommandLineTask;

/**
 * This task runs an Eclipse instance embedded within th enode process.
 * @author Laurent Cohen
 */
public class EclipseTask2 extends CommandLineTask
{
	/**
	 * Run Eclipse as a separate process.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			String eclipse = "c:/Tools/eclipse";
			setStartDir(eclipse);
			//org.eclipse.equinox.launcher_1.0.100.v20080509-1800.jar
			File plugins = new File(eclipse + "/plugins");
			File[] launcherJars = plugins.listFiles(new FileFilter()
			{
				public boolean accept(File file)
				{
					String s = file.getName().toLowerCase();
					return s.startsWith("org.eclipse.equinox.launcher_") && s.endsWith(".jar");
				}
			});
			setCommandList("java", "-jar", launcherJars[0].getCanonicalPath(), "-Xmx256m", "-XX:MaxPermSize=256m");
			setCaptureOutput(true);
			launchProcess();
			StringBuilder sb = new StringBuilder();
			sb.append("System.out:\n").append(getStandardOutput()).append("\n");
			sb.append("System.err:\n").append(getErrorOutput()).append("\n");
			setResult(sb.toString());
		}
		catch(Exception e)
		{
			setException(e);
		}
	}
}
