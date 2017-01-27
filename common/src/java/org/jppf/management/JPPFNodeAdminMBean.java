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

package org.jppf.management;

import java.util.Map;

import org.jppf.classloader.DelegationModel;


/**
 * Exposed interface of the JPPF node management bean.
 * @author Laurent Cohen
 */
public interface JPPFNodeAdminMBean extends JPPFAdminMBean {
  /**
   * Name of the node's admin MBean.
   */
  String MBEAN_NAME = "org.jppf:name=admin,type=node";

  /**
   * Get the latest state information from the node.
   * @return a <code>JPPFNodeState</code> information.
   * @throws Exception if any error occurs.
   */
  JPPFNodeState state() throws Exception;

  /**
   * Set the size of the node's thread pool.
   * @param size the size as an int.
   * @throws Exception if any error occurs.
   */
  void updateThreadPoolSize(Integer size) throws Exception;

  /**
   * Update the priority of all execution threads.
   * @param newPriority the new priority to set.
   * @throws Exception if an error is raised when invoking the node mbean.
   */
  void updateThreadsPriority(Integer newPriority) throws Exception;

  /**
   * Restart the node.
   * This is equivalent to calling {@code restart(true)}.
   * @throws Exception if any error occurs.
   */
  void restart() throws Exception;

  /**
   * Restart the node.
   * @param interruptIfRunning when {@code true}, then restart the node even if it is executing tasks,
   * when {@code false}, then only restart the node when it is no longer executing.
   * @throws Exception if any error occurs.
   * @since 5.0
   */
  void restart(Boolean interruptIfRunning) throws Exception;

  /**
   * Shutdown the node.
   * @throws Exception if any error occurs.
   */
  void shutdown() throws Exception;

  /**
   * Shutdown the node.
   * @param interruptIfRunning when {@code true}, then shutdown the node even if it is executing tasks,
   * when {@code false}, then only shutdown the node when it is no longer executing.
   * @throws Exception if any error occurs.
   * @since 5.0
   */
  void shutdown(Boolean interruptIfRunning) throws Exception;

  /**
   * Reset the node's executed tasks counter to zero.
   * @throws Exception if any error occurs.
   */
  void resetTaskCounter() throws Exception;

  /**
   * Reset the node's executed tasks counter to the specified value.
   * @param n the number to set the task counter to.
   * @throws Exception if any error occurs.
   */
  void setTaskCounter(Integer n) throws Exception;

  /**
   * Update the configuration properties of the node.
   * @param configOverrides the set of properties to update.
   * @param restart specifies whether the node should be restarted after updating the properties.
   * @param interruptIfRunning when {@code true}, then restart the node even if it is executing tasks, when {@code false}, then only shutdown the node when it is no longer executing.
   * This parameter only applies when the {@code restart} parameter is {@code true}.
   * @throws Exception if any error occurs.
   * @since 5.2
   */
  void updateConfiguration(Map<Object, Object> configOverrides, Boolean restart, Boolean interruptIfRunning) throws Exception;

  /**
   * Update the configuration properties of the node. This method is equivalent to calling {@link #updateConfiguration(Map, Boolean, Boolean) updateConfiguration(configOverrides, restart, true)}.
   * @param configOverrides the set of properties to update.
   * @param restart specifies whether the node should be restarted after updating the properties.
   * @throws Exception if any error occurs.
   */
  void updateConfiguration(Map<Object, Object> configOverrides, Boolean restart) throws Exception;

  /**
   * Cancel the job with the specified id.
   * @param jobId the id of the job to cancel.
   * @param requeue true if the job should be requeued on the server side, false otherwise.
   * @throws Exception if any error occurs.
   */
  void cancelJob(String jobId, Boolean requeue) throws Exception;

  /**
   * Get the current class loader delegation model for the node.
   * @return either {@link org.jppf.classloader.DelegationModel#PARENT_FIRST PARENT_FIRST} or {@link org.jppf.classloader.DelegationModel#URL_FIRST LOCAL_FIRST}.
   * @throws Exception if any error occurs.
   */
  DelegationModel getDelegationModel() throws Exception;

  /**
   * Set the current class loader delegation model for the node.
   * @param model either either {@link org.jppf.classloader.DelegationModel#PARENT_FIRST PARENT_FIRST} or {@link org.jppf.classloader.DelegationModel#URL_FIRST LOCAL_FIRST}.
   * If any other value is specified then this method has no effect.
   * @throws Exception if any error occurs.
   */
  void setDelegationModel(DelegationModel model) throws Exception;

  /**
   * Determine wether a deffered shutdwon or restartd was requested and not yet performed for the node.
   * @return one of the possible pending actions specified in the enum {@link NodePendingAction}.
   */
  NodePendingAction pendingAction();

  /**
   * Cancel a previous deferred shutdown or restart request, if any.
   * @return {@code true} if the node has a pending action and it was cancelled, {@code false} otherwise.
   */
  boolean cancelPendingAction();
}
