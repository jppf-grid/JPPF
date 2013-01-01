/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package org.jppf.management;

import java.util.*;

import javax.management.NotificationBroadcasterSupport;

import org.jppf.classloader.DelegationModel;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.nodeserver.*;
import org.slf4j.*;

/**
 * Implementation of the <code>JPPFNodeForwardingMBean</code> interface.
 * @author Laurent Cohen
 */
public class JPPFNodeForwarding extends NotificationBroadcasterSupport implements JPPFNodeForwardingMBean
{
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JPPFNodeForwarding.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Base name used for localization lookups.
   */
  private static final String I18N_BASE = "org.jppf.server.i18n.server_messages";
  /**
   * Reference to the JPPF drvier.
   */
  private final JPPFDriver driver = JPPFDriver.getInstance();

  /**
   * Initialize this MBean implementation.
   */
  public JPPFNodeForwarding()
  {
  }

  @Override
  public Map<String, Object> forwardInvoke(final NodeSelector selector, final String name, final String methodName, final Object[] params, final String[] signature) throws Exception
  {
    Set<AbstractNodeContext> channels = getChannels(selector);
    Map<String, Object> map = new HashMap<String, Object>();
    for (AbstractNodeContext context: channels)
    {
      String uuid = context.getUuid();
      try
      {
        JMXNodeConnectionWrapper wrapper = context.getJmxConnection();
        Object o = wrapper.invoke(name, methodName, params, signature);
        if (debugEnabled) log.debug("invoking '" + methodName + "()' on node " + uuid + ", result = " + o);
        map.put(uuid, o);
      }
      catch (Exception e)
      {
        if (debugEnabled) log.debug("exception invoking '" + methodName + "()' on node " + uuid, e);
        map.put(uuid, e);
      }
    }
    return map;
  }

  @Override
  public Map<String, Object> forwardInvoke(final NodeSelector selector, final String name, final String methodName) throws Exception
  {
    return forwardInvoke(selector, name, methodName, (Object[]) null, (String[]) null);
  }

  @Override
  public Map<String, Object> forwardGetAttribute(final NodeSelector selector, final String name, final String attribute) throws Exception
  {
    Set<AbstractNodeContext> channels = getChannels(selector);
    Map<String, Object> map = new HashMap<String, Object>();
    for (AbstractNodeContext context: channels)
    {
      String uuid = context.getUuid();
      try
      {
        JMXNodeConnectionWrapper wrapper = context.getJmxConnection();
        Object o = wrapper.getAttribute(name, attribute);
        if (debugEnabled) log.debug("getting attribute '" + attribute + "' from node " + uuid + ", result = " + o);
        map.put(uuid, o);
      }
      catch (Exception e)
      {
        if (debugEnabled) log.debug("exception getting attribute '" + attribute + "' from node " + uuid, e);
        map.put(uuid, e);
      }
    }
    return map;
  }

  @Override
  public Map<String, Object> forwardSetAttribute(final NodeSelector selector, final String name, final String attribute, final Object value) throws Exception
  {
    Set<AbstractNodeContext> channels = getChannels(selector);
    Map<String, Object> map = new HashMap<String, Object>();
    for (AbstractNodeContext context: channels)
    {
      String uuid = context.getUuid();
      try
      {
        JMXNodeConnectionWrapper wrapper = context.getJmxConnection();
        wrapper.setAttribute(name, attribute, value);
        if (debugEnabled) log.debug("set attribute '" + attribute + "' on node " + uuid);
      }
      catch (Exception e)
      {
        if (debugEnabled) log.debug("exception setting attribute '" + attribute + "' on node " + uuid, e);
        map.put(uuid, e);
      }
    }
    return map;
  }

  /**
   * Get a set of channels based on a NodeSelector.
   * @param selector the node selector used as a filter.
   * @return a set of {@link AbstractNodeContext} instances.
   */
  private Set<AbstractNodeContext> getChannels(final NodeSelector selector)
  {
    if (selector == null) throw new IllegalArgumentException("selector cannot be null");
    if (selector instanceof NodeSelector.AllNodesSelector) return getNodeNioServer().getAllChannelsAsSet();
    else if (selector instanceof NodeSelector.UuidSelector)
      return getChannels(new HashSet<String>(((NodeSelector.UuidSelector) selector).getUuidList()));
    else if (selector instanceof NodeSelector.ExecutionPolicySelector)
      return getChannels(((NodeSelector.ExecutionPolicySelector) selector).getPolicy());
    throw new IllegalArgumentException("unknown selector type: " + selector.getClass().getName());
  }

