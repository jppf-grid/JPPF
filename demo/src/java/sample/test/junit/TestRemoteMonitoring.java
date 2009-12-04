/*
 * JPPF.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

import junit.framework.TestCase;

import org.jppf.management.*;

/**
 * This class tests the remote task management features of JPPF,
 * such as cancelling or restarting a task and receiving notifications.
 * This test assumes a driver is started with the default ports, and a
 * node is started with jmx port = 12001.
 * @author Laurent Cohen
 */
public class TestRemoteMonitoring extends TestCase
{
	/**
	 * Cancel task command.
	 */
	private static final int CANCEL = 1;
	/**
	 * Restart task command.
	 */
	private static final int RESTART = 2;

	/**
	 * Test the querying the state of a node.
	 * @throws Exception if the test failed.
	 */
	public void testNodeState() throws Exception
	{
		JMXNodeConnectionWrapper jmxClient = new JMXNodeConnectionWrapper("192.168.0.5", 12001);
		jmxClient.connect();
		while (!jmxClient.isConnected()) Thread.sleep(100L);
		JPPFNodeState state = jmxClient.state();
		assertNotNull(state);
	}
}
