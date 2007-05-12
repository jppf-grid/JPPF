/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package sample.test;

//import java.sql.*;

/**
 * Test task to check that correct results are returned by the framework.
 * @author Laurent Cohen
 */
public class DB2LoadingTask extends JPPFTestTask
{
	/**
	 * Initialize this task.
	 */
	public DB2LoadingTask()
	{
	}

	/**
	 * Execute the task
	 * @throws Exception .
	 * @see java.lang.Runnable#run()
	 */
	public void test() throws Exception
	{
		Class<?> c = Class.forName("com.ibm.db2.jcc.DB2Driver");
		//c.newInstance();
		//Driver driver = new com.ibm.db2.jcc.DB2Driver();
		//Connection conn = DriverManager.getConnection("", null);
	}
}
