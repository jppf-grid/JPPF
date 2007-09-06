/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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

package org.jppf.management;

import java.io.Serializable;

/**
 * Management interface for the administration of a JPPF component, driver or node.
 * @param <T> the type of the parameters keys.
 * @param <U> the type of the parameters values.
 * @author Laurent Cohen
 */
public interface JPPFAdminMBean<T, U> extends Serializable
{
	/**
	 * Name of the node's admin MBean.
	 */
	String NODE_MBEAN_NAME = "org.jppf:name=admin,type=node";
	/**
	 * Name of the node's admin MBean.
	 */
	String DRIVER_MBEAN_NAME = "org.jppf:name=admin,type=driver";
	/**
	 * Perform an administration request specified by its parameters.
	 * @param request an object specifying the request parameters.
	 * @return a <code>JPPFManagementResponse</code> instance.
	 */
	JPPFManagementResponse performAdminRequest(JPPFManagementRequest<T, U> request);
}
