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

package test.classversion;

import java.io.*;

/**
 * 
 * @author Laurent Cohen
 */
public class MyClass implements Serializable
{
  /**
   * 
   */
  private String name = "all your base are belong to us";

  /**
   * Get the name.
   * @return a string.
   */
  public String getName()
  {
    return name;
  }

  /**
   * Set the name.
   * @param name a string.
   */
  public void setName(final String name)
  {
    this.name = name;
  }

  /**
   * 
   * @param in the input stream.
   * @throws IOException if an I/O error occurs.
   * @throws ClassNotFoundException if a class cannot be found.
   */
  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException
  {
    name = (String) in.readObject();
    getClass().getClassLoader().loadClass(getClass().getName());
  }

  /**
   * 
   * @param out the output stream.
   * @throws IOException if an I/O error occurs.
   */
  private void writeObject(final ObjectOutputStream out) throws IOException
  {
    out.writeObject(name);
  }
}
