/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.serialization;

import java.io.*;
import java.io.ObjectInputStream.GetField;
import java.util.*;

/**
 * JPPF implementation of the GetField API.
 * @author Laurent Cohen
 * @exclude
 */
class JPPFGetField extends GetField {
  /**
   * Map of names to primitive values.
   */
  final Map<String, Object> primitiveFields = new HashMap<>();
  /**
   * Map of names to object values.
   */
  final Map<String, Object> objectFields = new HashMap<>();

  @Override
  public ObjectStreamClass getObjectStreamClass() {
    return null;
  }

  @Override
  public boolean defaulted(final String name) throws IOException {
    return (primitiveFields.get(name) == null) && (objectFields.get(name) == null);
  }

  @Override
  public boolean get(final String name, final boolean val) throws IOException {
    final Boolean r = (Boolean) primitiveFields.get(name);
    return r == null ? val : r;
  }

  @Override
  public byte get(final String name, final byte val) throws IOException {
    final Byte r = (Byte) primitiveFields.get(name);
    return r == null ? val : r;
  }

  @Override
  public char get(final String name, final char val) throws IOException {
    final Character r = (Character) primitiveFields.get(name);
    return r == null ? val : r;
  }

  @Override
  public short get(final String name, final short val) throws IOException {
    final Short r = (Short) primitiveFields.get(name);
    return r == null ? val : r;
  }

  @Override
  public int get(final String name, final int val) throws IOException {
    final Integer r = (Integer) primitiveFields.get(name);
    return r == null ? val : r;
  }

  @Override
  public long get(final String name, final long val) throws IOException {
    final Long r = (Long) primitiveFields.get(name);
    return r == null ? val : r;
  }

  @Override
  public float get(final String name, final float val) throws IOException {
    final Float r = (Float) primitiveFields.get(name);
    return r == null ? val : r;
  }

  @Override
  public double get(final String name, final double val) throws IOException {
    final Double r = (Double) primitiveFields.get(name);
    return r == null ? val : r;
  }

  @Override
  public Object get(final String name, final Object val) throws IOException {
    final Object r = objectFields.get(name);
    return r == null ? val : r;
  }
}
