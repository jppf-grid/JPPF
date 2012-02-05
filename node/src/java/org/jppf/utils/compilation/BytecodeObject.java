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

package org.jppf.utils.compilation;

import java.io.*;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

/**
 * Instances of this class represent the in-memory output of compiling
 * a java class, including top-level, inner and anonymous classes.
 * This is in fact the equivalent of a .class file, but sotred in memory.
 * @author Laurent Cohen
 */
class BytecodeObject extends SimpleJavaFileObject {
  /**
   * The byte code resulting from a source compilation.
   */
  private byte[] bytecode;

  /**
   * Constructs a new BytecodeObject.
   * @param name the name of the compilation unit represented by this file object
   */
  public BytecodeObject(final String name) {
    super(URI.create("bytecode:///" + name.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
  }

  /**
   * Open an input stream that reads from the array of bytes that holds the class definition.
   * @return a {@link ByteArrayInputStream} backed by the array holding the class definition.
   * @throws IOException if any I/O error occurs.
   */
  @Override
  public InputStream openInputStream() throws IOException {
    return new ByteArrayInputStream(bytecode);
  }

  /**
   * Create an output stream in which to write the bytecode.
   * @return a {@link ByteArrayOutputStream}.
   * @throws IOException if any I/O error occurs.
   */
  @Override
  public OutputStream openOutputStream() throws IOException {
    return new ByteArrayOutputStream() {
      /**
       * Upon closing this output stream, update the bytecode.
       */
      @Override
      public void close() throws IOException {
        bytecode = this.toByteArray();
        super.close();
      }
    };
  }

  /**
   * Get the bytecode of this object.
   * @return an array of bytes.
   */
  public byte[] getBytecode() {
    return bytecode;
  }
}
