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

package test.client.nissalia;

import org.jppf.server.protocol.JPPFTask;

public class TestMatrixTask2 extends JPPFTask
{

	private double[][] mA;

	private int result;

	public TestMatrixTask2(int i)
	{
		result = i;
	}

	void init(int tailleMatrix)
	{
		mA = new double[tailleMatrix][tailleMatrix];

		for (int i = 0; i < tailleMatrix; i++)
		{
			for (int j = 0; j < tailleMatrix; j++)
			{
				mA[i][j] = Math.random();
			}
		}
	}

	public Object getResult()
	{
		return result;
	}

	public void run()
	{
		fireNotification("start exec");
		try
		{
			doWork();
		}
		finally
		{
			fireNotification("end exec");
		}
	}

	public void doWork()
	{
		try
		{
			System.out.println("start " + result);
			init(1500);
			System.out.println("end " + result);
		}
		catch (Exception e)
		{
			setException(e);
		}
	}
}
