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

/**
 * This is a piece of market data.
 * @author Laurent Cohen
 */
public class MarketData implements Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Market data identifier.
   */
  private String id = "";

  /**
   * Default constructor.
   */
  public MarketData()
  {
  }

  /**
   * Initialize the trade with the specified identifier.
   * @param id the market data identifier.
   */
  public MarketData(final String id)
  {
    this.id = id;
  }

  /**
   * Get the market data identifier.
   * @return the id as a string.
   */
  public String getId()
  {
    return id;
  }

  /**
   * Set the market data identifier.
   * @param id the id as a string.
   */
  public void setId(final String id)
  {
    this.id = id;
  }
}
