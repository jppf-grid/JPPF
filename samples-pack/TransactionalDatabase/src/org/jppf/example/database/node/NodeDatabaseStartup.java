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

package org.jppf.example.database.node;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.jppf.startup.JPPFNodeStartupSPI;
import org.jppf.utils.*;

/**
 * This is a test of a node startup class.
 * @author Laurent Cohen
 */
public class NodeDatabaseStartup implements JPPFNodeStartupSPI
{
	/**
	 * Singleton instance of this class.
	 */
	private static NodeDatabaseStartup instance;
	//private static NodeDatabaseStartup instance = new NodeDatabaseStartup();
	/**
	 * The database connection pool.
	 */
	private DataSource datasource = null;

	/**
	 * Default constructor
	 */
	public NodeDatabaseStartup()
	{
	}

	/**
	 * This is a test of a node startup class.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		System.out.println("Initializing data source");
		try
		{
			System.out.println("instance = " + this);
			System.out.println("class loader = " + getClass().getClassLoader());
			instance = this;
			datasource = initDatasource();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Initialize he data source.
	 * @return a JDBC data source.
	 * @throws Exception if any error occurs. 
	 */
	private DataSource initDatasource() throws Exception
	{
		BasicDataSource basicDataSource = new BasicDataSource();
		TypedProperties config = JPPFConfiguration.getProperties();
		int size = config.getInt("processing.threads");
		System.out.println("pool size = " + size);
    basicDataSource.setInitialSize(size);
    basicDataSource.setMaxActive(size);
    basicDataSource.setMaxIdle(size);
    basicDataSource.setDriverClassName("com.mysql.jdbc.Driver");
    basicDataSource.setUsername("user");
    basicDataSource.setPassword("user");
    basicDataSource.setUrl("jdbc:mysql://localhost:3306/test");
    basicDataSource.setValidationQuery("select 1");
    return basicDataSource;
	}

	/**
	 * Get the datasource held by this startup class.
	 * @return a JDBC data source.
	 */
	public DataSource getDatasource()
	{
		return datasource;
	}

	/**
	 * Get the singleton instance for this class.
	 * @return a {@link NodeDatabaseStartup} instance.
	 */
	public static NodeDatabaseStartup getInstance()
	{
		return instance;
	}
}
