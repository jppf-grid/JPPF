/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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
/*
 * @(#)ArrayQueue.java	1.3
 *
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * The contents of this file are subject to the terms of either the GNU General
 * Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL")(collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of the
 * License at http://opendmk.dev.java.net/legal_notices/licenses.txt or in the
 * LEGAL_NOTICES folder that accompanied this code. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file found at
 *     http://opendmk.dev.java.net/legal_notices/licenses.txt
 * or in the LEGAL_NOTICES folder that accompanied this code.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.
 *
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 *
 *       "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding
 *
 *       "[Contributor] elects to include this software in this distribution
 *        under the [CDDL or GPL Version 2] license."
 *
 * If you don't indicate a single choice of license, a recipient has the option
 * to distribute your version of this file under either the CDDL or the GPL
 * Version 2, or to extend the choice of license to its licensees as provided
 * above. However, if you add GPL Version 2 code and therefore, elected the
 * GPL Version 2 license, then the option applies only if the new code is made
 * subject to such option by the copyright holder.
 *
 */

package com.sun.jmx.remote.opt.internal;

import java.lang.reflect.Array;
import java.util.AbstractList;

/**
 * @param <E> .
 */
public class ArrayQueue<E> extends AbstractList<E> {
  /**
   *
   */
  private int capacity;
  /**
   *
   */
  private E[] queue;
  /**
   *
   */
  private int head;
  /**
   *
   */
  private int tail;
  /**
   *
   */
  private final Class<E> clazz;

  /**
   *
   * @param clazz .
   * @param capacity .
   */
  public ArrayQueue(final Class<E> clazz, final int capacity) {
    if (clazz == null) throw new IllegalArgumentException("class cannot be null");
    this.clazz = clazz;
    this.capacity = capacity + 1;
    this.queue = createArray(capacity + 1);
    this.head = 0;
    this.tail = 0;
  }

  /**
   *
   * @param newcapacity .
   */
  public void resize(final int newcapacity) {
    int size = size();
    if (newcapacity < size) throw new IndexOutOfBoundsException("Resizing would lose data");
    int nc = newcapacity;
    nc++;
    if (nc == this.capacity) return;
    //Object[] newqueue = new Object[newcapacity];
    E[] newqueue = createArray(nc);
    for (int i = 0; i < size; i++) newqueue[i] = get(i);
    this.capacity = nc;
    this.queue = newqueue;
    this.head = 0;
    this.tail = size;
  }

  @Override
  public boolean add(final E o) {
    queue[tail] = o;
    int newtail = (tail + 1) % capacity;
    if (newtail == head) throw new IndexOutOfBoundsException("Queue full");
    tail = newtail;
    return true; // we did add something
  }

  @Override
  public E remove(final int i) {
    //if (i != 0) throw new IllegalArgumentException("Can only remove head of queue");
    if (head == tail) throw new IndexOutOfBoundsException("Queue empty");
    E removed = queue[head];
    if (i != 0) {
      if (i > tail) throw new IndexOutOfBoundsException(String.format("Queue empty, index=%d, queue size=%d", i, tail));
      if (i == tail) {
        tail--;
        queue[i] = null;
      } else {
        int n = size();
        queue[i] = null;
        System.arraycopy(queue, i+1, queue, i, n - (i + 1));
      }
    } else {
      queue[head] = null;
      head = (head + 1) % capacity;
    }
    return removed;
  }

  @Override
  public E get(final int i) {
    int size = size();
    if ((i < 0) || (i >= size)) throw new IndexOutOfBoundsException(String.format("Index %d, queue size %d", i, size));
    int index = (head + i) % capacity;
    return queue[index];
  }

  @Override
  public int size() {
    // Can't use % here because it's not mod: -3 % 2 is -1, not +1.
    int diff = tail - head;
    if (diff < 0) diff += capacity;
    return diff;
  }

  /**
   *
   * @param capacity .
   * @return .
   */
  @SuppressWarnings("unchecked")
  private E[] createArray(final int capacity) {
    return (E[]) Array.newInstance(clazz, capacity);
  }
}
