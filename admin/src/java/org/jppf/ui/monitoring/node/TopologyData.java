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

package org.jppf.ui.monitoring.node;

import org.apache.commons.logging.*;
import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.utils.NetworkUtils;

/**
 * Instances of this class represent the state of a node in the Yopology panel tree.
 * @author Laurent Cohen
 */
public class TopologyData
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(TopologyData.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The type of this object: driver or node.
	 */
	private TopologyDataType type = null;
	/**
	 * The status of the node.
	 */
	private TopologyDataStatus status = TopologyDataStatus.UP;
	/**
	 * A driver connection.
	 */
	private JPPFClientConnection clientConnection = null;
	/**
	 * Wrapper holding the connection to the JMX server on a driver or a node. 
	 */
	private JMXConnectionWrapper jmxWrapper = null;
	/**
	 * Information on the JPPF node .
	 */
	private JPPFManagementInfo nodeInformation = null;
	/**
	 * Object describing the current state of a node.
	 */
	private JPPFNodeState nodeState = new JPPFNodeState();

	/**
	 * Initialize topology job data with the specified type.
	 * @param type - the type of this job data object as a <code>JobDataType</code> enum value.
	 */
	protected TopologyData(TopologyDataType type)
	{
		this.type = type;
	}

	/**
	 * Initialize this topology data as a driver related object.
	 * @param clientConnection - a reference to the driver connection.
	 */
	public TopologyData(JPPFClientConnection clientConnection)
	{
		this(TopologyDataType.DRIVER);
		this.clientConnection = clientConnection;
		this.jmxWrapper = ((JPPFClientConnectionImpl) clientConnection).getJmxConnection();
	}

	/**
	 * Initialize this topology data as holding information about a node.
	 * @param nodeInformation - information on the JPPF node.
	 */
	public TopologyData(JPPFManagementInfo nodeInformation)
	{
		this(TopologyDataType.NODE);
		this.nodeInformation = nodeInformation;
		String host = NetworkUtils.getHostName(nodeInformation.getHost());
		jmxWrapper = new JMXNodeConnectionWrapper(host, nodeInformation.getPort());
		jmxWrapper.connect();
	}

	/**
	 * Get the type of this job data object.
	 * @return a <code>TopologyDataType</code> enum value.
	 */
	public TopologyDataType getType()
	{
		return type;
	}

	/**
	 * Get the wrapper holding the connection to the JMX server on a driver or node. 
	 * @return a <code>JMXDriverConnectionWrapper</code> instance.
	 */
	public JMXConnectionWrapper getJmxWrapper()
	{
		return jmxWrapper;
	}

	/**
	 * Get the information on a JPPF node.
	 * @return a <code>NodeManagementInfo</code> instance.
	 */
	public JPPFManagementInfo getNodeInformation()
	{
		return nodeInformation;
	}

	/**
	 * Get a string reprensation of this object.
	 * @return a string displaying the host and port of the underlying jmx connection.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return jmxWrapper.getId();
	}

	/**
	 * Get the object describing the current state of a node.
	 * @return a <code>JPPFNodeState</code> instance.
	 */
	public JPPFNodeState getNodeState()
	{
		return nodeState;
	}

	/**
	 * Set the object describing the current state of a node.
	 * @param nodeState - a <code>JPPFNodeState</code> instance.
	 */
	public void setNodeState(JPPFNodeState nodeState)
	{
		this.nodeState = nodeState;
	}

	/**
	 * Get the driver connection.
	 * @return a <code>JPPFClientConnection</code> instance.
	 */
	public JPPFClientConnection getClientConnection()
	{
		return clientConnection;
	}

	/**
	 * Refresh the state of the node represented by this topology data.
	 */
	public void refreshNodeState()
	{
		if (!TopologyDataType.NODE.equals(type)) return;
		try
		{
			if (!jmxWrapper.isConnected()) return;
			nodeState = ((JMXNodeConnectionWrapper) jmxWrapper).state();
			if (nodeState == null) setStatus(TopologyDataStatus.DOWN);
		}
		catch(Exception e)
		{
			setStatus(TopologyDataStatus.DOWN);
			if (debugEnabled) log.debug(e.getMessage(), e);
		}
	}

	/**
	 * Get the status of the node.
	 * @return the node status.
	 */
	public TopologyDataStatus getStatus()
	{
		return status;
	}

	/**
	 * Set the status of the node.
	 * @param status the node status.
	 */
	public void setStatus(TopologyDataStatus status)
	{
		this.status = status;
	}
}
