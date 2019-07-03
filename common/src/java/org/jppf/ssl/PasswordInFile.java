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

package org.jppf.ssl;

import static org.jppf.utils.StringUtils.maskPassword;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;

import org.jppf.utils.FileUtils;
import org.slf4j.*;

/**
 * A password source which uses a base64-encoded string stored in a file
 * @author Laurent Cohen
 */
public class PasswordInFile implements Callable<char[]> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(PasswordInFile.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Plain text format.
   */
  private static final int FORMAT_PLAIN = 1;
  /**
   * Plain text format.
   */
  private static final int FORMAT_BASE64 = 2;
  /**
   * Optional arguments that may be specified in the configuration.
   */
  private final String path;
  /**
   * 
   */
  private final int format;

  /**
   * Initialize this password source with a plain text password.
   * @param args the first argument represents the path to a text file.
   * @throws Exception if any error occurs.
   */
  public PasswordInFile(final String... args) throws Exception {
    if ((args == null) || (args.length == 0)) throw new SSLConfigurationException("missing password file path");
    this.path = args[0];
    int fmt = FORMAT_PLAIN;
    if ((args.length >= 2) && "base64".equalsIgnoreCase(args[1])) fmt = FORMAT_BASE64;
    this.format = fmt;
  }

  @Override
  public char[] call() throws Exception {
    String pwd = FileUtils.readTextFile(path).trim();
    if (debugEnabled) log.debug("got '{}' from file {}", maskPassword(pwd), path);
    if (format == FORMAT_BASE64) {
      final byte[] bytes = Base64.getDecoder().decode(pwd);
      if (debugEnabled) log.debug("decoded bytes length={}", bytes.length);
      pwd = new String(bytes, StandardCharsets.ISO_8859_1);
    }
    return pwd.toCharArray();
  }
}
