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
 * @(#)CacheMap.java	1.3
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

package com.sun.jmx.remote.opt.util;

import java.lang.ref.SoftReference;
import java.util.*;

/**
 * Like WeakHashMap, except that the keys of the <em>n</em> most recently-accessed entries are kept as {@link SoftReference soft references}.
 * Accessing an element means creating it, or retrieving it with {@link #get(Object) get}. Because these entries are kept with soft references,
 * they will tend to remain even if their keys are not referenced elsewhere. But if memory is short, they will be removed.
 * @param <K> the type of the keys in this map.
 * @param <V> the type of the values in this map.
 */
public class CacheMap<K, V> extends WeakHashMap<K, V> {
  /**
   * List of soft references for the most-recently referenced keys.
   * The list is in most-recently-used order, i.e. the first element is the most-recently referenced key.
   * There are never more than nSoftReferences elements of this list.
   * If we didn't care about J2SE 1.3 compatibility, we could use LinkedHashSet in conjunction with a subclass of SoftReference whose equals and hashCode reflect the referent.
   */
  private final LinkedList<SoftReference<K>> cache = new LinkedList<>();
  /**
   * 
   */
  private final int nSoftReferences;

  /**
   * Create a <code>CacheMap</code> that can keep up to <code>nSoftReferences</code> as soft references.
   * @param nSoftReferences Maximum number of keys to keep as soft references. Access times for {@link #get(Object) get}
   * and {@link #put(Object, Object) put} have a component that scales linearly with <code>nSoftReferences</code>, so this value should not be too great.
   * @throws IllegalArgumentException if <code>nSoftReferences</code> is negative.
   */
  public CacheMap(final int nSoftReferences) {
    if (nSoftReferences < 0) throw new IllegalArgumentException("nSoftReferences = " + nSoftReferences);
    this.nSoftReferences = nSoftReferences;
  }

  @Override
  public V put(final K key, final V value) {
    cache(key);
    return super.put(key, value);
  }

  @Override
  @SuppressWarnings("unchecked")
  public V get(final Object key) {
    cache((K) key);
    return super.get(key);
  }

  /**
   * We don't override remove(Object) or try to do something with the map's iterators to detect removal.
   * So we may keep useless entries in the soft reference list for keys that have since been removed.
   * The assumption is that entries are added to the cache but never removed. But the behaviour is not wrong if they are in fact removed -- the caching is just less performant.
   * @param key the key to cache.
   */
  private void cache(final K key) {
    Iterator<SoftReference<K>> it = cache.iterator();
    while (it.hasNext()) {
      SoftReference<K> sref = it.next();
      K key1 = sref.get();
      if (key1 == null) it.remove();
      else if (key.equals(key1)) {
        // Move this element to the head of the LRU list
        it.remove();
        cache.add(0, sref);
        return;
      }
    }
    int size = cache.size();
    if (size == nSoftReferences) {
      if (size == 0) return; // degenerate case, equivalent to WeakHashMap
      it.remove();
    }
    cache.add(0, new SoftReference<>(key));
  }
}
