/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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

import org.jppf.server.protocol.JPPFTask;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

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
		this.person = person;
		XStream xstream = new XStream(new DomDriver());
		personXml = xstream.toXML(person);
	}

	/**
	 * Run this task.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		XStream xstream = new XStream(new DomDriver());
		person = (Person) xstream.fromXML(personXml);
		String s = person.toString();
		System.out.println("deserialized this person: " + s);
		setResult(s);
	}
}
