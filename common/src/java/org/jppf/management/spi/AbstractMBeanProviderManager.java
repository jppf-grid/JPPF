/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import javax.management.*;

import org.jppf.management.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class manage all management plugins defined through the Service Provider Interface.
 * @param <S> the SPI interface for the mbean provider.
 * @author Laurent Cohen
 */
public abstract class AbstractMBeanProviderManager<S extends JPPFMBeanProvider> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AbstractMBeanProviderManager.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Keeps a list of MBeans registered with the MBean server.
   */
  protected final List<String> registeredMBeanNames = new Vector<>();
  /**
   * The mbean server with which all mbeans are registered.
   */
  protected final MBeanServer server;

  /**
   * Initialize this mbean provider manager and register the MBeans implementing the specified provider interface.
   * @param clazz the class object for the provider interface.
   * @param cl the class loader used to oad the MBean implementation classes.
   * @param createParams the parameters used to create the MBean implementations.
   * @param server the MBean server on which to register.
   * @throws Exception if the registration failed.
   */
  public AbstractMBeanProviderManager(final Class<S> clazz, final ClassLoader cl, final Object server, final Object...createParams) throws Exception {
    this.server = (MBeanServer) server;
    final ClassLoader tmp = Thread.currentThread().getContextClassLoader();
    ClassLoader loader = cl == null ? tmp : cl;
    if (loader == null) loader = getClass().getClassLoader();
    final List<S> providers = new ServiceFinder().findProviders(clazz, loader);
    try {
      Thread.currentThread().setContextClassLoader(loader);
      for (final S provider: providers) {
        try {
          processMBeanProvider(provider, loader, createParams);
        } catch (final Exception e) {
          final String message = "error processing MBean provider {} : {}";
          if (debugEnabled) log.debug(message, provider, ExceptionUtils.getStackTrace(e));
          else log.warn(message, provider, ExceptionUtils.getMessage(e));
        }
      }
    } finally {
      Thread.currentThread().setContextClassLoader(tmp);
    }
  }

  /**
   * 
   * @param provider the mbean provider to process.
   * @param params the parmaters to pass to the mbean implemantation.
   * @return an mbean inmplementation.
   * @throws Exception if any error occurs
   */
  protected abstract Object createMBeanImpl(final S provider, final Object...params) throws Exception;

  /**
   * 
   * @param provider the mbean provider to process.
   * @param cl the class loader to use.
   * @param params the parmaters to pass to the mbean implemantation.
   * @throws Exception if any error occurs
   */
  private void processMBeanProvider(final S provider, final ClassLoader cl, final Object...params) throws Exception {
    final Object mbean = createMBeanImpl(provider, params);
    if (mbean == null) {
      log.warn("an MBean implementation of type {} could not be created", (provider == null) ? "null" : provider.getClass().getName());
      return;
    }
    final String infName = provider.getMBeanInterfaceName();
    final Class<?> inf = Class.forName(infName, true, cl);
    final String mbeanName = provider.getMBeanName();
    final boolean b = registerProviderMBean(mbean, inf, mbeanName);
    if (debugEnabled) log.debug("MBean registration " + (b ? "succeeded" : "failed") + " for [" + mbeanName + ']');
    if (b) registeredMBeanNames.add(mbeanName);
  }

  /**
   * Register the specified MBean.
   * @param impl the MBean implementation.
   * @param intf the MBean exposed interface.
   * @param name the MBean name.
   * @return true if the registration succeeded, false otherwise.
   */
  private boolean registerProviderMBean(final Object impl, final Class<?> intf, final String name) {
    try {
      if (debugEnabled) log.debug("found MBean provider: [name={}, inf={}, impl={}]", name, intf, impl.getClass().getName());
      final ObjectName objectName = ObjectNameCache.getObjectName(name);
      if (!server.isRegistered(objectName)) {
        server.registerMBean(impl, objectName);
        if (debugEnabled) log.debug("registered {} with mbean server {}", name, server);
        /*
        else if (name.contains("config.notifier,type=node"))
          log.info("registered {} with {}, call stack:\n{}", name, JPPFMBeanServerFactory.toString(server), ExceptionUtils.getCallStack());
        */
        return true;
      } else log.warn("an instance of MBean [{}] already exists, registration was skipped", name);
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
    }
    return false;
  }

  /**
   * Un-register all registered mbeans.
   */
  public void unregisterProviderMBeans() {
    if (debugEnabled) log.debug("list of registered MBeans {}", registeredMBeanNames);
    while (!registeredMBeanNames.isEmpty()) {
      final String s = registeredMBeanNames.remove(0);
      try {
        server.unregisterMBean(ObjectNameCache.getObjectName(s));
        if (debugEnabled) log.debug("MBean un-registration succeeded for [{}]", s);
      } catch(final Exception e) {
        final String format = "MBean un-registration failed for [{}] : {}";
        if (debugEnabled) log.debug(format, s, ExceptionUtils.getStackTrace(e));
        else log.warn(format, s, ExceptionUtils.getMessage(e));
      }
    }
  }
}
