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
 * Simple class representing a person.<br>
 * Note that it is not serializable.
 * @author Laurent Cohen
 */
public class Person
{
	/**
	 * The person's first name.
	 */
	private String firstname;
	/**
	 * The person's last name.
	 */
  private String lastname;
	/**
	 * The person's phone number.
	 */
  private PhoneNumber phone;

  /**
   * Default constructor.
   */
  public Person()
  {
  }

  /**
   * Initialize this person with the specified parameters.
   * @param firstname the person's first name.
   * @param lastname the person's last name.
   * @param phone the person's phone number.
   */
  public Person(String firstname, String lastname, PhoneNumber phone)
  {
  	this.firstname = firstname;
  	this.lastname = lastname;
  	this.phone = phone;
  }

	/**
	 * Get the person's first name.
	 * @return the first name as a string.
	 */
	public String getFirstname()
	{
		return firstname;
	}

	/**
	 * Set the person's first name.
	 * @param firstname the first name as a string.
	 */
	public void setFirstname(String firstname)
	{
		this.firstname = firstname;
	}

	/**
	 * Get the person's last name.
	 * @return the last name as a streing.
	 */
	public String getLastname()
	{
		return lastname;
	}

	/**
	 * Set the person's last name.
	 * @param lastname the last name as a streing.
	 */
	public void setLastname(String lastname)
	{
		this.lastname = lastname;
	}

	/**
	 * Get the person's phone number.
	 * @return a <code>PhoneNumber</code> instance.
	 */
	public PhoneNumber getPhone()
	{
		return phone;
	}

	/**
	 * Set the person's phone number.
	 * @param phone a <code>PhoneNumber</code> instance.
	 */
	public void setPhone(PhoneNumber phone)
	{
		this.phone = phone;
	}

	/**
	 * Get a string representation of this person.
	 * @return this person as a string.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return
			"first name = " + firstname +
			", last name = " + lastname +
			", phone number = " + phone;
	}
}
