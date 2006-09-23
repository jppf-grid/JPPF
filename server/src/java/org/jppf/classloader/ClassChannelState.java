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
package org.jppf.classloader;

import java.io.*;
import org.jppf.node.JPPFResourceWrapper;
import org.jppf.server.ChannelState;

/**
 * Abstract superclass for all states of the class server.
 * @author Laurent Cohen
 */
public abstract class ClassChannelState implements ChannelState
{
	/**
	 * The JPPFNIOServer this state relates to.
	 */
	protected ClassServer server;

	/**
	 * Initialize this state with a specified ClassServer.
	 * @param server the ClassServer this state relates to.
	 */
	protected ClassChannelState(ClassServer server)
	{
		this.server = server;
	}

	/**
	 * Deserialize a resource wrapper from an array of bytes.
	 * @param bytes the byte array containing the serialized resource.
	 * @return a <code>JPPFResourceWrapper</code> instance.
	 * @throws IOException if an error occurs while deserializing.
	 */
	public JPPFResourceWrapper readResource(byte[] bytes) throws IOException
	{
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = new ObjectInputStream(bais);
		JPPFResourceWrapper resource = null;
		try
		{
			resource = (JPPFResourceWrapper) ois.readObject();
		}
		catch (Exception e)
		{
			if (e instanceof IOException) throw (IOException) e;
			else throw new IOException(e.getMessage());
		}
		ois.close();
		return resource;
	}

	/**
	 * Serialize a resource wrapper to an array of bytes.
	 * @param resource the resource wrapper to serialize.
	 * @return a byte array containing the serialized resource.
	 * @throws IOException if an error occurs while serializing.
	 */
	public byte[] writeResource(JPPFResourceWrapper resource) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream()
		{
			public synchronized byte[] toByteArray()
			{
				return buf;
			}
		};
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		try
		{
			oos.writeObject(resource);
		}
		catch (Exception e)
		{
			if (e instanceof IOException) throw (IOException) e;
			else throw new IOException(e.getMessage());
		}
		oos.close();
		return baos.toByteArray();
	}
}
