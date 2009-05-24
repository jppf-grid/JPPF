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

package org.jppf.management.spi;

import java.util.*;

import javax.imageio.spi.ServiceRegistry;
import javax.management.*;

import org.apache.commons.logging.*;

/**
 * Instances of this class manage all management plugins defined through the Service Provider Interface.
 * @author Laurent Cohen
 */
public class JPPFMBeanProviderManager
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFMBeanProviderManager.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The service registry for all mbean providers.
	 */
	private ServiceRegistry registry = null;

	/**
	 * Initialize this mbean provider manager.
	 */
	public JPPFMBeanProviderManager()
	{
		List<Class<?>> categories = new ArrayList<Class<?>>();
		categories.add(JPPFNodeMBeanProvider.class);
		registry = new ServiceRegistry(categories.iterator());
	}

	/**
	 * Retrieve all defined MBean providers.
	 * @return a list of <code>JPPFMBeanProvider</code> instances.
	 */
	public List<JPPFNodeMBeanProvider> findAllProviders()
	{
		List<JPPFNodeMBeanProvider> list = new ArrayList<JPPFNodeMBeanProvider>();
		//Iterator<JPPFMBeanProvider> it = registry.getServiceProviders(JPPFMBeanProvider.class, false);
		Iterator<JPPFNodeMBeanProvider> it = ServiceRegistry.lookupProviders(JPPFNodeMBeanProvider.class);
		while (it.hasNext()) list.add(it.next());
		return list;
	}

	/**
	 * Register the specified MBean.
	 * @param <T> - the type of the MBean interface. 
	 * @param impl - the MBean implementation.
	 * @param intf - the MBean exposed interface.
	 * @param name - the MBean name.
	 * @param server - the MBean server on which to register.
	 * @return true if the registration succeeded, false otherwise.
	 */
	public <T> boolean registerProviderMBean(T impl, Class<T> intf, String name, MBeanServer server)
	{
		try
		{
			if (debugEnabled) log.debug("found MBean provider: [name="+name+", inf="+intf+", impl="+impl.getClass().getName()+"]");
			server.registerMBean(impl, new ObjectName(name));
			/*
			StandardMBean std = new JPPFStandardMBean(impl, intf);
			server.registerMBean(std, new ObjectName(name));
			*/
			return true;
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return false;
	}

	/**
	 * Custom Standard MBean implementation.
	 */
	public static class JPPFStandardMBean extends StandardMBean
	{
		/**
		 * The class loader that loaded the implementation and interface.
		 */
		private ClassLoader cl = null;

		/**
		 * Initialize this standard MBean.
		 * @param implementation - the MBean's implementation.
		 * @param mbeanInterface - the MBean's interface.
		 * @throws NotCompliantMBeanException if the MBean does not comply tot he JMX specifications for MBeans.
		 */
		public JPPFStandardMBean(Object implementation, Class mbeanInterface) throws NotCompliantMBeanException
		{
			super(implementation, mbeanInterface);
			cl = mbeanInterface.getClassLoader();
		}

		/**
		 * Overriden to prevent MBeanInfo caching.
		 * @param info - the new MBeanInfo to cache.
		 * @see javax.management.StandardMBean#cacheMBeanInfo(javax.management.MBeanInfo)
		 */
		protected synchronized void cacheMBeanInfo(MBeanInfo info)
		{
			super.cacheMBeanInfo(info);
		}

		/**
		 * Overriden to prevent MBeanInfo caching.
		 * @return null.
		 * @see javax.management.StandardMBean#getCachedMBeanInfo()
		 */
		protected synchronized MBeanInfo getCachedMBeanInfo()
		{
			return super.getCachedMBeanInfo();
		}
	}
}
