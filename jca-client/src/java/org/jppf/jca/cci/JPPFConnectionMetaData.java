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

package org.jppf.jca.cci;

import javax.resource.cci.ConnectionMetaData;

/**
 * Metadata for a JPPFConnection.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFConnectionMetaData implements ConnectionMetaData
{
  /**
   * Name of the user fo a connection
   */
  private String userName;

  /**
   * Initialize this metadata with a user name.
   * @param userName the name as a string.
   */
  public JPPFConnectionMetaData(final String userName)
  {
    this.userName = userName;
  }

  @Override
  public String getEISProductName()
  {
    return "JPPF";
  }

  @Override
  public String getEISProductVersion()
  {
    return "4.0";
  }

  @Override
  public String getUserName()
  {
    return userName;
  }
}
