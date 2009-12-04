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

package org.jppf.management;

import static org.jppf.server.protocol.BundleParameter.*;

import java.util.*;

import javax.crypto.SecretKey;

import org.jppf.security.CryptoUtils;
import org.jppf.server.JPPFStats;
import org.jppf.server.protocol.BundleParameter;

/**
 * Node-specific connection wrapper, implementing a user-friendly interface for the monitoring
 * and management of the node.
 * @author Laurent Cohen
 */
public class JMXDriverConnectionWrapper extends JMXConnectionWrapper implements JPPFDriverAdminMBean
{
	/**
	 * Signature of the method invoked on the MBean.
	 */
	public static final String[] MBEAN_SIGNATURE = new String[] {Map.class.getName()};

	/**
	 * Initialize the connection to the remote MBean server.
	 * @param host the host the server is running on.
	 * @param port the RMI port used by the server.
	 */
	public JMXDriverConnectionWrapper(String host, int port)
	{
		super(host, port, JPPFAdminMBean.DRIVER_SUFFIX);
	}

	/**
	 * Request the JMX connection information for all the nodes attached to the server.
	 * @return a collection of <code>NodeManagementInfo</code> instances.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.management.JPPFDriverAdminMBean#nodesInformation()
	 */
	public Collection<NodeManagementInfo> nodesInformation() throws Exception
	{
		Map<BundleParameter, Object> params = new HashMap<BundleParameter, Object>();
		params.put(BundleParameter.COMMAND_PARAM, BundleParameter.REFRESH_NODE_INFO);
		Collection<NodeManagementInfo> nodeList =
			(Collection<NodeManagementInfo>) processManagementRequest(params);
		return nodeList;
	}

	/**
	 * Get the latest statistics snapshot from the JPPF driver.
	 * @return a <code>JPPFStats</code> instance.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.management.JPPFDriverAdminMBean#statistics()
	 */
	public JPPFStats statistics() throws Exception
	{
		Map<BundleParameter, Object> params = new EnumMap<BundleParameter, Object>(BundleParameter.class);
		params.put(COMMAND_PARAM, READ_STATISTICS);
    return (JPPFStats) processManagementRequest(params);
	}

	/**
	 * Process a management or monitoring request.
	 * @param parameters the parameters of the request to process
	 * @return the result of the request.
	 * @throws Exception if an error occurred while performing the request.
	 */
	public Object processManagementRequest(Map<BundleParameter, Object> parameters) throws Exception
	{
		if (!READ_STATISTICS.equals(parameters.get(COMMAND_PARAM)) &&
				!REFRESH_NODE_INFO.equals(parameters.get(COMMAND_PARAM)))
		{
			String password = (String) parameters.get(PASSWORD_PARAM);
			SecretKey tmpKey = CryptoUtils.generateSecretKey();
			parameters.put(KEY_PARAM, CryptoUtils.encrypt(tmpKey.getEncoded()));
			parameters.put(PASSWORD_PARAM, CryptoUtils.encrypt(tmpKey, password.getBytes()));
			String newPassword = (String) parameters.get(NEW_PASSWORD_PARAM);
			if (newPassword != null)
			{
				parameters.put(NEW_PASSWORD_PARAM, CryptoUtils.encrypt(tmpKey, newPassword.getBytes()));
			}
		}
		JPPFManagementRequest<BundleParameter, Object> request =
			new JPPFManagementRequest<BundleParameter, Object>(parameters);
		JPPFManagementResponse response = (JPPFManagementResponse)
			invoke(JPPFAdminMBean.DRIVER_MBEAN_NAME, "processManagementRequest", new Object[] {parameters}, MBEAN_SIGNATURE);
		if (response == null) return null;
		if (response.getException() == null) return response.getResult();
		throw response.getException();
	}

}
