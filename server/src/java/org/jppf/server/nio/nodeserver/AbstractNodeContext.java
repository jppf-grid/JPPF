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

import java.util.List;

import org.jppf.io.*;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.*;
import org.jppf.server.protocol.*;
import org.jppf.server.scheduler.bundle.*;
import org.jppf.utils.*;

/**
 * Context associated with a channel serving tasks to a node.
 * @author Laurent Cohen
 */
public abstract class AbstractNodeContext extends AbstractNioContext<NodeState>
{
  /**
   * The task bundle to send or receive.
   */
  protected ServerJob bundle = null;
  /**
   * Bundler used to schedule tasks for the corresponding node.
   */
  protected Bundler bundler = null;
  /**
   * Helper used to serialize the bundle objects.
   */
  protected SerializationHelper helper = new SerializationHelperImpl();
  /**
   * Determines whether this context is attached to a peer node.
   */
  private boolean peer = false;
  /**
   * True means the job was cancelled and the task completion listener must not be called.
   */
  protected boolean jobCanceled = false;
  /**
   * Represents the node system information.
   */
  protected JPPFSystemInformation nodeInfo = null;

  /**
   * Get the task bundle to send or receive.
   * @return a <code>BundleWrapper</code> instance.
   */
  public ServerJob getBundle()
  {
    return bundle;
  }

  /**
   * Set the task bundle to send or receive.
   * @param bundle a {@link JPPFTaskBundle} instance.
   */
  public void setBundle(final ServerJob bundle)
  {
    this.bundle = bundle;
  }

  /**
   * Get the bundler used to schedule tasks for the corresponding node.
   * @return a {@link Bundler} instance.
   */
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
  public boolean checkBundler(final Bundler serverBundler, final JPPFContext jppfContext)
  {
    if (this.bundler.getTimestamp() < serverBundler.getTimestamp())
    {
      this.bundler.dispose();
      if (this.bundler instanceof ContextAwareness) ((ContextAwareness)this.bundler).setJPPFContext(null);
      this.bundler = serverBundler.copy();
      if (this.bundler instanceof ContextAwareness) ((ContextAwareness)this.bundler).setJPPFContext(jppfContext);
      this.bundler.setup();
      if (this.bundler instanceof NodeAwareness) ((NodeAwareness) this.bundler).setNodeConfiguration(nodeInfo);
      return true;
    }
    return false;
  }

  /**
   * Resubmit a task bundle at the head of the queue. This method is invoked
   * when a node is disconnected while it was executing a task bundle.
   * @param bundle the task bundle to resubmit.
   */
  public void resubmitBundle(final ServerJob bundle)
  {
    JPPFDriver.getQueue().addBundle(bundle);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handleException(final ChannelWrapper<?> channel)
  {
    if (getBundler() != null) {
      getBundler().dispose();
      if (getBundler() instanceof ContextAwareness) ((ContextAwareness)getBundler()).setJPPFContext(null);
    }
    NodeNioServer.closeNode(channel, this);
    if ((bundle != null) && !JPPFTaskBundle.State.INITIAL_BUNDLE.equals(((JPPFTaskBundle) bundle.getJob()).getState()))
    {
      JPPFDriver.getInstance().getJobManager().jobReturned(bundle, channel);
      ServerJob tmpWrapper = bundle;
      bundle = null;
      JPPFTaskBundle tmpBundle = (JPPFTaskBundle) tmpWrapper.getJob();
      // broadcast jobs are not resubmitted.
      if (tmpBundle.getSLA().isBroadcastJob()) tmpBundle.fireTaskCompleted(tmpWrapper);
      else resubmitBundle(tmpWrapper);
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
    message.addLocation(bundle.getDataProvider());
    for (DataLocation dl: bundle.getTasks()) message.addLocation(dl);
    message.setBundle((JPPFTaskBundle) bundle.getJob());
    setMessage(message);
  }

  /**
   * Deserialize a task bundle from the message read into this buffer.
   * @return a {@link AbstractNodeContext} instance.
   * @throws Exception if an error occurs during the deserialization.
   */
  public BundleWrapper deserializeBundle() throws Exception
  {
    List<DataLocation> locations = ((AbstractTaskBundleMessage) message).getLocations();
    JPPFTaskBundle bundle = ((AbstractTaskBundleMessage) message).getBundle();
    BundleWrapper wrapper = new BundleWrapper(bundle);
    if (locations.size() > 1)
    {
      for (int i=1; i<locations.size(); i++) wrapper.addTask(locations.get(i));
    }
    return wrapper;
  }

  /**
   * Create a new message.
   * @return an {@link AbstractTaskBundleMessage} instance.
   */
  public abstract AbstractTaskBundleMessage newMessage();

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean readMessage(final ChannelWrapper<?> channel) throws Exception
  {
    if (message == null) message = newMessage();
    return message.read(channel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean writeMessage(final ChannelWrapper<?> channel) throws Exception
  {
    return message.write(channel);
  }

  /**
   * Determine whether the job was canceled.
   * @return true if the job was canceled, false otherwise.
   */
  public synchronized boolean isJobCanceled()
  {
    return jobCanceled;
  }

  /**
   * Specify whether the job was canceled.
   * @param jobCanceled true if the job was canceled, false otherwise.
   */
  public synchronized void setJobCanceled(final boolean jobCanceled)
  {
    this.jobCanceled = jobCanceled;
  }

  /**
   * Get the node system information.
   * @return a {@link JPPFSystemInformation} instance.
   */
  public JPPFSystemInformation getNodeInfo()
  {
    return nodeInfo;
  }

  /**
   * Set the node system information.
   * @param nodeInfo a {@link JPPFSystemInformation} instance.
   */
  public void setNodeInfo(final JPPFSystemInformation nodeInfo)
  {
    this.nodeInfo = nodeInfo;
  }
}