  /**
   * Get the available channels with the specified node uuids.
   * @param uuids the node uuids for which we want a channel reference.
   * @return a {@link Set} of {@link AbstractNodeContext} instances.
   */
  private Set<AbstractNodeContext> getChannels(final Set<String> uuids)
  {
    Set<AbstractNodeContext> result = new HashSet<AbstractNodeContext>();
    List<AbstractNodeContext> allChannels = getNodeNioServer().getAllChannels();
    for (AbstractNodeContext context: allChannels)
    {
      if (uuids.contains(context.getUuid())) result.add(context);
    }
    return result;
  }


  /**
   * Get the available channels for the specified nodes.
   * @param policy an execution to matrch against the nodes.
   * @return a {@link Set} of {@link AbstractNodeContext} instances.
   */
  private Set<AbstractNodeContext> getChannels(final ExecutionPolicy policy)
  {
    Set<AbstractNodeContext> result = new HashSet<AbstractNodeContext>();
    List<AbstractNodeContext> allChannels = getNodeNioServer().getAllChannels();
    for (AbstractNodeContext context: allChannels)
    {
      JPPFSystemInformation info = context.getSystemInformation();
      if (info == null) continue;
      if (policy.accepts(info)) result.add(context);
    }
    return result;
  }

  /**
   * Get the JPPF nodes server.
   * @return a <code>NodeNioServer</code> instance.
   * @exclude
   */
  private NodeNioServer getNodeNioServer()
  {
    return driver.getNodeNioServer();
  }

  @Override
  public Map<String, Object> state(final NodeSelector selector) throws Exception
  {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "state");
  }

  @Override
  public Map<String, Object> updateThreadPoolSize(final NodeSelector selector, final Integer size) throws Exception
  {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "updateThreadPoolSize", new Object[] { size }, new String[] { "java.lang.Integer" });
  }

  @Override
  public Map<String, Object> updateThreadsPriority(final NodeSelector selector, final Integer newPriority) throws Exception
  {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "updateThreadsPriority", new Object[] { newPriority }, new String[] { "java.lang.Integer" });
  }

  @Override
  public Map<String, Object> restart(final NodeSelector selector) throws Exception
  {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "restart");
  }

  @Override
  public Map<String, Object> shutdown(final NodeSelector selector) throws Exception
  {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "shutdown");
  }

  @Override
  public Map<String, Object> resetTaskCounter(final NodeSelector selector) throws Exception
  {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "resetTaskCounter");
  }

  @Override
  public Map<String, Object> setTaskCounter(final NodeSelector selector, final Integer n) throws Exception
  {
    return forwardSetAttribute(selector, JPPFNodeAdminMBean.MBEAN_NAME, "TaskCounter", n);
  }

  @Override
  public Map<String, Object> updateConfiguration(final NodeSelector selector, final Map<Object, Object> config, final Boolean reconnect) throws Exception
  {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "updateConfiguration",
        new Object[] { config, reconnect }, new String[] { "java.util.Map", "java.lang.Boolean" });
  }

  @Override
  public Map<String, Object> cancelJob(final NodeSelector selector, final String jobUuid, final Boolean requeue) throws Exception
  {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "cancelJob", new Object[] { jobUuid, requeue }, new String[] { "java.lang.String", "java.lang.Boolean" });
  }

  @Override
  public Map<String, Object> getDelegationModel(final NodeSelector selector) throws Exception
  {
    return forwardGetAttribute(selector, JPPFNodeAdminMBean.MBEAN_NAME, "DelegationModel");
  }

  @Override
  public Map<String, Object> setDelegationModel(final NodeSelector selector, final DelegationModel model) throws Exception
  {
    return forwardSetAttribute(selector, JPPFNodeAdminMBean.MBEAN_NAME, "DelegationModel", model);
  }

  @Override
  public Map<String, Object> systemInformation(final NodeSelector selector) throws Exception
  {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "systemInformation");
  }
}
