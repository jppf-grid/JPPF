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
package org.jppf.server.node.local;

import java.util.List;
import java.util.concurrent.*;

import org.jppf.JPPFUnsupportedOperationException;
import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.io.DataLocation;
import org.jppf.node.protocol.TaskBundle;
import org.jppf.server.nio.nodeserver.LocalNodeMessage;
import org.jppf.server.node.*;
import org.slf4j.*;

/**
 * Instances of this class represent dynamic class loading, and serialization/deserialization, capabilities, associated
 * with a specific client application.<br>
 * The application is identified through a unique uuid. This class effectively acts as a container for the classes of
 * a client application, a provides the methods to enable the transport, serialization and deserialization of these classes.
 * @author Laurent Cohen
 */
public class JPPFLocalContainer extends JPPFContainer {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFLocalContainer.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this container with a specified application uuid.
   * @param node the node holding this container.
   * @param uuidPath the unique identifier of a submitting application.
   * @param classLoader the class loader for this container.
   * @param clientAccess whether the node has access to the client that submitted the job.
   * @throws Exception if an error occurs while initializing.
   */
  public JPPFLocalContainer(final AbstractCommonNode node, final List<String> uuidPath, final AbstractJPPFClassLoader classLoader, final boolean clientAccess) throws Exception {
    super(node, uuidPath, classLoader, clientAccess);
  }

  /**
   * Deserialize a number of objects from a socket client.
   * @param list a list holding the resulting deserialized objects.
   * @param count the number of objects to deserialize.
   * @param currentMessage the number of objects to deserialize.
   * @param executor the number of objects to deserialize.
   * @return the new position in the source data after deserialization.
   * @throws Exception if an error occurs while deserializing.
   */
  public int deserializeObjects(final Object[] list, final int count, final LocalNodeMessage currentMessage, final ExecutorService executor) throws Exception {
    if (debugEnabled) log.debug("deserializing {} objects", count);
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(classLoader);
      final CompletionService<ObjectDeserializationTask> completionService = new ExecutorCompletionService<>(executor, new ArrayBlockingQueue<Future<ObjectDeserializationTask>>(count));
      final List<DataLocation> locations = currentMessage.getLocations();
      for (int i = 0; i < count; i++) {
        completionService.submit(new ObjectDeserializationTask(this, (TaskBundle) list[0], locations.get(i + 1), i));
      }
      for (int i=0; i<count; i++) {
        final Future<ObjectDeserializationTask> f = completionService.take();
        final ObjectDeserializationTask task = f.get();
        list[task.getIndex() + 1] = task.getObject();
      }
      return 0;
    } finally {
      Thread.currentThread().setContextClassLoader(cl);
    }
  }

  @Override
  public int deserializeObjects(final Object[] list, final int count, final ExecutorService executor) throws Exception {
    throw new JPPFUnsupportedOperationException("method " + getClass().getName() + ".deserializeObjects(Object[], int, ExecutorService) should never be called for a local node");
  }
}
