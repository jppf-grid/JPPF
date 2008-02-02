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

package org.jppf.server.protocol.commandline;

import java.io.Serializable;

/**
 * Instances of this class represent an artifact, such as a file or url, that is used or generated
 * by the program or script executed by a coomand-line task.
 * <p>The use of the local and remote locations is as follows:
 * <ul>
 * <li>for an input artifact: the artifact identified by the local location will be read, on the client side,
 * into the content field, and written to the remote location on the node side</li>
 * <li>for an output artifact: the artifact identified by the remote location will be read, on the node side,
 * into the content field, and written to the local location on the client side</li>
 * </ul>
 * @author Laurent Cohen
 */
public class ExternalArtifact implements Serializable
{
	/**
	 * The location of this artifact on the client side.
	 */
	private Location localLocation = null;
	/**
	 * The location of this artifact on the node side.
	 */
	private Location remoteLocation = null;
	/**
	 * Holds the actual content of the artifact.
	 */
	private byte[] content = null;

	/**
	 * Initialize this artifac with the specified locations.
	 * @param localLocation the location of this artifact on the client side, may be null.
	 * @param remoteLocation the location of this artifact on the node side, may be null.
	 */
	public ExternalArtifact(Location localLocation, Location remoteLocation)
	{
		this.localLocation = localLocation;
		this.remoteLocation = remoteLocation;
	}

	/**
	 * Get the location of this artifact on the client side.
	 * @return a <code>Location</code> instance.
	 */
	public Location getLocalLocation()
	{
		return localLocation;
	}

	/**
	 * Get the location of this artifact on the node side.
	 * @return a <code>Location</code> instance.
	 */
	public Location getRemoteLocation()
	{
		return remoteLocation;
	}

	/**
	 * Get the content of this artifact.
	 * @return the content as an array of bytes.
	 */
	public byte[] getContent()
	{
		return content;
	}

	/**
	 * Set the content of this artifact.
	 * @param content the content as an array of bytes.
	 */
	public void setContent(byte[] content)
	{
		this.content = content;
	}
}
