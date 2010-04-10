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
package org.jppf.example.database.client;

import java.io.*;
import java.sql.*;

import javax.sql.DataSource;

import org.jppf.example.database.node.NodeDatabaseStartup;
import org.jppf.server.protocol.JPPFTask;

/**
 * This class is a JPPF task.
 * @author Laurent Cohen
 */
public class DatabaseTask extends JPPFTask
{
	/**
	 * Perform initializations on the client side,
	 * before the task is executed by the node.
	 */
	public DatabaseTask()
	{
	}

	/**
	 * This method contains the code that will be executed by a node.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		System.out.println("Starting database task");
		try
		{
			NodeDatabaseStartup instance = NodeDatabaseStartup.getInstance();
			System.out.println("instance = " + instance);
			System.out.println("instance class loader = " + instance.getClass().getClassLoader());
			DataSource ds = NodeDatabaseStartup.getInstance().getDatasource();
			Connection connection = ds.getConnection();
			PreparedStatement stmt = connection.prepareStatement("SELECT * FROM LINKS_GROUPS WHERE GROUP_ID = 1");
			ResultSet rs = stmt.executeQuery();
			rs.next();
			String s = rs.getString("DESC");
			rs.close();
			stmt.close();
			setResult("Group 1 description: " + s);
		}
		catch(Exception e)
		{
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			setResult(sw.toString());
		}
	}
}
