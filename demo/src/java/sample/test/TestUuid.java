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

package sample.test;

import java.util.Random;

import org.jppf.utils.JPPFUuid;

/**
 * Test different uuid generation algorithms.
 * @author Laurent Cohen
 */
public class TestUuid
{
	/**
	 * Set of characters used to compose a uuid.
	 */
	private static final String[] ALPHABET =
	{ "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l",
		"m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H",
		"I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "'", "!", "@", "#",
		"$", "%", "^", "&", "*", "(", ")", "_", "+", "|", "{", "}", "[", "]", "-", "=", "/", ",", ".", "?", ":", ";"};
	/**
	 * Pseudo-random number generator.
	 */
	private static Random rand = new Random(System.currentTimeMillis());

	/**
	 * Entry point for running this test.
	 * @param args not used.
	 */
	public static void main(final String...args)
	{
		try
		{
			int nbIter = 1000 * 1000;
			long elapsed_1 = 0L;
			long elapsed_2 = 0L;
			TestUuid tu = new TestUuid();
			long start = System.currentTimeMillis();
			for (int i=0; i<nbIter; i++)
			{
				String s = tu.generate1();
			}
			elapsed_1 = System.currentTimeMillis() - start;
			System.out.println("Test 1 : " + elapsed_1 + " ms");
			start = System.currentTimeMillis();
			for (int i=0; i<nbIter; i++)
			{
				String s = tu.generate2();
			}
			elapsed_2 = System.currentTimeMillis() - start;
			System.out.println("Test 2 : " + elapsed_2 + " ms");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		System.exit(0);
	}

	/**
	 * Generate a uuid using the JPPFUuid class.
	 * @return a uuid represented as a string.
	 */
	public String generate1()
	{
		return new JPPFUuid().toString();
	}

	/**
	 * Generate a shorter uuid using a different algorithm.
	 * @return a uuid represented as a string.
	 */
	public String generate2()
	{
		int len = ALPHABET.length;
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<16; i++) sb.append(ALPHABET[rand.nextInt(len)]);
		return sb.toString();
	}
}
