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

package org.jppf.management.spi;


/**
 * Service provider interface for pluggable management beans.
 * @author Laurent Cohen
 */
public interface JPPFMBeanProvider
{
	/**
	 * Return the fully qualified name of the management interface defined by this provider.
	 * @return the fully qualified interface name as a string.
	 */
	String getMBeanInterfaceName();
	/**
	 * Return the name of the specified MBean.<br>
	 * This is the name under which the MBean will be registered with the MBean server.
	 * It must be a valid object name, as specified in the documentation for {@link javax.management.ObjectName ObjectName}.
	 * @return the MBean name for this MBean provider.
	 */
	String getMBeanName();
}
