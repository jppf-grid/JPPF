/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.io;

import java.nio.channels.*;

/**
 * This interface represents an abstraction of a task bundle's piece of data, regardless of where it is stored.
 * @author Laurent Cohen
 */
public interface DataLocation
{
	/**
	 * Get the size of the data referenced by this data location.
	 * @return the data size as an int.
	 */
	int getSize();

	/**
	 * Transfer the content of this data location from the specified input source.
	 * @param source the input source to transfer to.
	 * @param blocking if true, the method will block until the entire content has been transferred.
	 * @return the number of bytes actually transferred. 
	 * @throws Exception if an IO error occurs.
	 */
	int transferFrom(InputSource source, boolean blocking) throws Exception;

	/**
	 * Transfer the content of this data location from the specified channel.
	 * @param source the channel to transfer to.
	 * @param blocking if true, the method will block until the entire content has been transferred.
	 * @return the number of bytes actually transferred. 
	 * @throws Exception if an IO error occurs.
	 */
	int transferFrom(ReadableByteChannel source, boolean blocking) throws Exception;

	/**
	 * Transfer the content of this data location to the specified output destination.
	 * @param dest the output destination to transfer to.
	 * @param blocking if true, the method will block until the entire content has been transferred. 
	 * @return the number of bytes actually transferred. 
	 * @throws Exception if an IO error occurs.
	 */
	int transferTo(OutputDestination dest, boolean blocking) throws Exception;

	/**
	 * Transfer the content of this data location to the specified channel.
	 * @param dest the channel to transfer to.
	 * @param blocking if true, the method will block until the entire content has been transferred. 
	 * @return the number of bytes actually transferred. 
	 * @throws Exception if an IO error occurs.
	 */
	int transferTo(WritableByteChannel dest, boolean blocking) throws Exception;
}
