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

package test.org.jppf.jmxremote;

import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class ConnectorTest implements ConnectorTestMBean {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ConnectorTest.class);
  /**
   * 
   */
  private String stringParam;
  /**
   * 
   */
  private int intParam;

  @Override
  public String test1(final String stringParam, final int intParam) {
    return "[" + stringParam + " - " + intParam + "]";
  }

  @Override
  public String getStringParam() {
    return stringParam;
  }

  @Override
  public void setStringParam(final String stringParam) {
    log.info("setting stringParam = {}", stringParam);
    this.stringParam = stringParam;
  }

  @Override
  public int getIntParam() {
    return intParam;
  }

  @Override
  public void setIntParam(final int intParam) {
    log.info("setting intParam = {}", intParam);
    this.intParam = intParam;
  }
}
