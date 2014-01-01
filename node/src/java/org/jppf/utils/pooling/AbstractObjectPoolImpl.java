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

package org.jppf.utils.pooling;

/**
 * 
 * @param <T>
 * @author Laurent Cohen
 */
public abstract class AbstractObjectPoolImpl<T> implements ObjectPool<T>
{
  /**
   * The pool of objects.
   */
  protected final LinkedData<T> data = new LinkedData<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public T get()
  {
    T t = data.get();
    return t == null ? create() : t;
  }

  /**
   * Create a new object for the pool.
   * @return an object that can be returned to the pool.
   */
  protected abstract T create();
 
  /**
   * {@inheritDoc}
   */
  @Override
  public void put(final T t)
  {
    data.put(t);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEmpty()
  {
    return data.isEmpty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size()
  {
    return data.size();
  }


  /**
   * @param <E>
   */
  public static class LinkedData<E>
  {
    /**
     * 
     */
    private LinkedNode<E> head = null;
    /**
     * 
     */
    private LinkedNode<E> tail = null;
    /**
     * 
     */
    int size = 0;

    /**
     * Add an object to the tail.
     * @param content the object to add.
     */
    public void put(final E content)
    {
      LinkedNode<E> node = new LinkedNode<>(content);
      synchronized(this)
      {
        if (tail != null)
        {
          node.next = tail;
          tail.prev = node;
        }
        else head = node;
        tail = node;
        size++;
      }
    }

    /**
     * Get the head object or null when queue is empty;
     * @return the head object or null.
     */
    public synchronized E get()
    {
      if (head == null) return null;
      LinkedNode<E> res = head;
      if (res.prev == null)
      {
        tail = null;
        head = null;
      }
      else
      {
        head = res.prev;
        head.next = null;
      }
      size--;
      return res.content;
    }

    /**
     * Get the size of this queue.
     * @return the size of this queue.
     */
    synchronized int size()
    {
      return size;
    }

    /**
     * Determine whether this queue is empty
     * @return whether this queue is empty
     */
    synchronized boolean isEmpty()
    {
      return head == null;
    }
  }

  /**
   * @param <E>
   */
  static class LinkedNode<E>
  {
    /**
     * 
     */
    final E content;
    /**
     * 
     */
    LinkedNode<E> prev = null;
    /**
     * 
     */
    LinkedNode<E> next = null;

    /**
     * Initialize this node with the specified content.
     * @param content the node's content.
     */
    LinkedNode(final E content)
    {
      this.content = content;
    }
  }
}
