/*
 * JPPF.
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
package org.jppf.ui.monitoring.data;

import org.jppf.management.*;

/**
 * Convenience class used to group a node state and corresponding JMX server.
 * @author Laurent Cohen
 */
public class NodeInfoHolder
{
	/**
	 * Holds state information about the node.
	 */
	private JPPFNodeState state = null;
	/**
	 * The JMX client for the node.
	 */
	private JMXNodeConnectionWrapper jmxClient = null;
	/**
	 * Determines whether the automatic refresh for the node is active. 
	 */
	private boolean active = false;

	/**
	 * Initialize this node information holder with the specified node state and jmx client.
	 * @param state holds state information about the node.
	 * @param jmxClient the JMX client for the node.
	 */
	public NodeInfoHolder(JPPFNodeState state, JMXNodeConnectionWrapper jmxClient)
	{
		this.state = state;
		this.jmxClient = jmxClient;
	}

	/**
	 * Get the state information about the node.
	 * @return a <code>JPPFNodeState</code> instance.
	 */
	public JPPFNodeState getState()
	{
		return state;
	}

	/**
	 * Set the state information about the node.
	 * @param state a <code>JPPFNodeState</code> instance.
	 */
	public void setState(JPPFNodeState state)
	{
		this.state = state;
	}

	/**
	 * Get the JMX client for the node.
	 * @return a <code>JMXConnectionWrapper</code> instance.
	 */
	public JMXNodeConnectionWrapper getJmxClient()
	{
		return jmxClient;
	}

	/**
	 * Get the state of the automatic refresh for the node is active. 
	 * @return true if automatic refresh is active, false otherwise.
	 */
	public boolean isActive()
	{
		return active;
	}

	/**
	 * Set the state of the automatic refresh for the node is active. 
	 * @param active true if automatic refresh is active, false otherwise.
	 */
	public void setActive(boolean active)
	{
		this.active = active;
	}

	/**
	 * Get a string reprensation of this object.
	 * @return a string displaying the host and port of the underlying jmx connection.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return jmxClient.getHost() + ":" + jmxClient.getPort();
	}
}
