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
package org.jppf.example.gridfailover;

import static org.jppf.utils.configuration.JPPFProperties.*;

import org.jppf.JPPFException;
import org.jppf.discovery.ClientConnectionPoolInfo;
import org.jppf.discovery.ClientDriverDiscovery;
import org.jppf.utils.JPPFConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A client-side driver dicovery which reads the definitions of the drivers from a yaml file.
 * @author Laurent Cohen
 */
public class ClientSideDiscovery extends ClientDriverDiscovery {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ClientSideDiscovery.class);

  @Override
  public void discover() throws InterruptedException {
    try {
      configureReconnectionBehavior();
      // locate the drivers definition file in YAML format
      final String yamlPath = JPPFConfiguration.getProperties().getString("drivers.definition.file", "drivers.yaml");
      // read and parse the drivers definition file
      final ClientConnectionPoolInfo[] infos = Utils.parseYaml(yamlPath, ClientConnectionPoolInfo[].class);
      if ((infos == null) || (infos.length <= 0))
        throw new JPPFException("did not discover any driver to connect to");

      int priority = infos.length;
      for (final ClientConnectionPoolInfo info: infos) {
        // set the driver priority in descending order
        info.setPriority(priority--);
        System.out.println("discovered new driver: " + info);
        // notify the client that a new driver was discovered
        newConnection(info);
      }
    } catch (final Exception e) {
      log.error("error discovering the drivers", e);
    }
  }

  /**
   * Configure how the client will attempt to re-establish a broken driver connection.
   */
  static void configureReconnectionBehavior() {
    JPPFConfiguration
      // how long to try to reconnect, -1 means never stop
      .set(RECONNECT_MAX_TIME, -1L)
      // interval in seconds between reconnection attempts
      .set(RECONNECT_INTERVAL, 5L);
  }
}
