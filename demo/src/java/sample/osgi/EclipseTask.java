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

import java.lang.reflect.*;
import java.security.Permission;

import org.jppf.server.protocol.JPPFTask;

/**
 * This task runs an Eclipse instance embedded within th enode process.
 * @author Laurent Cohen
 */
public class EclipseTask extends JPPFTask
{
	/**
	 * ExitVM permission.
	 */
	private static String EXIT_VM = "exitVM";

	/**
	 * Initialize this task with a predefined length of time, in milliseconds, during which it will run.
	 */
	public EclipseTask()
	{
	}

	/**
	 * Perform the excution of this task.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			// we need ECLIPSE_HOME/plugins/org.eclipse.equinox.launcher_*.jar in the classpath
			String launcherClassName = "org.eclipse.equinox.launcher.Main";
			String[] args = { "-configuration file:/C:Tools/eclipse/configuration/config.ini", "-os win32", "-ws win32", "-arch x86", "-nl en_US" };
			Class clazz = Class.forName(launcherClassName);
			Method mainMethod = null;
			for (Method m: clazz.getDeclaredMethods())
			{
				if ("main".equals(m.getName()))
				{
					mainMethod = m;
					break;
				}
			}
			SecurityManager sm = new SecurityManager()
			{
				public void checkPermission(Permission permission)
				{
					if (EXIT_VM.equals(permission.getName())) throw new SecurityException("Permission denied: " + EXIT_VM);
				}
			};
			SecurityManager oldSm = System.getSecurityManager();
			try
			{
				System.setSecurityManager(sm);
				org.eclipse.equinox.launcher.Main.main(args);
				//mainMethod.invoke(null, new Object[] { args });
			}
			catch(Exception e)
			{
				if (e instanceof InvocationTargetException)
				{
					Throwable cause = e.getCause();
					if (!(cause instanceof SecurityException) || !cause.getMessage().contains(EXIT_VM)) throw e;
				}
				else throw e;
			}
			finally
			{
				System.setSecurityManager(oldSm);
			}
		}
		catch(Exception e)
		{
			setException(e);
		}
	}
}
