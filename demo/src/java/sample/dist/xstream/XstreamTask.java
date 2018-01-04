/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import java.lang.reflect.*;

import org.jppf.node.protocol.AbstractTask;

/**
 * Sample task using XStream to serialize/deserialize objects.
 * @author Laurent Cohen
 */
public class XstreamTask extends AbstractTask<String> {
  /**
   * Person object to serialize with xstream. Note that it must be declared as transient.
   */
  private transient Person person = null;
  /**
   * Xml representation of the Person object to deserialize with xstream.
   */
  private String personXml = null;

  /**
   * Initialize this task with the specified person.
   * @param person a <code>Person</code> instance.
   */
  public XstreamTask(final Person person) {
    try {
      this.person = person;
      final Object xstream = instantiateXStream();
      final Method m = xstream.getClass().getDeclaredMethod("toXML", Object.class);
      this.personXml = (String) m.invoke(xstream, person);
    } catch (final Exception e) {
      setThrowable(e);
    }
  }

  /**
   * Run this task.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    try {
      final Object xstream = instantiateXStream();
      final Method m = xstream.getClass().getDeclaredMethod("fromXML", String.class);
      this.person = (Person) m.invoke(xstream, personXml);
      final String s = this.person.toString();
      System.out.println("deserialized this person: " + s);
      setResult(s);
    } catch (final Exception e) {
      setThrowable(e);
    }
  }

  /**
   * Instantiates an <code>XStream</code> instance through reflection.
   * This avoids compile errors if the XStream jars are not in the classpath.
   * @return an XStream object.
   * @throws Exception if an instantiation error occurs or the required classes are not in the classpath.
   */
  private static Object instantiateXStream() throws Exception {
    final Class<?> xstreamClass = Class.forName("com.thoughtworks.xstream.XStream");
    final Class<?> hierarchicalStreamDriverClass = Class.forName("com.thoughtworks.xstream.io.HierarchicalStreamDriver");
    final Constructor<?> c = xstreamClass.getConstructor(hierarchicalStreamDriverClass);
    final Class<?> domDriverClass = Class.forName("com.thoughtworks.xstream.io.xml.DomDriver");
    final Object driver = domDriverClass.newInstance();
    return c.newInstance(driver);
  }
}
