/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

package org.jppf.server.protocol;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

import org.jppf.utils.*;

/**
 * Instances of this class represent the location of an artifact, generally a file or the data found at a url.
 * @param <T> the type of this location.
 * @author Laurent Cohen
 */
public abstract class AbstractLocation<T> implements Serializable, Location<T>
{
	/**
	 * The path for this location.
	 */
	protected T path = null;
	/**
	 * The list of listeners to this location.
	 */
	protected List<LocationEventListener> listeners = new ArrayList<LocationEventListener>();
	/**
	 * Boolean flag that determines if at least one listener is registered.
	 * Used to minimize the overhead of sending events if there is no listener.
	 */
	protected boolean eventsEnabled = false;

	/**
	 * Initialize this location with the specified type and path.
	 * @param path the path for this location.
	 */
	public AbstractLocation(T path)
	{
		this.path = path;
	}

	/**
	 * Return the path to this location.
	 * @return the path.
	 * @see org.jppf.server.protocol.Location#getPath()
	 */
	public T getPath()
	{
		return path;
	}

	/**
	 * Copy the content at this location to another location.
	 * @param location the location to copy to.
	 * @throws Exception if an I/O error occurs.
	 * @see org.jppf.server.protocol.Location#copyTo(org.jppf.server.protocol.Location)
	 */
	public void copyTo(Location location) throws Exception
	{
		InputStream is = getInputStream();
		OutputStream os = location.getOutputStream();
		copyStream(is, os);
		is.close();
		os.flush();
		os.close();
	}

	/**
	 * Get the content at this location as an array of bytes.
	 * @return a byte array.
	 * @throws Exception if an I/O error occurs.
	 * @see org.jppf.server.protocol.Location#toByteArray()
	 */
	public byte[] toByteArray() throws Exception
	{
		InputStream is = getInputStream();
		JPPFByteArrayOutputStream os = new JPPFByteArrayOutputStream();
		copyStream(is, os);
		is.close();
		os.flush();
		os.close();
		return os.toByteArray();
	}

	/**
	 * Get a string representation of this location.
	 * @return this location as a string.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return "" + getPath();
	}

	/**
	 * Add a listener to the list of location event listeners for this location.
	 * @param listener the listener to add to the list.
	 * @throws NullPointerException if the listener object is null.
	 * @see org.jppf.server.protocol.Location#addLocationEventListener(org.jppf.server.protocol.LocationEventListener)
	 */
	public void addLocationEventListener(LocationEventListener listener)
	{
		if (listener == null) throw new NullPointerException("null listener not accepted");
		listeners.add(listener);
		if (!eventsEnabled) eventsEnabled = true;
	}

	/**
	 * Remove a listener from the list of location event listeners for this location.
	 * @param listener the listener to remove from the list.
	 * @throws NullPointerException if the listener object is null.
	 * @see org.jppf.server.protocol.Location#removeLocationEventListener(org.jppf.server.protocol.LocationEventListener)
	 */
	public void removeLocationEventListener(LocationEventListener listener)
	{
		if (listener == null) throw new NullPointerException("null listener not accepted");
		listeners.remove(listener);
		if (listeners.isEmpty()) eventsEnabled = false;
	}

	/**
	 * Notify all listeners that a data transfer has occurred.
	 * @param n - the size of the data that was transferred.
	 */
	protected void fireLocationEvent(int n)
	{
		LocationEvent event = new LocationEvent(this, n);
		for (LocationEventListener l: listeners) l.dataTransferred(event);
	}

	/**
	 * Copy the data read from the specified input stream to the specified output stream. 
	 * @param is the input stream to read from.
	 * @param os the output stream to write to.
	 * @throws IOException if an I/O error occurs.
	 */
	private void copyStream(InputStream is, OutputStream os) throws IOException
	{
		ByteBuffer tmp = BufferPool.pickBuffer();
		byte[] bytes = tmp.array();
		while(true)
		{
			int n = is.read(bytes);
			if (eventsEnabled) fireLocationEvent(n);
			if (n <= 0) break;
			os.write(bytes, 0, n);
		}
		BufferPool.releaseBuffer(tmp);
	}
}
