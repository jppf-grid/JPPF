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
public class JPPFGetField extends GetField
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
	public ObjectStreamClass getObjectStreamClass()
	{
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean defaulted(String name) throws IOException
	{
		return (primitiveFields.get(name) == null) && (objectFields.get(name) == null);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean get(String name, boolean val) throws IOException
	{
		Boolean r = (Boolean) primitiveFields.get(name);
		return r == null ? val : r;
	}

	/**
	 * {@inheritDoc}
	 */
	public byte get(String name, byte val) throws IOException
	{
		Byte r = (Byte) primitiveFields.get(name);
		return r == null ? val : r;
	}

	/**
	 * {@inheritDoc}
	 */
	public char get(String name, char val) throws IOException
	{
		Character r = (Character) primitiveFields.get(name);
		return r == null ? val : r;
	}

	/**
	 * {@inheritDoc}
	 */
	public short get(String name, short val) throws IOException
	{
		Short r = (Short) primitiveFields.get(name);
		return r == null ? val : r;
	}

	/**
	 * {@inheritDoc}
	 */
	public int get(String name, int val) throws IOException
	{
		Integer r = (Integer) primitiveFields.get(name);
		return r == null ? val : r;
	}

	/**
	 * {@inheritDoc}
	 */
	public long get(String name, long val) throws IOException
	{
		Long r = (Long) primitiveFields.get(name);
		return r == null ? val : r;
	}

	/**
	 * {@inheritDoc}
	 */
	public float get(String name, float val) throws IOException
	{
		Float r = (Float) primitiveFields.get(name);
		return r == null ? val : r;
	}

	/**
	 * {@inheritDoc}
	 */
	public double get(String name, double val) throws IOException
	{
		Double r = (Double) primitiveFields.get(name);
		return r == null ? val : r;
	}

	/**
	 * {@inheritDoc}
	 */
	public Object get(String name, Object val) throws IOException
	{
		Object r = objectFields.get(name);
		return r == null ? val : r;
	}
}
