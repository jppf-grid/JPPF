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
		Class.forName("com.ibm.db2.jcc.DB2Driver");
		//c.newInstance();
		//Driver driver = new com.ibm.db2.jcc.DB2Driver();
		//Connection conn = DriverManager.getConnection("", null);
	}
}
