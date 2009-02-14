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

package sample.xstream;

import java.lang.reflect.*;

import org.jppf.server.protocol.JPPFTask;

/**
 * Sample task using XStream to serialize/deserialize objects.
 * @author Laurent Cohen
 */
public class XstreamTask extends JPPFTask
{
	/**
	 * Person object to serialize with xstream. Note that it must be declared as transient.
	 */
	private transient Person person = null;
	/**
	 * Xml representation of the Person object to deserialize with xstream.
	 */
	private String personXml = null;

	/**
	 * Intiialize this task with the specified person.
	 * @param person a <code>Person</code> instance.
	 */
	public XstreamTask(Person person)
	{
		try
		{
			this.person = person;
			Object xstream = instantiateXStream();
			Method m = xstream.getClass().getDeclaredMethod("toXML", Object.class);
			this.personXml = (String) m.invoke(xstream, person);
		}
		catch(Exception e)
		{
			setException(e);
		}
	}

	/**
	 * Run this task.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			Object xstream = instantiateXStream();
			Method m = xstream.getClass().getDeclaredMethod("fromXML", String.class);
			this.person = (Person) m.invoke(xstream, personXml);
			String s = this.person.toString();
			System.out.println("deserialized this person: " + s);
			setResult(s);
		}
		catch(Exception e)
		{
			setException(e);
		}
	}

	/**
	 * Instantiates an <code>XStream</code> instance through reflection.
	 * This avoids compile errors if the XStream jars are not in the classpath.
	 * @return an XStream object.
	 * @throws Exception if an insantiation error occurs or the required classes are not in the classpath.
	 */
	private Object instantiateXStream() throws Exception
	{
		Class xstreamClass = Class.forName("com.thoughtworks.xstream.XStream");
		Class hierarchicalStreamDriverClass = Class.forName("com.thoughtworks.xstream.io.HierarchicalStreamDriver");
		Constructor c = xstreamClass.getConstructor(hierarchicalStreamDriverClass);
		Class domDriverClass = Class.forName("com.thoughtworks.xstream.io.xml.DomDriver");
		Object driver = domDriverClass.newInstance();
		return c.newInstance(driver);
	}
}
