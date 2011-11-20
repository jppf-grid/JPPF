/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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
 */
class JPPFGetField extends GetField
{
  /**
   * Map of names to primitive values.
   */
  Map<String, Object> primitiveFields = new HashMap<String, Object>();
  /**
   * Map of names to object values.
   */
  Map<String, Object> objectFields = new HashMap<String, Object>();

  /**
   * {@inheritDoc}
   */
  @Override
  public ObjectStreamClass getObjectStreamClass()
  {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean defaulted(final String name) throws IOException
  {
    return (primitiveFields.get(name) == null) && (objectFields.get(name) == null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean get(final String name, final boolean val) throws IOException
  {
    Boolean r = (Boolean) primitiveFields.get(name);
    return r == null ? val : r;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte get(final String name, final byte val) throws IOException
  {
    Byte r = (Byte) primitiveFields.get(name);
    return r == null ? val : r;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public char get(final String name, final char val) throws IOException
  {
    Character r = (Character) primitiveFields.get(name);
    return r == null ? val : r;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public short get(final String name, final short val) throws IOException
  {
    Short r = (Short) primitiveFields.get(name);
    return r == null ? val : r;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int get(final String name, final int val) throws IOException
  {
    Integer r = (Integer) primitiveFields.get(name);
    return r == null ? val : r;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long get(final String name, final long val) throws IOException
  {
    Long r = (Long) primitiveFields.get(name);
    return r == null ? val : r;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public float get(final String name, final float val) throws IOException
  {
    Float r = (Float) primitiveFields.get(name);
    return r == null ? val : r;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double get(final String name, final double val) throws IOException
  {
    Double r = (Double) primitiveFields.get(name);
    return r == null ? val : r;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object get(final String name, final Object val) throws IOException
  {
    Object r = objectFields.get(name);
    return r == null ? val : r;
  }
}
