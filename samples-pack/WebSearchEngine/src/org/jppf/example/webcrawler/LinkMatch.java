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
package org.jppf.example.webcrawler;

import java.io.Serializable;

/**
 * Instances of this class represent a match for the search query.
 */
public class LinkMatch implements Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The url of the link matching the query.
   */
  public String url = null;
  /**
   * The relevance score of this match.
   */
  public float relevance = 0f;

  /**
   * Initialize this match with the specified parameters.
   * @param url the url of the link matching the query.
   * @param relevance the relevance score of this match.
   */
  public LinkMatch(final String url, final float relevance)
  {
    this.url = url;
    this.relevance = relevance;
  }

  /**
   * Get the hash code for this object.
   * @return the hash code as an int.
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode()
  {
    return (url == null) ? 0 : url.hashCode();
  }

  /**
   * Determine whether this object is equal to another.
   * @param obj the object to compare with.
   * @return true if the objects are equal, false otherwise.
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final Object obj)
  {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final LinkMatch other = (LinkMatch) obj;
    if (url == null)
    {
      if (other.url != null) return false;
    }
    else if (!url.equals(other.url)) return false;
    return true;
  }

  /**
   * A comparator for <code>LinkMatch</code> instances, used to sort them in descending
   * order of their relevance score.
   */
  public static class Comparator implements java.util.Comparator<LinkMatch>, Serializable
  {
    /**
     * Compare 2 LinkMatch objects in descending order of their relevance.
     * @param lm1 the first object to compare.
     * @param lm2 the second object to compare.
     * @return a negative value if the first object is less than the second,
     * a positive value if it is more, 0 otherwise.
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(final LinkMatch lm1, final LinkMatch lm2)
    {
      if (lm1 == null) return (lm2 == null) ? 0 : -1;
      else if (lm2 == null) return 1;
      return lm1.relevance > lm2.relevance ? -1 : (lm1.relevance < lm2.relevance ? 1 : 0);
    }
  }
}
