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

package org.jppf.utils;

import java.security.MessageDigest;

import org.slf4j.*;

/**
 * Utility and helper methods to deal with cryptographic operations. 
 * @author Laurent Cohen
 */
public class CryptoUtils {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(CryptoUtils.class);

  /**
   * Compute a hash string from a soruce string.
   * @param source the string from which to compute a hash.
   * @param algorithm the name of the hash algorithm to use, e.g. "SHA-256".
   * @return the computed hash, or null if any of the arguments is null or if the hash algorithm is unknown.
   */
  public static String computeHash(final String source, final String algorithm) {
    String hash = null;
    if ((source != null) && (algorithm != null)) {
      try {
        final MessageDigest digest = MessageDigest.getInstance(algorithm);
        final JPPFBuffer buf = new JPPFBuffer(source);
        digest.update(buf.buffer, 0, buf.length);
        final byte[] sig = digest.digest();
        hash = StringUtils.toHexString(sig);
      } catch (final Exception e) {
        log.error(String.format("error compputing %s hash for string %s : %s", algorithm, source, ExceptionUtils.getStackTrace(e)));
      }
    }
    return hash;
  }
}
