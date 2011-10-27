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

package sample.prime;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Reresentation of large positive integers.
 * @author Laurent Cohen
 */
public class LargeInt implements Serializable
{
	/**
	 * Explicit serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 2's complement representation of zero.
	 */
	public static final byte[] ZERO = { 0 };
	/**
	 * 2's complement representation of zero.
	 */
	public static final byte[] ONE = { 1 };
	/**
	 * 2's complement representation of zero.
	 */
	public static final byte[] TWO = { 2 };
	/**
	 * 2's complement representation of zero.
	 */
	public static final byte[] FOUR = { 4 };
	/**
	 * 2's complement representation of this number.
	 */
	public byte[] data = null;
	/**
	 * 
	 */
	public int dataLength = 0;

	/**
	 * .
	 * @param data .
	 */
	public LargeInt(final byte[] data)
	{
		this.data = data;
		dataLength = actualLength(data);
	}

	/**
	 * Multiply by an int no larger than 127.
	 * @param other the number to multiply by.
	 */
	public void multiply(final LargeInt other)
	{
		LargeInt result = new LargeInt(ZERO);
		for (int i=0; i<other.dataLength; i++)
		{
			byte[] tempData = multiply(other.data[i], i);
			result.add(new LargeInt(tempData));
		}
	}

	/**
	 * Multiply by an int no larger than 127.
	 * @param operand the number to multiply by.
	 * @param position the position from which to start in the array.
	 * @return the reuslt of the multiplication.
	 */
	private byte[] multiply(final int operand, final int position)
	{
		if (operand == 0) return ZERO;
		if (operand == 1) return data;
		int length = dataLength + position;
		byte[] temp = new byte[length + 1];
		int toReport = 0;
		Arrays.fill(temp, 0, position, (byte) 0);
		for (int i=0; i<dataLength; i++)
		{
			int n = operand * data[i];
			if (n > 127)
			{
				temp[i + position] = (byte) (n % 128);
				toReport = n / 128;
			}
			else
			{
				temp[i + position] = (byte) n;
				toReport = 0;
			}
		}
		return temp;
	}

	/**
	 * Add a LargeInt to this one.
	 * @param other the value to add.
	 */
	public void add(final LargeInt other)
	{
		int n1 = dataLength;
		int n2 = other.dataLength;
		if (n1 > n2) data = add(data, n1, other.data, n2);
		else data = add(other.data, n2, data, n1);
		dataLength = actualLength(data);
	}

	/**
	 * Add the 2's complement representations of 2 large ints.
	 * @param smaller the smallest of the 2 representations.
	 * @param smallerLength the actual length of the smaller array.
	 * @param bigger the largest of the 2 representation.
	 * @param biggerLength the actual length of the bigger array.
	 * @return an array of byte, 2's complement representation of the addition.
	 */
	private byte[] add(final byte[] smaller, final int smallerLength, final byte[] bigger, final int biggerLength)
	{
		byte[] temp = new byte[biggerLength + 1];
		int toReport = 0;
		for (int i=0; i<biggerLength; i++)
		{
			int n = bigger[i] + (i < smallerLength ? smaller[i] : 0) + toReport;
			if (n > 127)
			{
				n -= 128;
				toReport = 1;
			}
			else toReport = 0;
			temp[i] = (byte) n;
		}
		temp[biggerLength] = (toReport > 0) ? (byte) toReport : 0;
		return temp;
	}

	/**
	 * Add a LargeInt to this one.
	 * @param other the value to add.
	 */
	public void subtract(final LargeInt other)
	{
		data = add(data, dataLength, other.data, other.dataLength);
		dataLength = actualLength(data);
	}

	/**
	 * Subtract an integer no larger than 127.
	 * @param n the value to subtract.
	 */
	public void subtract(final int n)
	{
		if (n <= data[0]) data[0] = (byte) (data[0] - n);
		else
		{
			int tmp = 128 * data[1] + data[0] - n;
			data[1] = (byte) (tmp / 128);
			data[0] = (byte) (tmp % 128);
			dataLength = actualLength(data);
		}
	}

	/**
	 * Subtract the 2's complement representations of 2 large ints.
	 * @param smaller the smallest of the 2 representations.
	 * @param smallerLength the actual length of the smaller array.
	 * @param bigger the largest of the 2 representations.
	 * @param biggerLength the actual length of the bigger array.
	 * @return an array of byte, 2's complement representation of the addition.
	 */
	private byte[] subtract(final byte[] smaller, final int smallerLength, final byte[] bigger, final int biggerLength)
	{
		byte[] temp = new byte[biggerLength];
		int toReport = 0;
		for (int i=biggerLength-1; i<=0; i--)
		{

			int n = bigger[i] - (i < smallerLength ? smaller[i] : 0) - toReport;
			if (n < 0)
			{
				n += 128;
				toReport = 1;
			}
			else toReport = 0;
			temp[i] = (byte) n;
		}
		temp[biggerLength] = (toReport > 0) ? (byte) toReport : 0;
		return temp;
	}

	/**
	 * Return the actual length of the specified array, which is the length minus the number of 0 at the end.
	 * @param bytes the array from which to get the length.
	 * @return the length as an int value.
	 */
	private int actualLength(final byte[] bytes)
	{
		int n = bytes.length;
		while ((bytes[n - 1] == 0) && (n > 0)) n--;
		return n;
	}
}
