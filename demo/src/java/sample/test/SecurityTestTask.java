/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package sample.test;

import java.io.*;
import java.net.Socket;
import org.jppf.utils.JPPFConfiguration;

/**
 * Thnis task is intended for testing the framework only.
 * @author Laurent Cohen
 */
public class SecurityTestTask extends JPPFTestTask
{
	/**
	 * Initialize this task.
	 */
	public SecurityTestTask()
	{
	}
	
	/**
	 * Try exiting the JVM through a <code>System.exit(int)</code> call.
	 * @throws SecurityException if the security manager prevents from exiting the JVM.
	 */
	public void testExitVM() throws SecurityException
	{
		System.exit(0);
	}

	/**
	 * Try connecting to a non authorized host through a TCP/IP socket.
	 * @throws SecurityException if the security manager prevents from connecting to the host.
	 */
	public void testConnectForbiddenHost() throws SecurityException
	{
		try
		{
			Socket s = new Socket("www.jppf.org", 8000);
			s.close();
		}
		catch(IOException e)
		{
		}
	}

	/**
	 * Try connecting to a non authorized port on the JPPF server.
	 * @throws SecurityException if the security manager prevents from connecting on the specified port.
	 */
	public void testConnectForbiddenPort() throws SecurityException
	{
		try
		{
			String host = JPPFConfiguration.getProperties().getString("jppf.server.host", "localhost");
			Socket s = new Socket(host, 1001);
			s.close();
		}
		catch(IOException e)
		{
		}
	}

	/**
	 * Try writing a dummy file.
	 * @throws SecurityException if the security manager prevents from writing the file.
	 */
	public void testWriteFile() throws SecurityException
	{
		try
		{
			FileWriter writer = new FileWriter("foo.bar");
			writer.write("Hello");
			writer.close();
		}
		catch(IOException e)
		{
		}
	}

	/**
	 * Try reading a non-authorized file.
	 * @throws SecurityException if the security manager prevents from writing the file.
	 */
	public void testReadFile() throws SecurityException
	{
		try
		{
			File file = new File("/");
			File[] dirList = file.listFiles();
			for (File f: dirList)
			{
				if (!f.isDirectory())
				{
					FileInputStream fis = new FileInputStream(f);
					fis.read();
					fis.close();
					break;
				}
			}
		}
		catch(IOException e)
		{
		}
	}
}
