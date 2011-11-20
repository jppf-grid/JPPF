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

package test.org.jppf.client;

import java.io.IOException;

import org.jppf.client.JPPFClient;
import org.junit.*;

import test.org.jppf.test.setup.Setup1D1N;

/**
 * Unit tests for {@link JPPFExecutorService}.
 * @author Laurent Cohen
 */
public class TestJPPFClient extends Setup1D1N
{
  /**
   * Launches a driver and node and start the client.
   * @throws IOException if a process could not be started.
   */
  @Before
  public void setupTest() throws IOException
  {
  }

  /**
   * Stops the driver and node and close the client.
   * @throws IOException if a process could not be stopped.
   */
  @After
  public void cleanupTest() throws IOException
  {
  }

  /**
   * Invocation of the <code>JPPFClient()</code> constructor.
   * @throws Exception if any error occurs
   */
  @Test
  public void testDefaultConstructor() throws Exception
  {
    JPPFClient client = new JPPFClient();
    client.close();
  }

  /**
   * Invocation of the <code>JPPFClient(String uuid)</code> constructor.
   * @throws Exception if any error occurs
   */
  @Test
  public void testConstructorWithUuid() throws Exception
  {
    JPPFClient client = new JPPFClient("some_uuid");
    client.close();
  }
}
