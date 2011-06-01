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

import org.jppf.utils.SerializationUtils;

/**
 * Implementation of {@link ObjectInputStream} that reads objects without regards to whether
 * they implement {@link Serializable} or not. This allows using non-serializable classes in
 * JPPF tasks, especially when their source code is not available.
 * <p>The rest of the {@link ObjectInputStream} specification is respected:
 * <ul>
 * <li>transient fields are not deserialized</li>
 * <li><code>private void readObject(ObjectInputStream)</code> is used whenever implemented</li>
 * <li>the {@link Externalized} interface is respected</li>
 * </ul>
 * @author Laurent Cohen
 */
public class JPPFObjectInputStream extends ObjectInputStream
{
	/**
	 * The stream to read data from.
	 */
	private DataInputStream in;
	/**
	 * The deserializer.
	 */
	private Deserializer deserializer;
	/**
	 * Determines whether the stream is already reading an object graph.
	 */
	private boolean readingObject = false;
	/**
	 * Temporary buffer to write primitive types.
	 */
	private final byte[] buf = new byte[8];

	/**
	 * Initialize this object input stream witht he specified stream.
	 * @param in the stream to read data from.
	 * @throws IOException if an I/O error occurs.
	 */
	public JPPFObjectInputStream(InputStream in) throws IOException
	{
		super();
		this.in = new DataInputStream(in);
		deserializer = new Deserializer(this);
	}

	/**
	 * {@inheritDoc}
	 */
	protected Object readObjectOverride() throws IOException, ClassNotFoundException
	{
		Object o = null;
		boolean alreadyReading = readingObject;
		try
		{
			if (!alreadyReading)
			{
				readingObject = true;
				o = deserializer.readObject();
			}
			else
			{
				o = deserializer.readObject();
			}
		}
		catch(Exception e)
		{
			if (e instanceof ClassNotFoundException) throw (ClassNotFoundException) e;
			else if (e instanceof IOException) throw (IOException) e;
			else throw new IOException(e.getMessage(), e);
		}
		finally
		{
			if (!alreadyReading) readingObject = false;
		}
		return o;
	}

	/**
	 * {@inheritDoc}
	 */
	public int read() throws IOException
	{
		return in.read();
	}

	/**
	 * {@inheritDoc}
	 */
	public int read(byte[] buf, int off, int len) throws IOException
	{
		return in.read(buf, off, len);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean readBoolean() throws IOException
	{
		return in.readBoolean();
	}

	/**
	 * {@inheritDoc}
	 */
	public byte readByte() throws IOException
	{
		return in.readByte();
	}

	/**
	 * {@inheritDoc}
	 */
	public char readChar() throws IOException
	{
		return in.readChar();
	}

	/**
	 * {@inheritDoc}
	 */
	public short readShort() throws IOException
	{
		return in.readShort();
	}

	/**
	 * {@inheritDoc}
	 */
	public int readInt() throws IOException
	{
		in.read(buf, 0, 4);
		//return in.readInt();
		return SerializationUtils.readInt(buf, 0);
	}

	/**
	 * {@inheritDoc}
	 */
	public long readLong() throws IOException
	{
		return in.readLong();
	}

	/**
	 * {@inheritDoc}
	 */
	public float readFloat() throws IOException
	{
		return in.readFloat();
	}

	/**
	 * {@inheritDoc}
	 */
	public double readDouble() throws IOException
	{
		return in.readDouble();
	}

	/**
	 * {@inheritDoc}
	 */
	public int skipBytes(int len) throws IOException
	{
		return in.skipBytes(len);
	}

	/**
	 * {@inheritDoc}
	 */
	public String readUTF() throws IOException
	{
		return in.readUTF();
	}

	/**
	 * {@inheritDoc}
	 */
	public void close() throws IOException
	{
		in.close();
	}

	/**
	 * {@inheritDoc}
	 */
	public int readUnsignedByte() throws IOException
	{
		return in.readUnsignedByte();
	}

	/**
	 * {@inheritDoc}
	 */
	public int readUnsignedShort() throws IOException
	{
		return in.readUnsignedShort();
	}

	/**
	 * {@inheritDoc}
	 */
	public void readFully(byte[] buf) throws IOException
	{
		in.readFully(buf);
	}

	/**
	 * {@inheritDoc}
	 */
	public void readFully(byte[] buf, int off, int len) throws IOException
	{
		in.readFully(buf, off, len);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("deprecation")
	public String readLine() throws IOException
	{
		return in.readLine();
	}

	/**
	 * {@inheritDoc}
	 */
	public void defaultReadObject() throws IOException, ClassNotFoundException
	{
		try
		{
			deserializer.readFields(deserializer.currentClassDescriptor, deserializer.currentObject);
		}
		catch(Exception e)
		{
			if (e instanceof ClassNotFoundException) throw (ClassNotFoundException) e;
			else if (e instanceof IOException) throw (IOException) e;
			else throw new IOException(e.getMessage(), e);
		}
	}
}
