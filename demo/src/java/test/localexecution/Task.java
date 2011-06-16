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
package test.localexecution;

import java.io.File;
import java.net.URL;
import java.util.*;

import org.jppf.classloader.JPPFClassLoader;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;

/**
 * Test task.
 * @author Laurent Cohen
 */
public class Task extends JPPFTask
{
	/**
	 * Explicit serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * {@inheritDoc}
	 */
	public void run()
	{
		try
		{
			System.out.println("starting task");
			JPPFClassLoader cl = (JPPFClassLoader) getClass().getClassLoader();
			ClassLoaderWrapper wrapper = new ClassLoaderWrapper(cl);
			System.out.println("downloading jar file");
			URL url = cl.getResource("../samples-pack/shared/lib/hazelcast-1.9.3.jar");
			System.out.println("got URL: " + url);
			wrapper.addURL(url);
			Class c = cl.loadClass("com.hazelcast.core.Hazelcast");
			System.out.println("found class " + c);
			setResult(c.getName());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			setException(e);
		}
	}

	/**
	 * Downlaod the specified files.
	 * @author Laurent Cohen
	 */
	public static class DownloadCallable implements JPPFCallable<Map<String, byte[]>>
	{
		/**
		 * The local paths for the files to download.
		 */
		private List<String> paths;

		/**
		 * Initialize this callable.
		 * @param paths the local paths for the files to download.
		 */
		public DownloadCallable(List<String> paths)
		{
			this.paths = paths;
		}

		/**
		 * {@inheritDoc}
		 */
		public Map<String, byte[]> call() throws Exception
		{
			Map<String, byte[]> map = null;
			for (String path: paths)
			{
				try
				{
					File file = new File(path);
					if (!file.exists()) continue;
					byte[] bytes = FileUtils.getFileAsByte(file);
					if (map == null) map = new HashMap<String, byte[]>();
					map.put(path, bytes);
				}
				catch (Exception e)
				{
				}
			}
			return null;
		}
	}
}
