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

import org.jppf.utils.SerializationUtils;

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
	 * Determines whether the stream is already writing an object graph.
	 */
	private boolean writingObject = false;
	/**
	 * The object graph serializer.
	 */
	private Serializer serializer = null;
	/**
	 * Temporary buffer to write primitive types.
	 */
	private final byte[] buf = new byte[8];

	/**
	 * Initialize this object stream.
	 * @param out the stream to write objects to.
	 * @throws IOException if any error occurs.
	 */
	public JPPFObjectOutputStream(OutputStream out) throws IOException
	{
		super();
		this.out = new DataOutputStream(out);
		serializer = new Serializer(this);
	}

	/**
	 * {@inheritDoc}
	 */
	protected final void writeObjectOverride(Object obj) throws IOException
	{
		boolean alreadyWriting = writingObject;
		try
		{
			if (!alreadyWriting)
			{
				writingObject = true;
				serializer.writeObject(obj);
			}
			else
			{
				serializer.writeObject(obj);
			}
		}
		catch (Exception e)
		{
			throw (e instanceof IOException) ? (IOException) e : new IOException(e.getMessage(), e);
		}
		finally
		{
			if (!alreadyWriting)
			{
				writingObject = false;
				flush();
			}
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
		SerializationUtils.writeInt(val, buf, 0);
		out.write(buf, 0, 4);
		//out.writeInt(val);
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
		//out.writeDouble(val);
		out.writeLong(Double.doubleToLongBits(val));
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

	/**
	 * {@inheritDoc}
	 */
	public void defaultWriteObject() throws IOException
	{
		try
		{
			serializer.writeFields(serializer.currentObject, serializer.currentClassDescriptor);
		}
		catch(Exception e)
		{
			if (e instanceof IOException) throw (IOException) e;
			else throw new IOException(e.getMessage(), e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void flush() throws IOException
	{
		out.flush();
	}

	/**
	 * {@inheritDoc}
	 */
	public void close() throws IOException
	{
		out.close();
	}
}
