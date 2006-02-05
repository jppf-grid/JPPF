/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package sample.matrix;

import org.jppf.server.protocol.JPPFTask;

/**
 * Thnis task is intended for testing the framework only.
 * @author Laurent Cohen
 */
public class JPPFTestTask extends JPPFTask
{
	static
	{
		System.out.println("JPPFTestTask loaded by "+JPPFTestTask.class.getClassLoader());
	}
	
	/**
	 * Dummy data for serialization test.
	 */
	protected String someString = "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999";

	/**
	 * Initialize this task with a specified row of values to multiply.
	 */
	public JPPFTestTask()
	{
	}
	
	/**
	 * Run the test.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		//run2();
		System.out.println("Task Executing");
		System.exit(0);
	}
	//public void run2() { System.out.println("Task 2.0 Executing"); }
}
