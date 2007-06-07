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

import org.jppf.server.protocol.JPPFTask;

/**
 * 
 * @author Laurent Cohen
 */
public class MyTask extends JPPFTask
{
	/**
	 * serialVersionUID.
	 */
	private static final long serialVersionUID = 7898318895418777681L;

	/**
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		String str = null;

		try
		{
			str = ((SimpleData) getDataProvider().getValue("DATA")).getStr();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		setResult(str);
	}
}
