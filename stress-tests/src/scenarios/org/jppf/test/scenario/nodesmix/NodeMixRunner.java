/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.test.scenario.nodesmix;

import java.util.concurrent.ExecutorService;

import org.jppf.client.JPPFClient;
import org.jppf.test.scenario.AbstractScenarioRunner;
import org.jppf.utils.TypedProperties;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class NodeMixRunner extends AbstractScenarioRunner {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(NodeMixRunner.class);
  /**
   * Executes job submissions in parallel by the same client.
   */
  private ExecutorService executor;
  /**
   * The JPPF client.
   */
  JPPFClient client;

  @Override
  public void run() {
    client = getSetup().getClient();
    TypedProperties config = getConfiguration().getProperties();
    try {
      StreamUtils.waitKeyPressed("Press [Enter] to terminate ...");
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (executor != null) executor.shutdownNow();
    }
  }
}
