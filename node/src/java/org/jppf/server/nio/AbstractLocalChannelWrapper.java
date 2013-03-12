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

package org.jppf.server.nio;

import org.jppf.utils.SimpleObjectLock;
import org.slf4j.*;

/**
 * Channel wrapper and I/O implementation for the class loader of an in-VM node.
 * @param <S> The type of message handled by this channel wrapper.
 * @param <T> The type of context used by the channel on the server side of the communication.
 * @author Laurent Cohen
 */
public class AbstractLocalChannelWrapper<S, T extends AbstractNioContext> extends AbstractChannelWrapper<T>
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(AbstractLocalChannelWrapper.class);
  /**
   * Determines whether trace-level logging is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * This channel's key ops.
   */
  protected int keyOps = 0;
  /**
   * This channel's ready ops.
   */
  protected int readyOps = 0;
  /**
   * The resource passed to the node.
   */
  protected S nodeResource = null;
  /**
   * The resource passed to the server.
   */
  protected S serverResource = null;
  /**
   * Object used to synchronize threads when reading/writing the node message.
   */
  protected final SimpleObjectLock nodeLock = new SimpleObjectLock();
  /**
   * Object used to synchronize threads when reading/writing the server message.
   */
  protected final SimpleObjectLock serverLock = new SimpleObjectLock();
  /**
   * Object used to synchronize threads when reading/writing the keyOps or readyOps.
   */
  protected final SimpleObjectLock opsLock = new SimpleObjectLock();

  /**
   * Initialize this I/O handler with the specified context.
   * @param context the context used as communication channel.
   */
  public AbstractLocalChannelWrapper(final T context)
  {
    super(context);
    if (traceEnabled) log.trace("created " + this);
  }

  @Override
  public NioContext getContext()
  {
    return getChannel();
  }

  @Override
  public int getKeyOps()
  {
    synchronized(opsLock)
    {
      return keyOps;
    }
  }

  @Override
  public void setKeyOps(final int keyOps)
  {
    synchronized(opsLock)
    {
      this.keyOps = keyOps;
      if (traceEnabled) log.debug("id=" + id + ", readyOps=" + readyOps + ", keyOps=" + keyOps);
      if (selector != null) selector.wakeUp();
    }
  }

  @Override
  public int getReadyOps()
  {
    synchronized(opsLock)
    {
      return readyOps;
    }
  }

  /**
   * Set the operations for which this channel is ready.
   * @param readyOps the bitwise operations as an int value.
   */
  public void setReadyOps(final int readyOps)
  {
    synchronized(opsLock)
    {
      this.readyOps = readyOps;
      if (traceEnabled) log.debug("id=" + id + ", readyOps=" + readyOps + ", keyOps=" + keyOps);
      if (selector != null) selector.wakeUp();
    }
  }

  /**
   * Fetermine whether this channel can be selected by its selector.
   * @return <code>true</code> if the channel can be selected, <code>false</code> otherwise.
   */
  public boolean isSelectable()
  {
    synchronized(opsLock)
    {
      return (readyOps & keyOps) != 0;
    }
  }

  /**
   * Get the resource passed to the node.
   * @return an instance of the resource type used by this channel.
   */
  public S getNodeResource()
  {
    synchronized(nodeLock)
    {
      return nodeResource;
    }
  }

  /**
   * Set the resource passed to the node.
   * @param resource an instance of the resource type used by this channel.
   */
  public void setNodeResource(final S resource)
  {
    synchronized(nodeLock)
    {
      this.nodeResource = resource;
      nodeLock.wakeUp();
    }
  }

  /**
   * Get the resource passed to the server.
   * @return an instance of the resource type used by this channel.
   */
  public S getServerResource()
  {
    synchronized(serverLock)
    {
      return serverResource;
    }
  }

  /**
   * Set the resource passed to the server.
   * @param serverResource an instance of the resource type used by this channel.
   */
  public void setServerResource(final S serverResource)
  {
    synchronized(serverLock)
    {
      this.serverResource = serverResource;
      serverLock.wakeUp();
    }
  }


  /**
   * Get the object used to synchronize threads when reading/writing the node resource.
   * @return a {@link SimpleObjectLock} instance.
   */
  public SimpleObjectLock getNodeLock()
  {
    return nodeLock;
  }

  /**
   * Get the object used to synchronize threads when reading/writing the server resource.
   * @return a {@link SimpleObjectLock} instance.
   */
  public SimpleObjectLock getServerLock()
  {
    return serverLock;
  }

  /**
   * Get the object used to synchronize threads when reading/writing the keyOps or readyOps.
   * @return a {@link SimpleObjectLock} instance.
   */
  public SimpleObjectLock getOpsLock()
  {
    return opsLock;
  }

  /**
   * @return <code>true</code>.
   */
  @Override
  public boolean isLocal()
  {
    return true;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder(1000);
    sb.append(getClass().getSimpleName());
    sb.append('[');
    sb.append(getStringId());
    sb.append(", readyOps=").append(getReadyOps());
    sb.append(", keyOps=").append(getKeyOps());
    sb.append(", serverResource=").append(serverResource);
    sb.append(", nodeResource=").append(serverResource);
    sb.append(", context=").append(getContext());
    sb.append(']');
    return sb.toString();
  }
}
