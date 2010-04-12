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
package org.jppf.classloader;

import java.io.*;
import java.security.PrivilegedAction;

import org.jppf.JPPFException;

/**
 * Privileged action wrapper for saving a resource definition to a temporary file.
 */
class SaveFileAction implements PrivilegedAction<File>
{
	/**
	 * The resource definition to save.
	 */
	private final byte[] definition;
	/**
	 * An eventually resulting exception.
	 */
	private Exception exception = null;

	/**
	 * Initialize this action with the specified resource definition.
	 * @param definition the resource definition to save.
	 */
	public SaveFileAction(final byte[] definition)
	{
		this.definition = definition;
	}

	/**
	 * Execute this action.
	 * @return the abstract path for the created file.
	 * @see java.security.PrivilegedAction#run()
	 */
	public File run()
	{
		File tmp = null;
		try
		{
			/*
			System.out.println("tmpDir: " + System.getProperty("java.io.tmpdir") + ", user.name: " + System.getProperty("user.name") +
				", user.home: " + System.getProperty("user.home") + ", user.dir: " + System.getProperty("user.dir"));
			*/
			try
			{
				tmp = File.createTempFile("jppftemp_", ".tmp");
			}
			catch(IOException e)
			{
				File tmpDir = new File(System.getProperty("user.dir") + File.separator + "temp" + File.separator);
				boolean created = true;
				if (!tmpDir.exists() || !tmpDir.isDirectory()) created = tmpDir.mkdirs();
				if (created) tmp = File.createTempFile("jppftemp_", ".tmp", tmpDir);
				else throw new JPPFException("Could not create temp dir " + tmpDir.getCanonicalPath());
			}
			tmp.deleteOnExit();
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tmp));
			bos.write(definition);
			bos.flush();
			bos.close();
		}
		catch(Exception e)
		{
			exception = e;
		}
		return tmp;
	}

	/**
	 * Get the resulting exception.
	 * @return an <code>Exception</code> or null if no exception was raised.
	 */
	public Exception getException()
	{
		return exception;
	}
}
