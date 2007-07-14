/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.jca.spi;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnectionMetaData;

/**
 * Implementation of the ManagedConnectionMetaData interface.
 * @author Laurent Cohen
 */
public class JPPFManagedConnectionMetaData implements ManagedConnectionMetaData
{
	/**
	 * Name of the user of the connection.
	 */
	private String userName;

  /**
   * Initialize this metadata with a specified user name.
   * @param userName the name of the user of the connection.
   */
  public JPPFManagedConnectionMetaData(final String userName)
  {
    this.userName = userName;
  }

	/**
	 * Get the name of the product.
	 * @return the product name as a string.
	 * @throws ResourceException if the product name could not be obtained.
	 * @see javax.resource.spi.ManagedConnectionMetaData#getEISProductName()
	 */
	public String getEISProductName() throws ResourceException
	{
		return "JPPF";
	}

	/**
	 * Get the version of the product.
	 * @return the version as a string.
	 * @throws ResourceException if the version could not be obtained.
	 * @see javax.resource.spi.ManagedConnectionMetaData#getEISProductVersion()
	 */
	public String getEISProductVersion() throws ResourceException
	{
		return "JPPF 1.0 beta";
	}

	/**
	 * Get the maximum number of connections.
	 * @return the number of connections as an int.
	 * @throws ResourceException if the number of connections could not be obtained.
	 * @see javax.resource.spi.ManagedConnectionMetaData#getMaxConnections()
	 */
	public int getMaxConnections() throws ResourceException
	{
		return 10;
	}

	/**
	 * Get the name of the user of the connection.
	 * @return the name as a stirng.
	 * @throws ResourceException if the name could not be obrained.
	 * @see javax.resource.spi.ManagedConnectionMetaData#getUserName()
	 */
	public String getUserName() throws ResourceException
	{
		return userName;
	}

}
