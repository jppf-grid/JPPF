/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Trade identifier.
   */
  private String id = "";
  /**
   * A list of identifiers for the pieces of market data this depends on.
   */
  private SortedSet<String> dataDependencies = new TreeSet<>();

  /**
   * Default constructor.
   */
  public Trade()
  {
  }

  /**
   * Initialize the trade with the specified identifier.
   * @param id the trade identifier.
   */
  public Trade(final String id)
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
  public void setId(final String id)
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
  public void setDataDependencies(final SortedSet<String> dataDependencies)
  {
    this.dataDependencies = dataDependencies;
  }
}
