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
package org.jppf.server.node.android;

import java.util.List;

import org.jppf.JPPFError;
import org.jppf.classloader.*;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.node.*;
import org.jppf.node.event.LifeCycleEventHandler;
import org.jppf.node.protocol.Task;
import org.jppf.server.node.*;
import org.jppf.server.protocol.*;
import org.jppf.startup.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class encapsulate execution nodes.
 * @author Laurent Cohen
 * @author Domingos Creado
 */
public abstract class AbstractJPPFAndroidNode extends AbstractNode
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractJPPFAndroidNode.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The task execution manager for this node.
   */
  protected AndroidNodeExecutionManager executionManager = null;
  /**
   * The object responsible for this node's I/O.
   */
  protected NodeIO nodeIO = null;
  /**
   * Action executed when the node exits the main loop, in its {@link #run() run()} method.
   */
  private Runnable exitAction = null;
  /**
   * Handles the firing of node life cycle events and the listeners that subscribe to these events.
   */
  private LifeCycleEventHandler lifeCycleEventHandler = null;
  /**
   * Manages the class loaders and how they are used.
   */
  protected AbstractClassLoaderManager classLoaderManager = null;

  /**
   * Default constructor.
   */
  public AbstractJPPFAndroidNode()
  {
    uuid = NodeRunner.getUuid();
    executionManager = new AndroidNodeExecutionManager(this);
  }

  /**
   * Main processing loop of this node.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    setStopped(false);
    boolean initialized = false;
    if (debugEnabled) log.debug("Start of node main loop, nodeUuid=" + uuid);
    while (!isStopped())
    {
      try
      {
        init();
        if (!initialized)
        {
          System.out.println("Node successfully initialized");
          initialized = true;
        }
        perform();
      }
      catch(SecurityException e)
      {
        throw new JPPFError(e);
      }
      catch(Exception e)
      {
        log.error(e.getMessage(), e);
        if (classLoaderManager.getClassLoader() != null)
        {
          classLoaderManager.getClassLoader().reset();
          classLoaderManager.setClassLoader(null);
        }
        try
        {
          synchronized(this)
          {
            closeDataChannel();
            classLoaderManager.getContainerMap().clear();
            classLoaderManager.getContainerList().clear();
          }
        }
        catch(Exception ex)
        {
          log.error(ex.getMessage(), ex);
        }
      }
    }
    if (debugEnabled) log.debug("End of node main loop");
    if (exitAction != null)
    {
      Runnable r = exitAction;
      setExitAction(null);
      r.run();
    }
  }

  /**
   * Perform the main execution loop for this node. At each iteration, this method listens for a task to execute,
   * receives it, executes it and sends the results back.
   * @throws Exception if an error was raised from the underlying socket connection or the class loader.
   */
  public void perform() throws Exception
  {
    if (debugEnabled) log.debug("Start of node secondary loop");
    while (!isStopped())
    {
      Pair<JPPFTaskBundle, List<Task>> pair = nodeIO.readTask();
      JPPFTaskBundle bundle = pair.first();
      checkInitialBundle(bundle);
      List<Task> taskList = pair.second();
      boolean notEmpty = (taskList != null) && (!taskList.isEmpty());
      if (debugEnabled)
      {
        if (notEmpty) log.debug("received a bundle with " + taskList.size()  + " tasks");
        else log.debug("received an empty bundle");
      }
      if (notEmpty) executionManager.execute(bundle, taskList);
      processResults(bundle, taskList);
    }
    if (debugEnabled) log.debug("End of node secondary loop");
  }

  /**
   * Checks whether the received bundle is the initial one sent by the driver,
   * and prepare a specific response if it is.
   * @param bundle - the bundle to check.
   */
  private void checkInitialBundle(final JPPFTaskBundle bundle)
  {
    if (JPPFTaskBundle.State.INITIAL_BUNDLE.equals(bundle.getState()))
    {
      if (debugEnabled) log.debug("setting initial bundle uuid");
      bundle.setBundleUuid(uuid);
      bundle.setParameter(BundleParameter.NODE_UUID_PARAM, uuid);
    }
  }

  /**
   * Send the results back to the server and perform final checks for the current execution.
   * @param bundle - the bundle that contains the tasks and header information.
   * @param taskList - the tasks results.
   * @throws Exception if any error occurs.
   */
  private void processResults(final JPPFTaskBundle bundle, final List<Task> taskList) throws Exception
  {
    if (debugEnabled) log.debug("processing results for job '" + bundle.getName() + '\'');
    if (executionManager.checkConfigChanged())
    {
      JPPFSystemInformation info = new JPPFSystemInformation(NodeRunner.getUuid());
      info.populate();
      bundle.setParameter(BundleParameter.NODE_SYSTEM_INFO_PARAM, info);
    }
    nodeIO.writeResults(bundle, taskList);
    if ((taskList != null) && (!taskList.isEmpty()))
    {
      setTaskCount(getTaskCount() + taskList.size());
      if (debugEnabled) log.debug("tasks executed: " + getTaskCount());
    }
  }

  /**
   * Initialize this node's resources.
   * @throws Exception if an error is raised during initialization.
   */
  private synchronized void init() throws Exception
  {
    if (debugEnabled) log.debug("start node initialization");
    initHelper();
    new JPPFStartupLoader().load(JPPFNodeStartupSPI.class);
    initDataChannel();
    lifeCycleEventHandler = new LifeCycleEventHandler(executionManager);
    lifeCycleEventHandler.loadListeners();
    lifeCycleEventHandler.fireNodeStarting();
    if (debugEnabled) log.debug("end node initialization");
  }

  /**
   * Initialize this node's data channel.
   * @throws Exception if an error is raised during initialization.
   */
  protected abstract void initDataChannel() throws Exception;

  /**
   * Initialize this node's data channel.
   * @throws Exception if an error is raised during initialization.
   */
  protected abstract void closeDataChannel() throws Exception;

  /**
   * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
   * @return a <code>ClassLoader</code> used for loading the classes of the framework.
   */
  public AbstractJPPFClassLoader getClassLoader()
  {
    return classLoaderManager.getClassLoader();
  }

  /**
   * Set the main classloader for the node.
   * @param cl the class loader to set.
   */
  public void setClassLoader(final JPPFClassLoader cl)
  {
    classLoaderManager.setClassLoader(cl);
  }

  /**
   * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
   * @throws Exception if an error occurs while instantiating the class loader.
   */
  public void initHelper() throws Exception
  {
    if (debugEnabled) log.debug("Initializing serializer");
    Class<?> c = getClassLoader().loadJPPFClass("org.jppf.utils.ObjectSerializerImpl");
    if (debugEnabled) log.debug("Loaded serializer class " + c);
    Object o = c.newInstance();
    serializer = (ObjectSerializer) o;
    c = getClassLoader().loadJPPFClass("org.jppf.utils.SerializationHelperImpl");
    if (debugEnabled) log.debug("Loaded helper class " + c);
    o = c.newInstance();
    helper = (SerializationHelper) o;
    if (debugEnabled) log.debug("Serializer initialized");
  }

  /**
   * Get a reference to the JPPF container associated with an application uuid.
   * @param uuidPath the uuid path containing the key to the container.
   * @return a <code>JPPFContainer</code> instance.
   * @throws Exception if an error occurs while getting the container.
   */
  public JPPFContainer getContainer(final List<String> uuidPath) throws Exception
  {
    return classLoaderManager.getContainer(uuidPath);
  }

  /**
   * Get the task execution manager for this node.
   * @return a <code>NodeExecutionManager</code> instance.
   */
  public AndroidNodeExecutionManager getExecutionManager()
  {
    return executionManager;
  }

  /**
   * Stop this node and release the resources it is using.
   * @see org.jppf.node.Node#stopNode(boolean)
   */
  @Override
  public synchronized void stopNode()
  {
    if (debugEnabled) log.debug("stopping node");
    lifeCycleEventHandler.fireNodeEnding();
    setStopped(true);
    executionManager.shutdown();
    try
    {
      closeDataChannel();
    }
    catch(Exception ex)
    {
      log.error(ex.getMessage(), ex);
    }
    classLoaderManager.setClassLoader(null);
  }

  /**
   * Shutdown and eventually restart the node.
   * @param restart determines whether this node should be restarted by the node launcher.
   */
  public void shutdown(final boolean restart)
  {
    lifeCycleEventHandler.fireNodeEnding();
    NodeRunner.shutdown(this, restart);
  }

  /**
   * Set the action executed when the node exits the main loop.
   * @param exitAction the action to execute.
   */
  public synchronized void setExitAction(final Runnable exitAction)
  {
    this.exitAction = exitAction;
  }

  /**
   * Get the object that handles the firing of node life cycle events and the listeners that subscribe to these events.
   * @return an instance of <code>LifeCycleEventHandler</code>.
   */
  @Override
  public LifeCycleEventHandler getLifeCycleEventHandler()
  {
    return lifeCycleEventHandler;
  }
}
