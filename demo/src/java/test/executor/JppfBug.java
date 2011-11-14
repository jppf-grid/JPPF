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

package test.executor;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

import org.jppf.client.JPPFClient;
import org.jppf.client.concurrent.*;
import org.slf4j.*;
/**
 * 
 * @author Laurent Cohen
 */
public class JppfBug
{
	/**
	 * 
	 */
	public static class HelloWorld implements Callable<String>, Serializable
	{
		/**
		 * Serial version UID.
		 */
		public static final long serialVersionUID = 1L;

		/**
		 * {@inheritDoc}
		 */
		public String call() throws Exception
		{
			return "hello world";
		}
	}

	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(BatchHandler.class);

	/**
	 * Entry point.
	 * @param args not used.
	 */
	public static void main(String[] args)
	{
		JPPFClient client = new JPPFClient();
		ExecutorService executor = new JPPFExecutorService(client);
		List<Future<String>> futures = new ArrayList<Future<String>>();

		try
		{
			for (int i = 0; i < 10; i++)
			{
				Future<String> future = executor.submit(new HelloWorld());
				futures.add(future);
			}
			for (Future<String> future : futures)
			{
				String result = future.get(10000, TimeUnit.MILLISECONDS);
				System.out.println(result);
			}
		}
		catch (Exception ex)
		{
			log.error(ex.getClass().getSimpleName(), ex);
		}
		finally
		{
			try
			{
				executor.shutdownNow();
				client.close();
			}
			catch (Exception e)
			{
				log.error(e.getClass().getSimpleName(), e);
			}
			finally
			{
				//System.exit(0); // http://www.jppf.org/forums/index.php/topic,1672.0.html
			}
		}
	}
}
