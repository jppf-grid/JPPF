/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.example.datadependency.model;

import java.io.Serializable;
import java.util.*;

/**
 * This is a trade.
 * @author Laurent Cohen
 */
public class Trade implements Serializable
{
	/**
	 * Trade identifier.
	 */
	private String id = "";
	/**
	 * A list of identifiers for the pieces of market data this depends on.
	 */
	private SortedSet<String> dataDependencies = new TreeSet<String>(); 

	/**
	 * Default constructor.
	 */
	public Trade()
	{
	}

	/**
	 * Iniitialize the trade with the specified identifier.
	 * @param id the trade identifier.
	 */
	public Trade(String id)
	{
		this.id = id;
	}

	/**
	 * Get the trade identifier.
	 * @return the id as a string.
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * Set the trade identifier.
	 * @param id the id as a string.
	 */
	public void setId(String id)
	{
		this.id = id;
	}

	/**
	 * Get the list of dependencies for this trade.
	 * @return a list of market data identifier strings.
	 */
	public SortedSet<String> getDataDependencies()
	{
		return dataDependencies;
	}

	/**
	 * Set the list of dependencies for this trade.
	 * @param dataDependencies a list of market data identifier strings.
	 */
	public void setDataDependencies(SortedSet<String> dataDependencies)
	{
		this.dataDependencies = dataDependencies;
	}
}
