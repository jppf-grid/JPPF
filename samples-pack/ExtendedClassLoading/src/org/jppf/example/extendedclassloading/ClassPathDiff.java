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

package org.jppf.example.extendedclassloading;

import java.util.*;

/**
 * Instances of this class compute the differences between two {@link ClassPath} instances.
 * @author Laurent Cohen
 */
public class ClassPathDiff
{
  /**
   * The list of elements that are the same in the source and the target classpath.
   */
  private final List<String> unchangedElements = new ArrayList<>();
  /**
   * The list of elements that are in the target but not in the source classpath.
   */
  private final List<String> newElements = new ArrayList<>();
  /**
   * The list of elements that are that are in the source and whose signature is different in the target classpath.
   */
  private final List<String> updatedElements = new ArrayList<>();
  /**
   * The list of elements that are in the source and not in the target classpath.
   */
  private final List<String> deletedElements = new ArrayList<>();

  /**
   * Initialize this diff-er with the specified source and target classpaths.
   * This object does not keep any reference to the specified classpaths,
   * it only computes the differences between them at construction time.
   * @param source the source classpath.
   * @param target the target classpath.
   */
  public ClassPathDiff(final ClassPath source, final ClassPath target)
  {
    computeDifferences(source, target);
  }

  /**
   * Get he list of elements that are the same in the source and the target classpath.
   * @return a list of unchanged element names.
   */
  public List<String> getUnchangedElements()
  {
    return unchangedElements;
  }

  /**
   * Get the list of elements that are in the target but not in the source classpath.
   * @return a list of new element names.
   */
  public List<String> getNewElements()
  {
    return newElements;
  }

  /**
   * Get the list of elements that are that are in the source and whose signature is different in the target classpath.
   * @return a list of updated element names.
   */
  public List<String> getUpdatedElements()
  {
    return updatedElements;
  }

  /**
   * Get the list of elements that are in the source and not in the target classpath.
   * @return a list of deleted element names.
   */
  public List<String> getDeletedElements()
  {
    return deletedElements;
  }

  /**
   * Compute the differences between both classpaths.
   * @param source the source classpath.
   * @param target the target classpath.
   */
  private void computeDifferences(final ClassPath source, final ClassPath target)
  {
    for (Map.Entry<String, String> sourceEntry: source.elements().entrySet())
    {
      String name = sourceEntry.getKey();
      String targetSignature = target.getElementSignature(name);
      // element in the source but not in the target: it has been deleted
      if (targetSignature == null) deletedElements.add(name);
      else
      {
        String sourceSignature = sourceEntry.getValue();
        // source signature different from the target: element has been updated
        if (!targetSignature.equals(sourceSignature)) updatedElements.add(name);
        // signatures are equal: no change
        else unchangedElements.add(name);
      }
    }
    // compute which elements were added
    Map<String, String> targetMap = target.elements();
    for (Map.Entry<String, String> targetEntry: target.elements().entrySet())
    {
      String name = targetEntry.getKey();
      String sourceSignature = source.getElementSignature(name);
      // element in the target but not in the source: it has been added
      if (sourceSignature == null) newElements.add(name);
    }
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append("[unchanged=").append(unchangedElements);
    sb.append(", new=")     .append(newElements);
    sb.append(", updated=") .append(updatedElements);
    sb.append(", deleted=") .append(deletedElements);
    sb.append(']');
    return sb.toString();
  }
}
