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

package org.jppf.management.forwarding;

import static org.jppf.utils.collections.CollectionUtils.array;

import java.util.*;

import org.jppf.classloader.DelegationModel;
import org.jppf.jmx.JMXHelper;
import org.jppf.management.*;
import org.jppf.management.diagnostics.DiagnosticsMBean;
import org.jppf.node.provisioning.JPPFNodeProvisioningMBean;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.nodeserver.BaseNodeContext;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Implementation of the <code>JPPFNodeForwardingMBean</code> interface.
 * @author Laurent Cohen
 * @exclude
 * @deprecated use {@link NodeForwarding} instead.
 */
public class JPPFNodeForwarding extends AbstractNodeForwarding implements JPPFNodeForwardingMBean {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JPPFNodeForwarding.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this MBean implementation.
   * @param driver reference to the JPPF driver.
   */
  public JPPFNodeForwarding(final JPPFDriver driver) {
    super(driver);
  }

  @Override
  public Map<String, Object> forwardInvoke(final NodeSelector selector, final String name, final String methodName, final Object[] params, final String[] signature) throws Exception {
    final Set<BaseNodeContext> channels = selectionHelper.getChannels(selector);
    if (debugEnabled) log.debug("invoking {}() on mbean={} for selector={} ({} channels)", new Object[] {methodName, name, selector, channels.size()});
    return forward(JMXHelper.INVOKE, channels, name, methodName, params, signature);
  }

  @Override
  public Map<String, Object> forwardInvoke(final NodeSelector selector, final String name, final String methodName) throws Exception {
    return forwardInvoke(selector, name, methodName, (Object[]) null, (String[]) null);
  }

  @Override
  public Map<String, Object> forwardGetAttribute(final NodeSelector selector, final String name, final String attribute) throws Exception {
    final Set<BaseNodeContext> channels = selectionHelper.getChannels(selector);
    return forward(JMXHelper.GET_ATTRIBUTE, channels, name, attribute);
  }

  @Override
  public Map<String, Object> forwardSetAttribute(final NodeSelector selector, final String name, final String attribute, final Object value) throws Exception {
    final Set<BaseNodeContext> channels = selectionHelper.getChannels(selector);
    return forward(JMXHelper.SET_ATTRIBUTE, channels, name, attribute, value);
  }

