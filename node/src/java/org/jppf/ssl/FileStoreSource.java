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

package org.jppf.ssl;

import java.io.*;

import org.jppf.JPPFException;
import org.jppf.utils.FileUtils;

/**
 * A secure store source that uses a file as source.
 * @author Laurent Cohen
 */
public class FileStoreSource extends SecureStoreSource
{
  /**
   * Initialize this store source with a file name.
   * @param args the firt argument reprsents the path to the key or trust store
   */
  public FileStoreSource(final String... args)
  {
    super(args);
    if ((args == null) || (args.length == 0)) throw new IllegalArgumentException("missing parameter: keystore or trustore path");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream call() throws Exception
  {
    InputStream is = FileUtils.getFileInputStream(args[0]);
    if (is == null) throw new JPPFException("could not find secure store " + args[0]); 
    return is;
  }
}
