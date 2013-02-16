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

package org.jppf.utils.compilation;

import java.io.IOException;
import java.util.*;

import javax.tools.*;

/**
 * A {@link JavaFileManager} which outputs the classes byte code to memory.
 * @param <M> the type of file manager to delegate to.
 * @author Laurent Cohen
 */
class InMemoryFileManager<M extends JavaFileManager> extends ForwardingJavaFileManager<M>
{
  /**
   * Map of class names to JavaFileObject instances for the bytecode.
   */
  private Map<String, BytecodeObject> classMap = new HashMap<String, BytecodeObject>();

  /**
   * Construct this file manager.
   * @param fileManager the file manager to delegate to.
   */
  public InMemoryFileManager(final M fileManager)
  {
    super(fileManager);
  }

  /**
   * This is where we use our in-memory {@link BytecodeObject} objects, whenever <code>kind</code> is equal to {@link JavaFileObject.Kind#CLASS}.
   * <p>{@inheritDoc}
   */
  @Override
  public JavaFileObject getJavaFileForOutput(final Location location, final String className,
    final JavaFileObject.Kind kind, final FileObject sibling) throws IOException
  {
    // special processing for class files only
    if (kind == JavaFileObject.Kind.CLASS)
    {
      BytecodeObject bytecode = classMap.get(className);
      if (bytecode == null)
      {
        bytecode = new BytecodeObject(className);
        classMap.put(className, bytecode);
      }
      return bytecode;
    }
    // otherwise delegate to the initial file manager
    return super.getJavaFileForOutput(location, className, kind, sibling);
  }

  /**
   * Get the bytecode of all classes compiled with this file manager.
   * @return a mapping of class names to the corresponding bytecode expressed as a byte[].
   */
  public Map<String, byte[]> getAllByteCodes()
  {
    Map<String, byte[]> result = new HashMap<String, byte[]>();
    for (Map.Entry<String, BytecodeObject> entry: classMap.entrySet())
    {
      result.put(entry.getKey(), entry.getValue().getBytecode());
    }
    return result;
  }

  /**
   * We cleanup the bytecode map, because {@link BytecodeObject} does not make a copy of its internal byte array,
   * which could prevent this file manager instance from being garbage-collected.
   * <p>This implies that a call to {@link #getAllByteCodes()} after this method has been invoked will
   * return an empty <code>Map</code>.
   * @throws IOException if any I/O error occurs.
   */
  @Override
  public void close() throws IOException
  {
    classMap.clear();
    super.close();
  }
}
