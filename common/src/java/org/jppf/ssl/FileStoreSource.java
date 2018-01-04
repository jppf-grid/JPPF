/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import java.io.InputStream;
import java.util.concurrent.Callable;

import org.jppf.utils.FileUtils;

/**
 * A secure store source that uses a file as source.
 * @author Laurent Cohen
 */
public class FileStoreSource implements Callable<InputStream> {
  /**
   * Optional arguments that may be specified in the configuration.
   */
  private final String[] args;

  /**
   * Initialize this store source with a file name.
   * @param args the firt argument represents the path to the key or trust store
   * @throws SSLConfigurationException if there is less than 1 argument.
   */
  public FileStoreSource(final String... args) throws SSLConfigurationException {
    this.args = args;
    if ((args == null) || (args.length == 0)) throw new SSLConfigurationException("missing parameter: keystore or trustore path");
  }

  @Override
  public InputStream call() throws Exception {
    final InputStream is = FileUtils.getFileInputStream(args[0]);
    //if (is == null) throw new SSLConfigurationException("could not find secure store " + args[0]); 
    return is;
  }
}
