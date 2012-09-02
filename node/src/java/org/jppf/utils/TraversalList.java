/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import java.io.Serializable;
import java.util.*;

/**
 * A list that maintains and manages a current position and corresponding current element.
 * @param <E> The element type for this list.
 * @author Laurent Cohen
 */
public class TraversalList<E> implements Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The actual list that backs this traversal list.
   */
  private final List<E> list;
  /**
   * The current position in the list.
   */
  private int position = -1;
  /**
   * String representation of this list.
   */
  private String toStr = null;

  /**
   * Default initialization.
   */
  public TraversalList()
  {
    list = new LinkedList<E>();
    toStr = list.toString();
  }

  /**
   * Initialize this traversal list with a specified list with the same element type.
   * @param list a list with the same element type as this traversal list.
   */
  public TraversalList(final List<E> list)
  {
    this.list = list;
    toStr = list.toString();
  }

  /**
   * Add a new element to the list.
   * @param element the element to add.
   */
  public void add(final E element)
  {
    list.add(element);
    toStr = list.toString();
  }

  /**
   * Get the first element in the list.
   * @return the first element.
   */
  public E getFirst()
  {
    return list.get(0);
  }

  /**
   * Get the last element in the list.
   * @return the last element.
   */
  public E getLast()
  {
    return list.get(list.size() - 1);
  }

  /**
   * Increment the current position by 1.
   */
  public void incPosition()
  {
    if (position >= list.size() - 1)
    {
      throw new ArrayIndexOutOfBoundsException("position "+(position+1)+" larger than " + list.size());
    }
    position++;
  }

  /**
   * Increment the current position by 1.
   */
  public void decPosition()
  {
    if (position <= 0)
    {
      throw new ArrayIndexOutOfBoundsException("position "+(position+1)+" less than 0");
    }
    position--;
  }

  /**
   * Get the list of elements of this traversal list.
   * @return an unmodifiable copy of the list.
   */
  public List<E> getList()
  {
    return list;
    //return Collections.unmodifiableList(list);
  }

  /**
   * Get the current position in the list.
   * @return the position as an int value, -1 if the list is empty.
   */
  public int getPosition()
  {
    return list.isEmpty() ? -1 : position;
  }

  /**
   * Set the current position in the list.
   * @param pos the position as an int value, -1 if the list is empty.
   */
  public void setPosition(final int pos)
  {
    if (list.isEmpty()) throw new IndexOutOfBoundsException("list is empty");
    else if (pos < 0) throw new IndexOutOfBoundsException("value "+pos+" is negative");
    else if (pos >= list.size()) throw new IndexOutOfBoundsException("value "+pos+" larger than "+(list.size()-1));
    this.position = pos;
  }

  /**
   * Return the element at the current position in the list.
   * @return the element at the current position.
   */
  public E getCurrentElement()
  {
    return list.get(position);
  }

  /**
   * Get the number of elements in this traversal list.
   * @return the size of the list as an int value.
   */
  public int size()
  {
    return list.size();
  }

  /**
   * Determine whether this traversal list contains a specified element.
   * @param element the element to lookup.
   * @return true if this list contains the element, false otherwise.
   */
  public boolean contains(final E element)
  {
    return list.contains(element);
  }

  @Override
  public String toString()
  {
    return toStr;
  }

  /**
   * Get a string rpresentation of this list.
   * @return a string equivalent to <code>toString()</code>.
   */
  public String asString()
  {
    return toStr;
  }

  @Override
  public boolean equals(final Object obj)
  {
    if (!(obj instanceof TraversalList)) return false;
    return list.equals(((TraversalList) obj).list);
    //return super.equals(obj);
  }

  @Override
  public int hashCode()
  {
    return list.hashCode();
  }
}
