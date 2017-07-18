/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package test.org.jppf.job.persistence;

import org.junit.BeforeClass;

import test.org.jppf.test.setup.*;

/**
 * Test asynchronous database job persistence. 
 * @author Laurent Cohen
 */
public class TestDefaultFilePersistenceAsync extends AbstractJobPersistenceTest {
  /**
   * Starts the DB server and create the database with a test table.
   * @throws Exception if any error occurs.
   */
  @BeforeClass
  public static void setup() throws Exception {
    String prefix = "job_persistence";
    TestConfiguration config = dbSetup(prefix);
    config.driverJppf = "classes/tests/config/" + prefix + "/driver_async_file.properties";
    config.driverLog4j = "classes/tests/config/" + prefix + "/log4j-driver.template.properties";
    client = BaseSetup.setup(1, 2, true, true, config);
  }
}
