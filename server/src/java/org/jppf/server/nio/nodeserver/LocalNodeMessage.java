/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.server.nio.nodeserver;

import java.io.InputStream;

import org.jppf.data.transform.JPPFDataTransformFactory;
import org.jppf.server.nio.ChannelWrapper;
import org.jppf.server.protocol.JPPFTaskBundle;
import org.jppf.utils.*;

/**
 * Node message implementation for an in-VM node.
 * @author Laurent Cohen
 */
public class LocalNodeMessage extends AbstractNodeMessage
{
	/**
	 * {@inheritDoc}
	 */
	public boolean read(ChannelWrapper<?> wrapper) throws Exception
	{
		//while (locations.isEmpty()) ((LocalNodeWrapperHandler) wrapper).goToSleep();
		InputStream is = locations.get(0).getInputStream();
		byte[] data = FileUtils.getInputStreamAsByte(is);
		data = JPPFDataTransformFactory.transform(false, data, 0, data.length);
		SerializationHelper helper = new SerializationHelperImpl();
		bundle = (JPPFTaskBundle) helper.getSerializer().deserialize(data);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	protected synchronized boolean readNextObject(ChannelWrapper<?> wrapper) throws Exception
	{
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean write(ChannelWrapper<?> wrapper) throws Exception
	{
		//((LocalNodeWrapperHandler) wrapper).wakeUp();
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	protected boolean writeNextObject(ChannelWrapper<?> wrapper) throws Exception
	{
		return true;
	}
}
