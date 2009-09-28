/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2009 JPPF Team.
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

package sample.dist.xstream;

/**
 * Simple class representing a US phone number.
 * @author Laurent Cohen
 */
public class PhoneNumber
{
	/**
	 * Area code.
	 */
	private int code;
  /**
   * 7 digits number.
   */
  private String number;

  /**
   * Default constructor.
   */
  public PhoneNumber()
  {
  }
 
  /**
   * Initialize this object with the specified parameters.
   * @param code the area code.
   * @param number a 7 digits number.
   */
  public PhoneNumber(int code, String number)
  {
  	this.code = code;
  	this.number = number;
  }

	/**
	 * Get the area code.
	 * @return the area code as an int.
	 */
	public int getCode()
	{
		return code;
	}

	/**
	 * Set the area code.
	 * @param code the area code as an int.
	 */
	public void setCode(int code)
	{
		this.code = code;
	}

	/**
   * Get the 7 digits number.
	 * @return the number as a string.
	 */
	public String getNumber()
	{
		return number;
	}

	/**
   * Set the 7 digits number.
	 * @param number the number as a string.
	 */
	public void setNumber(String number)
	{
		this.number = number;
	}

	/**
	 * Get a string representation of this phone number.
	 * @return this phone number as a string.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return "(" + code + ") " + number;
	}
}