  @Override
  public Map<String, Object> state(final NodeSelector selector) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "state");
  }

  @Override
  public Map<String, Object> updateThreadPoolSize(final NodeSelector selector, final Integer size) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "updateThreadPoolSize", array(size), array("java.lang.Integer"));
  }

  @Override
  public Map<String, Object> updateThreadsPriority(final NodeSelector selector, final Integer newPriority) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "updateThreadsPriority", array(newPriority), array("java.lang.Integer"));
  }

  @Override
  public Map<String, Object> restart(final NodeSelector selector) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "restart");
  }

  @Override
  public Map<String, Object> restart(final NodeSelector selector, final Boolean interruptIfRunning) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "restart", array(interruptIfRunning), array("java.lang.Boolean"));
  }

  @Override
  public Map<String, Object> shutdown(final NodeSelector selector) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "shutdown");
  }

  @Override
  public Map<String, Object> shutdown(final NodeSelector selector, final Boolean interruptIfRunning) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "shutdown", array(interruptIfRunning), array("java.lang.Boolean"));
  }

  @Override
  public Map<String, Object> resetTaskCounter(final NodeSelector selector) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "resetTaskCounter");
  }

  @Override
  public Map<String, Object> setTaskCounter(final NodeSelector selector, final Integer n) throws Exception {
    return forwardSetAttribute(selector, JPPFNodeAdminMBean.MBEAN_NAME, "TaskCounter", n);
  }

  @Override
  public Map<String, Object> updateConfiguration(final NodeSelector selector, final Map<Object, Object> configOverrides, final Boolean restart, final Boolean interruptIfRunning) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "updateConfiguration",
      new Object[] {configOverrides, restart, interruptIfRunning}, array("java.util.Map", "java.lang.Boolean", "java.lang.Boolean"));
  }

  @Override
  public Map<String, Object> updateConfiguration(final NodeSelector selector, final Map<Object, Object> configOverrides, final Boolean restart) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "updateConfiguration", new Object[] {configOverrides, restart}, array("java.util.Map", "java.lang.Boolean"));
  }

  @Override
  public Map<String, Object> cancelJob(final NodeSelector selector, final String jobUuid, final Boolean requeue) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "cancelJob", new Object[] {jobUuid, requeue}, array("java.lang.String", "java.lang.Boolean"));
  }

  @Override
  public Map<String, Object> getDelegationModel(final NodeSelector selector) throws Exception {
    return forwardGetAttribute(selector, JPPFNodeAdminMBean.MBEAN_NAME, "DelegationModel");
  }

  @Override
  public Map<String, Object> setDelegationModel(final NodeSelector selector, final DelegationModel model) throws Exception {
    return forwardSetAttribute(selector, JPPFNodeAdminMBean.MBEAN_NAME, "DelegationModel", model);
  }

  @Override
  public Map<String, Object> systemInformation(final NodeSelector selector) throws Exception {
    return forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "systemInformation");
  }

  @Override
  public Map<String, Object> healthSnapshot(final NodeSelector selector) throws Exception {
    return forwardInvoke(selector, DiagnosticsMBean.MBEAN_NAME_NODE, "healthSnapshot");
  }

  @Override
  public Map<String, Object> threadDump(final NodeSelector selector) throws Exception {
    return forwardInvoke(selector, DiagnosticsMBean.MBEAN_NAME_NODE, "threadDump");
  }

  @Override
  public Map<String, Object> gc(final NodeSelector selector) throws Exception {
    return forwardInvoke(selector, DiagnosticsMBean.MBEAN_NAME_NODE, "gc");
  }

  @Override
  public Map<String, Object> heapDump(final NodeSelector selector) throws Exception {
    return forwardInvoke(selector, DiagnosticsMBean.MBEAN_NAME_NODE, "heapDump");
  }

  @Override
  public Map<String, Object> getNbSlaves(final NodeSelector selector) throws Exception {
    return forwardGetAttribute(selector, JPPFNodeProvisioningMBean.MBEAN_NAME, "NbSlaves");
  }

  @Override
  public Map<String, Object> provisionSlaveNodes(final NodeSelector selector, final int nbNodes) throws Exception {
    return forwardInvoke(selector, JPPFNodeProvisioningMBean.MBEAN_NAME, "provisionSlaveNodes", new Object[] { nbNodes }, new String[] { "int" });
  }

  @Override
  public Map<String, Object> provisionSlaveNodes(final NodeSelector selector, final int nbNodes, final boolean interruptIfRunning) throws Exception {
    return forwardInvoke(selector, JPPFNodeProvisioningMBean.MBEAN_NAME, "provisionSlaveNodes", new Object[] { nbNodes, interruptIfRunning }, new String[] { "int", "boolean" });
  }

  @Override
  public Map<String, Object> provisionSlaveNodes(final NodeSelector selector, final int nbNodes, final TypedProperties configOverrides) throws Exception {
    return forwardInvoke(selector, JPPFNodeProvisioningMBean.MBEAN_NAME, "provisionSlaveNodes",
      new Object[] { nbNodes, configOverrides }, new String[] { "int", TypedProperties.class.getName() });
  }

  @Override
  public Map<String, Object> provisionSlaveNodes(final NodeSelector selector, final int nbNodes, final boolean interruptIfRunning, final TypedProperties configOverrides) throws Exception {
    return forwardInvoke(selector, JPPFNodeProvisioningMBean.MBEAN_NAME, "provisionSlaveNodes",
      new Object[] { nbNodes, interruptIfRunning, configOverrides }, new String[] { "int", "boolean", TypedProperties.class.getName() });
  }

  /**
   * Forward the specified operation to the specified nodes.
   * @param type the type of operation to forward.
   * @param nodes the nodes to forward to.
   * @param mbeanName the name of the node MBean to which the request is sent.
   * @param memberName the name of the method to invoke, or of the attribute to get or set.
   * @param params additional params to send with the request.
   * @return a mapping of node uuids to the result of invoking the MBean operation on the corresponding node. Each result may be an exception.
   * Additionally, each result may be {@code null}, in particular if the invoked method has a {@code void} return type.
   * @throws Exception if the invocation failed.
   */
  Map<String, Object> forward(final byte type, final Set<BaseNodeContext> nodes, final String mbeanName, final String memberName, final Object...params) throws Exception {
    try {
      final int size = nodes.size();
      if (size <= 0) return Collections.<String, Object>emptyMap();
      final ForwardCallbackImpl callback = new ForwardCallbackImpl(size);
      AbstractForwardingTask<?> task;
      for (final BaseNodeContext node: nodes) {
        final JMXConnectionWrapper jmx = node.getJmxConnection();
        switch(type) {
          case JMXHelper.INVOKE:
            task = new AbstractForwardingTask<Object>(node.getUuid(), callback) {
              @Override
              Object execute() throws Exception {
                return jmx.invoke(mbeanName, memberName, (Object[]) params[0], (String[]) params[1]);
              }
            };
            break;
          case JMXHelper.GET_ATTRIBUTE:
            task = new AbstractForwardingTask<Object>(node.getUuid(), callback) {
              @Override
              Object execute() throws Exception {
                return jmx.getAttribute(mbeanName, memberName);
              }
            };
            break;
          case JMXHelper.SET_ATTRIBUTE:
            task = new AbstractForwardingTask<Object>(node.getUuid(), callback) {
              @Override
              Object execute() throws Exception {
                jmx.setAttribute(mbeanName, memberName, params[0]);
                return null;
              }
            };
            break;
          default:
            throw new IllegalArgumentException(String.format(
              "unknown type of operation %d for mbean=%s, memeber=%s, param=%s, node=%s", type, mbeanName, memberName, Arrays.deepToString(params), node));
        }
        if (debugEnabled) log.debug("about to forward with type={}, mbean={}, member={}, params={}, node={}", type, mbeanName, memberName, Arrays.deepToString(params), node);
        executor.execute(task);
      }
      return callback.await();
    } catch (final Exception e) {
      if (debugEnabled) {
        log.debug("error forwarding with type={}, nb nodes={}, mbeanNaem={}, memberName={}, params={}\n{}",
          type, nodes.size(), mbeanName, memberName, Arrays.asList(params), ExceptionUtils.getStackTrace(e));
      }
      throw e;
    }
  }

  /**
   * A callback invoked by each submitted forwarding task to notify that results have arrived from a node.
   */
  private static class ForwardCallbackImpl implements ForwardCallback<Object> {
    /**
     * The map holding the results from all nodes.
     */
    private final Map<String, Object> resultMap;
    /**
     * The expected total number of results.
     */
    private final int expectedCount;
    /**
     * The current count of received results.
     */
    private int count;

    /**
     * Initialize with the specified expected total number of results.
     * @param expectedCount the expected total number of results.
     */
    ForwardCallbackImpl(final int expectedCount) {
      this.resultMap = new HashMap<>(expectedCount);
      this.expectedCount = expectedCount;
    }

    @Override
    public void gotResult(final String uuid, final InvocationResult<Object> result) {
      synchronized(this) {
        resultMap.put(uuid, result.isException() ? result.exception() : result.result());
        if (++count == expectedCount) notify();
      }
    }

    /**
     * Wait until the number of results reaches the expected count.
     * @return the results map;
     * @throws Exception if any error occurs.
     */
    public Map<String, Object> await() throws Exception {
      synchronized(this) {
        while (count < expectedCount) wait();
      }
      return resultMap;
    }
  }
}
