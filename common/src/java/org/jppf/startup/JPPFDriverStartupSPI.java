/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.startup;

/**
 * This interface class represents objects that are run at server startup time.
 * <p>More precisely, they are run just after the server MBeans have been registered.
 * <p>Classes implementing this interface must have a public no-arg constructor and implement the {@link java.lang.Runnable#run() run()} method.
 * <p>Server startup classes allow a developer to perform initializations at runtime, such as load specific APIs,
 * create connection pools, subscribe to the monitoring MBeans notifications, etc. The range of applications is quite broad.
 * <p>They are looked up by using the Service Provider Interface (SPI) lookup mechanism.
 * For the SPI to find them, proceed as follows:
 * <ul>
 * <li>in the classpath root create, if it does not exist, a folder named META-INF/services</li>
 * <li>in this folder create a file named &quot;org.jppf.startup.JPPFDriverStartupSPI&quot;</li>
 * <li>the content of this file is a line with the fully qualified class name of this interface's implementation</li>
 * <li>to specifiy multiple implementations, just put one per line in the file</li>
 * <li>there can also be multiple META-INF/services/org.jppf.startup.JPPFDriverStartupSPI resources in the classpath
 * (for instance in multiple jar files); all of them will be looked up and processed</li>
 * </ul>
 * @author Laurent Cohen
 */
public interface JPPFDriverStartupSPI extends JPPFStartup
{
}
