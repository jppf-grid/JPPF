/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

package test.serialization;

/**
 * Class used to test the JPPFObjectOutputStream.
 * @author Laurent Cohen
 */
public class TestA
{
	/**
	 * Test field.
	 */
	private int n = 0;

	/**
	 * Get the test field.
	 * @return an int. 
	 */
	public int getN()
	{
		return n;
	}

	/**
	 * Set the test field.
	 * @param n an int.
	 */
	public void setN(int n)
	{
		this.n = n;
	}
}
