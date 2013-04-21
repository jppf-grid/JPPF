/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import java.io.IOException;

import javax.management.remote.generic.ObjectWrapping;

import org.jppf.comm.socket.BootstrapObjectSerializer;
import org.jppf.utils.ObjectSerializer;

/**
 * This implementation uses the configured JPPF serialization scheme.
 */
public class CustomWrapping implements ObjectWrapping
{
  /**
   * 
   */
  private static ObjectSerializer serializer = new BootstrapObjectSerializer();

  @Override
  public Object unwrap(final Object wrapped, final ClassLoader cl) throws IOException, ClassNotFoundException
  {
    try
    {
      return serializer.deserialize((byte[]) wrapped);
    }
    catch(IOException e)
    {
      throw e;
    }
    catch(ClassNotFoundException e)
    {
      throw e;
    }
    catch(Exception e)
    {
      throw new IOException(e);
    }
  }

  @Override
  public Object wrap(final Object obj) throws IOException
  {
    try
    {
      return serializer.serialize(obj).buffer;
    }
    catch(IOException e)
    {
      throw e;
    }
    catch(Exception e)
    {
      throw new IOException(e);
    }
  }
}