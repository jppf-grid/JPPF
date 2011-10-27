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

package test.org.jppf.test.setup;

/**
 * Unit tests for {@link JPPFExecutorService}.
 * This class starts and stops a driver and a node before and after
 * running the tests in a unit test class.
 * @author Laurent Cohen
 */
public class BaseSetup
{
	/**
	 * Message used for successful task execution.
	 */
	public static final String EXECUTION_SUCCESSFUL_MESSAGE = "execution successful";
}
