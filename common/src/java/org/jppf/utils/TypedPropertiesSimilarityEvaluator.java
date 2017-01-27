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

package org.jppf.utils;

/**
 * Computes a similarity score between two instances of TypedProperties.
 * @author Laurent Cohen
 * @since 5.2
 */
public class TypedPropertiesSimilarityEvaluator {
  /**
   * Compute the cost of transforming the values of props2 into the values of the properties defined in props1.
   * @param props1 the properties to compare with.
   * @param props2 the rpoperties for which the transformation cost is computed.
   * @return an int representing the cost of transforming the values of props2 into the values of the properties defined in props1.
   */
  public int computeDistance(final TypedProperties props1, final TypedProperties props2) {
    int totalScore = 0;
    for (String name: props1.stringPropertyNames()) {
      totalScore += levenshteinDistance(props1.getString(name), props2.getString(name));
    }
    return totalScore;
  }

  /**
   * Compute the Levenshtein distance between two character sequences.
   * <p>This code is adapted from the <a href="https://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Java">Wikibooks algorithm</a>.
   * @param lhs the first char sequence to compare.
   * @param rhs the second char sequence to compare.
   * @return the Levenshtein distance between the 2 character sequences as an int.
   */
  private int levenshteinDistance(final CharSequence lhs, final CharSequence rhs) {
    if (lhs == rhs) return 0;
    else if (lhs == null) return rhs.length();
    else if (rhs == null) return lhs.length();
    else if (lhs.equals(rhs)) return 0;
    else if (lhs.length() <= 0) return rhs.length();
    else if (rhs.length() <= 0) return lhs.length();

    int len0 = lhs.length() + 1;
    int len1 = rhs.length() + 1;
    // the array of distances
    int[] cost = new int[len0];
    int[] newcost = new int[len0];

    // initial cost of skipping prefix in String s0
    for (int i = 0; i < len0; i++) cost[i] = i;

    // dynamically computing the array of distances
    // transformation cost for each letter in s1
    for (int j = 1; j < len1; j++) {
      // initial cost of skipping prefix in String s1
      newcost[0] = j;
      // transformation cost for each letter in s0
      for (int i = 1; i < len0; i++) {
        // matching current letters in both strings
        int match = (lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1;
        // computing cost for each transformation
        int cost_replace = cost[i - 1] + match;
        int cost_insert = cost[i] + 1;
        int cost_delete = newcost[i - 1] + 1;
        // keep minimum cost
        newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
      }

      // swap cost/newcost arrays
      int[] swap = cost;
      cost = newcost;
      newcost = swap;
    }
    // the distance is the cost for transforming all letters in both strings
    return cost[len0 - 1];
  }
}
