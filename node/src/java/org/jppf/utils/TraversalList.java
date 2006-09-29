/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
	 * The actual list that backs this traversal list.
	 */
	private List<E> list = new ArrayList<E>();
	/**
	 * The current position in the list.
	 */
	private int position = -1;

	/**
	 * Default initialization.
	 */
	public TraversalList()
	{
	}

	/**
	 * Initialize this traversal list with a specified list with the same element type.
	 * @param list a list with the same element type as this traversal list.
	 */
	public TraversalList(List<E> list)
	{
		this.list = list;
	}

	/**
	 * Add a new element to the list.
	 * @param element the element to add.
	 */
	public void add(E element)
	{
		list.add(element);
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
			throw new ArrayIndexOutOfBoundsException("position "+(position+1)+" les than 0");
		}
		position--;
	}

	/**
	 * Get the list of elements of this traversal list. 
	 * @return an unmodifiable copy of the list.
	 */
	public List<E> getList()
	{
		return Collections.unmodifiableList(list);
	}

	/**
	 * Get the current position in the list.
	 * @return the position as an int value, -1 if the lsit is empty.
	 */
	public int getPosition()
	{
		return list.isEmpty() ? -1 : position;
	}

	/**
	 * Set the current position in the list.
	 * @param pos the position as an int value, -1 if the lsit is empty.
	 */
	public void setPosition(int pos)
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
	public boolean contains(E element)
	{
		return list.contains(element);
	}
}
