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

package org.jppf.management.spi;

import java.util.*;

import javax.imageio.spi.ServiceRegistry;
import javax.management.*;

import org.apache.commons.logging.*;

/**
 * Instances of this class manage all management plugins defined through the Service Provider Interface.
 * @param <S> the SPI interface for the mbean provider.
 * @author Laurent Cohen
 */
public class JPPFMBeanProviderManager<S extends JPPFMBeanProvider>
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
	 * The class of the mbean provider interface.
	 */
	private Class<S> providerClass = null;
	/**
	 * The list of providers found in the class path.
	 */
	private List<S> providerList = null;
	/**
	 * Keeps a list of MBeans registered with the MBean server.
	 */
	private List<String> registeredMBeanNames = new Vector<String>();
	/**
	 * The mbean server with which all mbeans are registered.
	 */
	private MBeanServer server = null;

	/**
	 * Initialize this mbean provider manager.
	 * @param clazz the class object for the provider interface.
	 * @param server the MBean server on which to register.
	 */
	public JPPFMBeanProviderManager(Class<S> clazz, MBeanServer server)
	{
		this.providerClass = clazz;
		this.server = server;
	}

	/**
	 * Retrieve all defined MBean providers for the specified provider interface.
	 * @return a list of <code>S</code> instances.
	 */
	public List<S> getAllProviders()
	{
		if (providerList == null)
		{
			providerList = new ArrayList<S>();
			Iterator<S> it = ServiceRegistry.lookupProviders(providerClass);
			while (it.hasNext()) providerList.add(it.next());
		}
		return providerList;
	}

	/**
	 * Register the specified MBean.
	 * @param <T> the type of the MBean interface. 
	 * @param impl the MBean implementation.
	 * @param intf the MBean exposed interface.
	 * @param name the MBean name.
	 * @return true if the registration succeeded, false otherwise.
	 */
	public <T> boolean registerProviderMBean(T impl, Class<T> intf, String name)
	{
		try
		{
			if (debugEnabled) log.debug("found MBean provider: [name="+name+", inf="+intf+", impl="+impl.getClass().getName()+"]");
			server.registerMBean(impl, new ObjectName(name));
			registeredMBeanNames.add(name);
			return true;
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return false;
	}

	/**
	 * Un-register all registered mbeans.
	 */
	public void unregisterProviderMBeans()
	{
		while (!registeredMBeanNames.isEmpty())
		{
			String s = registeredMBeanNames.remove(0);
			try
			{
				server.unregisterMBean(new ObjectName(s));
			}
			catch(Exception e)
			{
				log.error(e.getMessage(), e);
			}
		}
	}
}
