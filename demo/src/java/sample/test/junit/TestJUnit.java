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

package sample.test.junit;

import java.io.Serializable;

import junit.framework.TestCase;

import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;

/**
 * 
 * @author Laurent Cohen
 */
public class TestJUnit extends TestCase implements Serializable
{
	/**
	 * First test.
	 * @throws Exception if the test fails.
	 */
	public void test1() throws Exception
	{
		JPPFClient client = new JPPFClient();
		JPPFJob job = new JPPFJob();
		job.addTask(new JPPFTask()
		{
      private static final long serialVersionUID = 1L;
      public void run()
      {
    		System.out.println("executing task");
      }
		});
		client.submit(job);
		System.out.println("before close");
		client.close();
		System.out.println("after close");
 	}
}
