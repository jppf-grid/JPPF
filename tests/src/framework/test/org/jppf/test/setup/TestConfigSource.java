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

package test.org.jppf.test.setup;

import java.io.*;

import org.jppf.utils.*;

/** */
public class TestConfigSource implements JPPFConfiguration.ConfigurationSourceReader {
  /**
   * Path to the client configuration file.
   */
  private static String clientConfig = null;

  @Override
  public Reader getPropertyReader() throws IOException {
    if (clientConfig == null) return null;
    return FileUtils.getFileReader(clientConfig);
  }

  /**
   * Get the path to the client configuration file.
   * @return the path as a string.
   */
  public static String getClientConfig() {
    return clientConfig;
  }

  /**
   * Set the path to the client configuration file.
   * @param clientConfig the path as a string.
   */
  public static void setClientConfig(final String clientConfig) {
    TestConfigSource.clientConfig = clientConfig;
  }
}