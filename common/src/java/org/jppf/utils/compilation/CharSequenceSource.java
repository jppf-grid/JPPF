/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import java.net.URI;

import javax.tools.SimpleJavaFileObject;

/**
 * Instances of this class encapsulate a java compilation unit (source code "file")
 * represented as a character sequence.
 * @author Laurent Cohen
 */
class CharSequenceSource extends SimpleJavaFileObject
{
  /**
   * The source code of this "file".
   */
  private final CharSequence code;

  /**
   * Constructs a new StringJavaSource.
   * @param name the name of the compilation unit represented by this file object
   * @param code the source code for the compilation unit represented by this file object
   */
  public CharSequenceSource(final String name, final CharSequence code)
  {
    super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
    this.code = code;
  }

  @Override
  public CharSequence getCharContent(final boolean ignoreEncodingErrors)
  {
    return code;
  }
}
