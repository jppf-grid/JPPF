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

package org.jppf.serialization.kryo;

import java.io.IOException;

import org.jppf.node.protocol.graph.TaskGraph;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 *
 * @author Laurent Cohen
 */
public class JobTaskGraphSerializer extends Serializer<TaskGraph> {
  @Override
  public TaskGraph read(final Kryo kryo, final Input input, final Class<TaskGraph> clazz) {
    final TaskGraph graph = new TaskGraph();
    try {
      graph.deserialize(input);
      return graph;
    } catch (final ClassNotFoundException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void write(final Kryo kryo, final Output output, final TaskGraph object) {
    try {
      object.serialize(output);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
