/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.jca.serialization;

import java.io.*;

import org.jppf.serialization.*;
import org.jppf.utils.ObjectSerializerImpl;

/**
 * 
 * @author Laurent Cohen
 */
public class JcaObjectSerializerImpl extends ObjectSerializerImpl
{
  /**
   * The default constructor must be public to allow for instantiation through Java reflection.
   */
  public JcaObjectSerializerImpl()
  {
  }

  /**
   * Read an object from an input stream.
   * @param is - the input stream to deserialize from.
   * @return the object that was deserialized from the array of bytes.
   * @throws Exception if the ObjectInputStream used for deserialization raises an error.
   * @see org.jppf.serialization.ObjectSerializer#deserialize(java.io.InputStream)
   */
  @Override
  public Object deserialize(final InputStream is) throws Exception
  {
    try
    {
      return JPPFSerialization.Factory.getSerialization().deserialize(is);
    }
    finally
    {
      is.close();
    }
  }
}
