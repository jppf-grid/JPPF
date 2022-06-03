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

package org.jppf.utils;


/**
 * Instances of this class represent a hostname / ip address pair.
 */
public class HostIP extends Pair<String, String> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Initialize this HostIP object with the specified host name / IP address.
   * @param hostName the host name or IP address.
   */
  public HostIP(final String hostName) {
    this(hostName, hostName);
  }

  /**
   * Initialize this HostIP object with the specified host name and IP address.
   * @param hostName the host name.
   * @param ipAddress the corresponding IP address.
   */
  public HostIP(final String hostName, final String ipAddress) {
    super(hostName, ipAddress);
  }

  /**
   * Get the host name.
   * @return the name as a string.
   */
  public String hostName() {
    return first();
  }

  /**
   * Get the ip address.
   * @return the ip address as a string.
   */
  public String ipAddress() {
    return second();
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append(hostName()).append('|').append(ipAddress())
      .append(']').toString();
  }
}
