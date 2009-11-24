/*
 * Java Parallel Processing Framework.
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
