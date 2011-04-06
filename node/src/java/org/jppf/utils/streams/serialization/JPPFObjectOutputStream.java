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

package org.jppf.utils.streams.serialization;

import java.io.*;

/**
 * 
 * @author Laurent Cohen
 */
public class JPPFObjectOutputStream extends ObjectOutputStream
{
	/**
	 * The stream serialized data is written to.
	 */
	private DataOutputStream out;

	/**
	 * Initialize this object stream.
	 * @param out the stream to write objects to.
	 * @throws IOException if any error occurs.
	 */
	public JPPFObjectOutputStream(OutputStream out) throws IOException
	{
		super();
		this.out = new DataOutputStream(out);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void writeObjectOverride(Object obj) throws IOException
	{
		try
		{
			ObjectGraphSerializer ser = new ObjectGraphSerializer(this);
			ser.exploreRoot(obj);
		}
		catch (Exception e)
		{
			throw (e instanceof IOException) ? (IOException) e : new IOException(e.getMessage(), e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void write(int val) throws IOException
	{
		out.write(val);
	}

	/**
	 * {@inheritDoc}
	 */
	public void write(byte[] buf) throws IOException
	{
		out.write(buf);
	}

	/**
	 * {@inheritDoc}
	 */
	public void write(byte[] buf, int off, int len) throws IOException
	{
		out.write(buf, off, len);
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeBoolean(boolean val) throws IOException
	{
		out.writeBoolean(val);
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeByte(int val) throws IOException
	{
		out.writeByte(val);
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeShort(int val) throws IOException
	{
		out.writeShort(val);
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeChar(int val) throws IOException
	{
		out.writeChar(val);
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeInt(int val) throws IOException
	{
		out.writeInt(val);
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeLong(long val) throws IOException
	{
		out.writeLong(val);
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeFloat(float val) throws IOException
	{
		out.writeFloat(val);
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeDouble(double val) throws IOException
	{
		out.writeDouble(val);
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeBytes(String str) throws IOException
	{
		out.writeBytes(str);
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeChars(String str) throws IOException
	{
		out.writeChars(str);
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeUTF(String str) throws IOException
	{
		out.writeUTF(str);
	}
}
