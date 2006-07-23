/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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

import org.jppf.server.protocol.JPPFTask;

/**
 * Test task to test that the framework behaves correctly when a class is not found
 * by the classloader.
 * @author Laurent Cohen
 */
public class ClassNotFoundTestTask extends JPPFTask
{
	/**
	 * Initialize this task.
	 */
	public ClassNotFoundTestTask()
	{
	}

	/**
	 * Execute the task.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		new org.ujac.ui.editor.TextArea();
		String s = "Please make sure the library 'ujac-ui.jar' is NOT present in the node, server or client classpath";
		setResult(s);
		System.out.println(s);
	}
}
