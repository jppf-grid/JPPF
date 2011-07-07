/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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
package test.localexecution;

import java.sql.*;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.jppf.server.protocol.JPPFTask;

/**
 * Test task.
 * 
 * @author Laurent Cohen
 */
public class JdbcTask extends JPPFTask
{
	/**
	 * Explicit serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * To determine if we must load the jars or not.
	 */
	private static boolean initialized = false;

	/**
	 * {@inheritDoc}
	 */
	public void run()
	{
		Connection conn = null;
		Statement stmt = null;
		ResultSet rset = null;

		try
		{
			System.out.println("starting task");
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("loaded JDBC driver");

			DataSource dataSource = setupDataSource();
			
			System.out.println("set up data source done");
			conn = dataSource.getConnection();
			System.out.println("created connection");
			stmt = conn.createStatement();
			System.out.println("created statement");
			rset = stmt.executeQuery("select * from links_groups");
			System.out.println("executed statement");
			System.out.println("Results:");
			int numcols = rset.getMetaData().getColumnCount();
			while (rset.next())
			{
				for (int i = 1; i <= numcols; i++)
				{
					System.out.print("\t" + rset.getString(i));
				}
				System.out.println("");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			setException(e);
		}
		finally
		{
			try
			{
				if (rset != null) rset.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				if (stmt != null) stmt.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				if (conn != null) conn.close();
			}
			catch (Exception e)
			{
			}
		}
		System.out.println("task done");
	}

	/**
	 * Setup the data source.
	 * @return a DataSource object.
	 */
	public DataSource setupDataSource()
	{
		BasicDataSource ds = new BasicDataSource();
		ds.setUrl("jdbc:mysql://192.168.1.14:3306/pervasiv_jppfweb");
		ds.setUsername("pervasiv_jppfadm");
		ds.setPassword("tri75den");
    return ds;
	}
}
