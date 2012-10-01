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

package org.jppf.server.nio.nodeserver;

import java.util.ArrayList;
import java.util.List;

import org.jppf.execute.*;
import org.jppf.io.*;
import org.jppf.job.JobNotificationEmitter;
import org.jppf.management.JMXNodeConnectionWrapper;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.server.nio.*;
import org.jppf.server.protocol.*;
import org.jppf.server.scheduler.bundle.*;
import org.jppf.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context associated with a channel serving tasks to a node.
 * @author Laurent Cohen
 */
public abstract class AbstractNodeContext extends AbstractNioContext<NodeState> implements ExecutorChannel<ServerTaskBundle>
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractNodeContext.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Dummy runnable used for bundle execution.
   */
  protected static final Runnable RUNNABLE = new Runnable() {
    @Override
    public void run() {
    }
  };
  /**
   * The task bundle to send or receive.
   */
  protected ServerTaskBundle bundle = null;
  /**
   * Bundler used to schedule tasks for the corresponding node.
   */
  protected Bundler bundler = null;
  /**
   * Helper used to serialize the bundle objects.
   */
  protected SerializationHelper helper = new SerializationHelperImpl();
  /**
   * Represents the node system information.
   */
  private JPPFSystemInformation systemInfo = null;
  /**
   * Represents the management information.
   */
  private JPPFManagementInfo managementInfo = null;
  /**
   * List of execution status listeners for this channel.
   */
  private final List<ExecutorChannelStatusListener> listenerList = new ArrayList<ExecutorChannelStatusListener>();
  /**
   * <code>Runnable</code> called when node context is closed.
   */
  private Runnable onClose = null;

  /**
   * Performs all operations that relate to channel states.
   */
  private final StateTransitionManager<NodeState, NodeTransition> transitionManager;
  /**
   * Provides access to the management functions of the driver.
   */
  protected JMXNodeConnectionWrapper jmxConnection = null;

  /**
   * Initialized abstract node context.
   * @param transitionManager instance of transition manager used by this node context.
   */
  protected AbstractNodeContext(final StateTransitionManager<NodeState, NodeTransition> transitionManager) {
    this.transitionManager = transitionManager;
  }

  /**
   * Get the task bundle to send or receive.
   * @return a <code>ServerJob</code> instance.
   */
  public ServerTaskBundle getBundle()
  {
    return bundle;
  }

  /**
   * Set the task bundle to send or receive.
   * @param bundle a {@link JPPFTaskBundle} instance.
   */
  public void setBundle(final ServerTaskBundle bundle)
  {
    this.bundle = bundle;

    if(bundle != null)
    {
      int bundleTaskCount = bundle.getTaskCount();
      int jobTaskCount = bundle.getJob().getTaskCount();
      int realTaskCount = bundle.getTasksL().size();

      if(bundleTaskCount != jobTaskCount || bundleTaskCount != realTaskCount)
      {
        throw new IllegalStateException("bundle.taskCount <> job.taskCount");
      }
    }
  }

  /**
   * Get the bundler used to schedule tasks for the corresponding node.
   * @return a {@link Bundler} instance.
   */
  @Override
  public Bundler getBundler()
  {
    return bundler;
  }

  /**
   * Set the bundler used to schedule tasks for the corresponding node.
   * @param bundler a {@link Bundler} instance.
   */
  public void setBundler(final Bundler bundler)
  {
    this.bundler = bundler;
  }

  /**
   * Check whether the bundler held by this context is up to date by comparison
   * with the specified bundler.<br>
   * If it is not, then it is replaced with a copy of the specified bundler, with a
   * timestamp taken at creation time.
   * @param serverBundler the bundler to compare with.
   * @param jppfContext execution context.
   * @return true if the bundler is up to date, false if it wasn't and has been updated.
   */
  @Override
  public boolean checkBundler(final Bundler serverBundler, final JPPFContext jppfContext)
  {
    if (serverBundler == null) throw new IllegalArgumentException("serverBundler is null");

    if (this.bundler == null || this.bundler.getTimestamp() < serverBundler.getTimestamp())
    {
      if (this.bundler != null)
      {
        this.bundler.dispose();
        if (this.bundler instanceof ContextAwareness) ((ContextAwareness)this.bundler).setJPPFContext(null);
      }
      this.bundler = serverBundler.copy();
      if (this.bundler instanceof ContextAwareness) ((ContextAwareness)this.bundler).setJPPFContext(jppfContext);
      this.bundler.setup();
      if (this.bundler instanceof NodeAwareness) ((NodeAwareness) this.bundler).setNodeConfiguration(systemInfo);
      return true;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handleException(final ChannelWrapper<?> channel, final Exception exception)
  {
    if (getBundler() != null) {
      getBundler().dispose();
      if (getBundler() instanceof ContextAwareness) ((ContextAwareness)getBundler()).setJPPFContext(null);
    }
    if(onClose != null) onClose.run();
    if ((bundle != null) && !JPPFTaskBundle.State.INITIAL_BUNDLE.equals(bundle.getJob().getState()))
    {
//      if(exception != null && !(exception instanceof EOFException)) exception.printStackTrace();
//      bundle.fireJobReturned((ExecutorChannel) channel.getContext()); // todo fix
      ServerTaskBundle tmpWrapper = bundle;
      setBundle(null);
      tmpWrapper.taskCompleted(new Exception(exception));
//      JPPFTaskBundle tmpBundle = (JPPFTaskBundle) tmpWrapper.getJob();
//      // broadcast jobs are not resubmitted.
//      if (tmpBundle.getSLA().isBroadcastJob()) tmpBundle.t(tmpWrapper);
//      else resubmitBundle(tmpWrapper);
    }
  }

  /**
   * Serialize this context's bundle into a byte buffer.
   * @param wrapper channel wrapper for this context.
   * @throws Exception if any error occurs.
   */
  public void serializeBundle(final ChannelWrapper<?> wrapper) throws Exception
  {
    AbstractTaskBundleMessage message = newMessage();
    message.addLocation(IOHelper.serializeData(bundle.getJob(), helper.getSerializer()));
    message.addLocation(bundle.getDataProviderL());
    for (DataLocation dl: bundle.getTasksL()) message.addLocation(dl);
//    System.out.println("serialize: Task count - bundle: " + bundle.getTaskCount() + "\t job: " + bundle.getJob().getTaskCount() + "\t real tasks: " + bundle.getTasksL().size());
    message.setBundle((JPPFTaskBundle) bundle.getJob());
    setMessage(message);
  }

  /**
   * Deserialize a task bundle from the message read into this buffer.
   * @return a {@link AbstractNodeContext} instance.
   * @throws Exception if an error occurs during the deserialization.
   * @param notificationEmitter an <code>JobNotificationEmitter</code> instance that fires job notifications.
   */
  public ServerTaskBundle deserializeBundle(final JobNotificationEmitter notificationEmitter) throws Exception
  {
    List<DataLocation> locations = ((AbstractTaskBundleMessage) message).getLocations();
    JPPFTaskBundle bundle = ((AbstractTaskBundleMessage) message).getBundle();
    List<DataLocation> tasks = new ArrayList<DataLocation>();
    if (locations.size() > 1)
    {
      for (int i=1; i<locations.size(); i++) tasks.add(locations.get(i));
    }
    return new ServerJob(notificationEmitter, bundle, null, tasks).copy(tasks.size());
  }

  /**
   * Create a new message.
   * @return an {@link AbstractTaskBundleMessage} instance.
   */
  public abstract AbstractTaskBundleMessage newMessage();

  @Override
  public boolean readMessage(final ChannelWrapper<?> channel) throws Exception
  {
    if (message == null) message = newMessage();
    return message.read(channel);
  }

  @Override
  public boolean writeMessage(final ChannelWrapper<?> channel) throws Exception
  {
    return message.write(channel);
  }

  /**
   * Get the node system information.
   * @return a {@link JPPFSystemInformation} instance.
   */
  @Override
  public JPPFSystemInformation getSystemInfo()
  {
    return systemInfo;
  }

  /**
   * Set the node system information.
   * @param nodeInfo a {@link JPPFSystemInformation} instance.
   */
  public void setNodeInfo(final JPPFSystemInformation nodeInfo)
  {
    setNodeInfo(nodeInfo, false);
  }

  /**
   * Set the node system information.
   * @param nodeInfo a {@link JPPFSystemInformation} instance.
   * @param update a flag indicates whether update system information in management information.
   */
  public void setNodeInfo(final JPPFSystemInformation nodeInfo, final boolean update)
  {
    if (update && debugEnabled) log.debug("updating node information for " + nodeInfo + ", channel=" + channel);
    this.systemInfo = nodeInfo;
    systemInfo.getJppf().setProperty("jppf.channel.local", String.valueOf(channel.isLocal()));
    if(update && managementInfo != null) managementInfo.setSystemInfo(nodeInfo);
  }

  /**
   * Get the management information.
   * @return a {@link JPPFManagementInfo} instance.
   */
  @Override
  public JPPFManagementInfo getManagementInfo()
  {
    return managementInfo;
  }

  /**
   * Set the management information.
   * @param managementInfo a {@link JPPFManagementInfo} instance.
   */
  public void setManagementInfo(final JPPFManagementInfo managementInfo)
  {
    if (debugEnabled) log.debug("context " + this + " setting management info [" + managementInfo + "]");
    this.managementInfo = managementInfo;
    initializeJmxConnection();
  }

  @Override
  public ExecutorStatus getExecutionStatus() {
    NodeState state = getState();
    if(state == null)
      return ExecutorStatus.DISABLED;
    else {
      switch (state) {
        case IDLE:
          if (getChannel().isOpen())
            return ExecutorStatus.ACTIVE;
          else
            return ExecutorStatus.FAILED;
        case SENDING_BUNDLE:
          return ExecutorStatus.EXECUTING;
        case WAITING_RESULTS:
          return ExecutorStatus.EXECUTING;
        default:
          return ExecutorStatus.DISABLED;
      }
    }
  }

  @Override
  public void setState(final NodeState state) {
    ExecutorStatus oldExecutionStatus = getExecutionStatus();
    super.setState(state);
    ExecutorStatus newExecutionStatus = getExecutionStatus();
    fireExecutionStatusChanged(oldExecutionStatus, newExecutionStatus);
  }

  @Override
  public void close() throws Exception {
    getChannel().close();
  }

  @Override
  public Object getMonitor() {
    return getChannel();
  }

  /**
   * Initialize the jmx connection using the specified jmx id.
   */
  public void initializeJmxConnection()
  {
    JPPFManagementInfo info = getManagementInfo();
    if(info == null)
      jmxConnection = null;
    else {
      jmxConnection = new JMXNodeConnectionWrapper(info.getHost(), info.getPort(), info.isSecure());
      jmxConnection.connect();
    }
  }

  /**
   * Get the object that provides access to the management functions of the driver.
   * @return a <code>JMXConnectionWrapper</code> instance.
   */
  public JMXNodeConnectionWrapper getJmxConnection()
  {
    return jmxConnection;
  }

  /**
   * Cancel the job with the specified id.
   * @param jobId the id of the job to cancel.
   * @param requeue true if the job should be requeued on the server side, false otherwise.
   * @throws Exception if any error occurs.
   * @see org.jppf.server.job.management.DriverJobManagementMBean#cancelJob(java.lang.String)
   * @return a <code>true</code> when cancel was successful <code>false</code> otherwise.
   */
  public boolean cancelJob(final String jobId, final boolean requeue) throws Exception
  {
    if (debugEnabled) log.debug("cancelling job uuid=" + jobId + " from " + this + ", jmxConnection=" + jmxConnection);
    if (jmxConnection != null && jmxConnection.isConnected())
    {
      jmxConnection.cancelJob(jobId, requeue);
      return true;
    }
    return false;
  }

  @Override
  public JPPFFuture<?> submit(final ServerTaskBundle bundleWrapper) {
    JPPFFuture<?> future = new JPPFFutureTask<Object>(RUNNABLE, null) {
      @Override
      public boolean cancel(final boolean mayInterruptIfRunning) {
        if (debugEnabled) log.debug("cancelling " + AbstractNodeContext.this + ", isCancelled()=" + isCancelled());
        if(isDone()) return false;
        if(isCancelled()) return true;
        else {
          bundle.cancel();
          try {
            cancelJob(bundle.getClientJob().getUuid(), false);
          } catch (Exception e) {
            if (debugEnabled) log.debug(e.getMessage(), e);
            else log.warn(ExceptionUtils.getMessage(e));
          } finally {
            return super.cancel(false);
          }
        }
      }
    };
    setBundle(bundleWrapper);
    transitionManager.transitionChannel(getChannel(), NodeTransition.TO_SENDING);
//    bundleWrapper.jobDispatched(getChannel(), future);
    if (getChannel().getSelector() != null) getChannel().getSelector().wakeUp();

    return future;
  }

  /**
   * Get the <code>Runnable</code> that will be called when node context is closed.
   * @return a <1code>Runnable</code> instance.
   */
  public Runnable getOnClose() {
    return onClose;
  }

  /**
   * Set the <code>Runnable</code> that will be called when node context is closed.
   * <code>Runnable</code> called when node context is closed.
   * @param onClose a <code>Runnable</code> called when node context is closed or <code>null</code>.
   */
  public void setOnClose(final Runnable onClose) {
    this.onClose = onClose;
  }

  @Override
  public void addExecutionStatusListener(final ExecutorChannelStatusListener listener) {
    if (listener == null) throw new IllegalArgumentException("listener is null");

    synchronized (listenerList)
    {
      listenerList.add(listener);
    }
  }

  @Override
  public void removeExecutionStatusListener(final ExecutorChannelStatusListener listener) {
    if (listener == null) throw new IllegalArgumentException("listener is null");

    synchronized (listenerList)
    {
      listenerList.remove(listener);
    }
  }

  /**
   * Notify all listeners that the execution status of this channel has changed.
   * @param oldValue the channel execution status before the change.
   * @param newValue the channel execution status after the change.
   */
  protected void fireExecutionStatusChanged(final ExecutorStatus oldValue, final ExecutorStatus newValue)
  {
    if (oldValue == newValue) return;
    ExecutorChannelStatusListener[] listeners;
    synchronized (listenerList)
    {
      listeners = listenerList.toArray(new ExecutorChannelStatusListener[listenerList.size()]);
    }
    ExecutorChannelStatusEvent event = new ExecutorChannelStatusEvent(this, oldValue, newValue);
    for (ExecutorChannelStatusListener listener : listeners) {
      listener.executionStatusChanged(event);
    }
  }
}
