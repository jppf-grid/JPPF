/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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
 * Test database job persistence.
 * @author Laurent Cohen
 */
public class TestDefaultDatabasePersistence extends AbstractJobPersistenceTest {
  /**
   * Starts the DB server and create the database with a test table.
   * @throws Exception if any error occurs.
   */
  @BeforeClass
  public static void setup() throws Exception {
    final String prefix = "job_persistence";
    final TestConfiguration config = dbSetup(prefix);
    config.driver.jppf = CONFIG_ROOT_DIR + prefix + "/driver_db.properties";
    client = BaseSetup.setup(1, 2, true, true, config);
  }
}
