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

package org.jppf.serialization;

import java.io.*;


/**
 * This the factory for the JPPF implementation of object streams which allow
 * the serialization and deserialization of objects whose class does not implement <code>java.io.Serializable</code>.
 * @author Laurent Cohen
 */
public class GenericObjectStreamBuilder implements JPPFObjectStreamBuilder
{
  /**
   * {@inheritDoc}
   */
  @Override
  public ObjectInputStream newObjectInputStream(final InputStream in) throws Exception
  {
    return new JPPFObjectInputStream(in);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ObjectOutputStream newObjectOutputStream(final OutputStream out) throws Exception
  {
    return new JPPFObjectOutputStream(out);
  }
}
