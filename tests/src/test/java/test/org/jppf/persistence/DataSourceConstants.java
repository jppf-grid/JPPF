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

package test.org.jppf.persistence;

/**
 * 
 * @author Laurent Cohen
 */
public interface DataSourceConstants {
  /**
   * JDBC driver class name.
   */
  public static final String DB_DRIVER_CLASS = "org.h2.Driver";
  /**
   * Database name.
   */
  public static final String DB_NAME = "tests_jppf";
  /**
   * Database password.
   */
  public static final String DB_PWD = "";
  /**
   * Connection URL.
   */
  public static final String DB_URL = "jdbc:h2:tcp://localhost:9092/./root/" + DB_NAME;
  /**
   * Database user.
   */
  public static final String DB_USER = "sa";
  /**
   * Name of the test table.
   */
  public static final String TABLE_NAME = "TEST_TABLE";
  /**
   * Message used for successful task execution.
   */
  public static final String EXECUTION_SUCCESSFUL_MESSAGE = "execution successful";
}
