/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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
package org.jppf.process;

import java.io.*;

/**
 * 
 * @author Laurent Cohen
 */
public class ProcessWrapper
{
	/**
	 * Content of the standard output for the process.
	 */
	private StringBuilder standardOutput = new StringBuilder();
	/**
	 * Content of the error output for the process.
	 */
	private StringBuilder errorOutput = new StringBuilder();
	/**
	 * The process to handle.
	 */
	private Process process = null;

	/**
	 * Initialize this process handler with the specified process. 
	 * @param process the process to handle.
	 */
	public ProcessWrapper(Process process)
	{
		this.process = process;
		new StreamHandler(process.getInputStream(), standardOutput).start();
		new StreamHandler(process.getErrorStream(), errorOutput).start();
	}

	/**
	 * Get the content of the error output for the process.
	 * @return the content as a <code>StringBuilder</code> instance.
	 */
	public StringBuilder getErrorOutput()
	{
		return errorOutput;
	}

	/**
	 * Get the content of the standard output for the process.
	 * @return the content as a <code>StringBuilder</code> instance.
	 */
	public StringBuilder getStandardOutput()
	{
		return standardOutput;
	}

	/**
	 * Get the process to handle.
	 * @return a <code>Process</code> instance.
	 */
	public Process getProcess()
	{
		return process;
	}

	/**
	 * Used to empty the standard or error output of a process, so as not to block the process.
	 */
	private class StreamHandler extends Thread
	{
		/**
		 * The stream to get the output from.
		 */
		private InputStream is = null;
		/**
		 * This is where the output goes.
		 */
		private StringBuilder sb = null;

		/**
		 * Initialize this handler with the specified stream and buffer receiving its content.
		 * @param is the stream where output is taken from.
		 * @param sb the buffer where the output is written.
		 */
		public StreamHandler(InputStream is, StringBuilder sb)
		{
			this.is = is;
			this.sb = sb;
		}

		/**
		 * Monitor the stream for avalaible data and write that data to the buffer.
		 * @see java.lang.Thread#run()
		 */
		public void run()
		{
			try
			{
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				String s = "";
				while (s != null)
				{
					s = reader.readLine();
					if (s != null) sb.append(s).append("\n");
				}
			}
			catch(Throwable t)
			{
				t.printStackTrace();
			}
		}
	}
}
