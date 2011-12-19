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
package test.node.nativelib;

import org.jppf.JPPFException;
import org.jppf.node.NodeRunner;
import org.jppf.server.protocol.JPPFTask;

/**
 * This class is a template for a standard JPPF task.
 * @author Laurent Cohen
 */
public class NativeLibTask extends JPPFTask
{
	/**
	 * Perform initializations on the client side,
	 * before the task is executed by the node.
	 */
	public NativeLibTask()
	{
	}

	/**
	 * This method contains the code that will be executed by a node.
	 */
	public void run()
	{
		try
		{
			String path = System.getProperty("java.library.path");
			System.out.println("java.library.path = " + path);
			System.setProperty("java.library.path", path + System.getProperty("path.separator") + "C:/temp");
			//getClass().getClassLoader().loadClass("test.node.nativelib.NativeLibLoader");
			Boolean b = (Boolean) NodeRunner.getPersistentData("libLoaded");
			if (b == null)
			{
  	    System.out.println("before loading the library");
        //System.loadLibrary("eclipse_1406");
        System.loadLibrary("eclipse_1312");
        NodeRunner.setPersistentData("libLoaded", Boolean.TRUE);
  	    System.out.println("after loading the library");
			}
			else System.out.println("library already loaded");
      setResult("the execution was performed successfully");
		}
    catch(Exception e)
    {
      setException(e);
      e.printStackTrace();
    }
    catch(Error e)
    {
      e.printStackTrace();
      setException(new JPPFException(e));
    }
	}
}
