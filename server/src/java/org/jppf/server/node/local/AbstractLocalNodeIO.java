/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.server.node.local;

import static org.jppf.node.protocol.BundleParameter.*;

import org.jppf.node.protocol.TaskBundle;
import org.jppf.server.nio.nodeserver.LocalNodeMessage;
import org.jppf.server.node.AbstractNodeIO;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * This class performs the I/O operations requested by the JPPFNode, for reading the task bundles and sending the results back.
 * @author Laurent Cohen
 */
public abstract class AbstractLocalNodeIO extends AbstractNodeIO<JPPFLocalNode> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractLocalNodeIO.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The message to deserialize.
   */
  protected LocalNodeMessage currentMessage;

  /**
   * Initialize this TaskIO with the specified node.
   * @param node - the node who owns this TaskIO.
   */
  public AbstractLocalNodeIO(final JPPFLocalNode node) {
    super(node);
  }

  /**
   * Performs the actions required if reloading the classes is necessary.
   * @throws Exception if any error occurs.
   * @see org.jppf.server.node.AbstractNodeIO#handleReload()
   */
  @Override
  protected void handleReload() throws Exception {
    node.setClassLoader(null);
    node.initHelper();
  }

  @Override
  protected Object[] deserializeObjects(final TaskBundle bundle) throws Exception {
    final int count = bundle.getTaskCount();
    final Object[] list = new Object[count + 2];
    list[0] = bundle;
    try {
      initializeBundleData(bundle);
      if (debugEnabled) log.debug("bundle task count = " + count + ", handshake = " + bundle.isHandshake());
      if (!bundle.isHandshake()) {
        final boolean clientAccess = !bundle.getParameter(FROM_PERSISTENCE, false);
        final JPPFLocalContainer cont = (JPPFLocalContainer) node.getClassLoaderManager().getContainer(bundle.getUuidPath().getList(), clientAccess, (Object[]) null);
        cont.getClassLoader().setRequestUuid(bundle.getUuid());
        if (!node.isOffline() && !bundle.getSLA().isRemoteClassLoadingEnabled()) cont.getClassLoader().setRemoteClassLoadingDisabled(true);
        node.getLifeCycleEventHandler().fireJobHeaderLoaded(bundle, cont.getClassLoader());
        cont.setCurrentMessage(currentMessage);
        cont.deserializeObjects(list, 1+count, node.getSerializationExecutor());
      } else {
        // skip null data provider
      }
      if (debugEnabled) log.debug("got all data");
    } catch(final Throwable t) {
      log.error("Exception occurred while deserializing the tasks", t);
      bundle.setTaskCount(0);
      bundle.setParameter(NODE_EXCEPTION_PARAM, t);
    } finally {
      currentMessage = null;
    }
    return list;
  }
}
