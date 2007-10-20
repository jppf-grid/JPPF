/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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

package org.jppf.jca.demo;

import java.io.Serializable;
import java.util.*;

import javax.naming.InitialContext;
import javax.resource.cci.ConnectionFactory;
import javax.rmi.PortableRemoteObject;

import org.jppf.jca.cci.JPPFConnection;
import org.jppf.server.protocol.JPPFTask;

/**
 * Instances of this class encapsulate a simple call to the JPPF resource adapter.
 * @author Laurent Cohen
 */
public class DemoTest implements Serializable
{
	/**
	 * JNDI name of the JPPFConnectionFactory.
	 */
	private String jndiBinding = null;
	/**
	 * Reference ot the initial context.
	 */
	private InitialContext ctx = null;

	/**
	 * Initialize this test object with a specified jndi location for the connection factory.
	 * @param jndiBinding JNDI name of the JPPFConnectionFactory.
	 */
	public DemoTest(String jndiBinding)
	{
		this.jndiBinding = jndiBinding;
	}

	/**
	 * Perform a simple call to the JPPF resource adapter.
	 * @return a string reporting either the task execution result or an error message.
	 * @throws Exception if the call to JPPF failed.
	 */
	public String testConnector() throws Exception
	{
		JPPFConnection connection = null;
		String msg = null;
		try
		{
			connection = getConnection();
			JPPFTask task = new DemoTask();
			List list = new ArrayList();
			list.add(task);
			list = connection.submit(list, null);
			task = (JPPFTask) list.get(0);
			if (task.getException() != null) msg = task.getException().getMessage();
			else msg = (String) task.getResult();
		}
		finally
		{
			if (connection != null) connection.close();
		}
		return msg;
	}

	/**
	 * Get the initial context.
	 * @return an <code>InitialContext</code> instance.
	 * @throws Exception if the context could not be obtained.
	 */
	public InitialContext getInitialContext() throws Exception
	{
		if (ctx == null) ctx = new InitialContext();
		return ctx;
	}

	/**
	 * Obtain a JPPF connection from the resource adaper's connection pool.
	 * The obtained connection must be closed by the caller of this method, once it is done using it.
	 * @return a <code>JPPFConnection</code> instance.
	 * @throws Exception if the connection could not be obtained.
	 */
	public JPPFConnection getConnection() throws Exception
	{
		Object objref = getInitialContext().lookup(jndiBinding);
		ConnectionFactory cf = (ConnectionFactory) PortableRemoteObject.narrow(objref, ConnectionFactory.class);
		return (JPPFConnection) cf.getConnection();
	}
}
