/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
package org.jppf.android;

import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Utility methods and constants for preferenes handling.
 * @author Laurent Cohen
 */
public class PreferenceUtils {
  /**
   * Code for a file read request.
   */
  public static final int READ_REQUEST_CODE = 42;
  /**
   * The key name for the server connections preference.
   */
  public static final String SERVERS_KEY = "pref_servers";
  /**
   * The key name for the processing threads preference.
   */
  public static final String THREADS_KEY = "pref_threads";
  /**
   * The key name for the key store location preference.
   */
  public static final String KEY_STORE_LOCATION_KEY = "pref_key_store_location";
  /**
   * The key name for the key store password preference.
   */
  public static final String KEY_STORE_PASSWORD_KEY = "pref_key_store_password";
  /**
   * The key name for the trust store location preference.
   */
  public static final String TRUST_STORE_LOCATION_KEY = "pref_trust_store_location";
  /**
   * The key name for the trust store password preference.
   */
  public static final String TRUST_STORE_PASSWORD_KEY = "pref_trust_store_password";
  /**
   * The key name for the name of the SSLContext protocol. Single-select prefeence from a list.
   */
  public static final String SSL_CONTEXT_PROTOCOL_KEY = "pref_ssl_context_protocol";
  /**
   * The key name for the names of the SSLEngine protocols. Multi-select prefeence from a list.
   */
  public static final String SSL_ENGINE_PROTOCOL_KEY = "pref_ssl_engine_protocol";
  /**
   * The key name for the names enabled cypher suites. Multi-select prefeence from a list.
   */
  public static final String ENABLED_CIPHER_SUITES_KEY = "pref_enabled_cipher_suites";

  /**
   * Instantiation of this class is not permitted.
   */
  private PreferenceUtils() {
  }

  /**
   * Encode a url specified as a string.
   * @param url the url to encode.
   * @return the encoded url as a string, or the original url if it could not be encoded.
   */
  public static String encodeURL(final String url) {
    try {
      return URLEncoder.encode(url, "utf-8");
    } catch(Exception ignore) {
    }
    return url;
  }

  /**
   * Decode a url specified as a string.
   * @param url the url to decode.
   * @return the decoded url as a string, or the original url if it could not be decoded.
   */
  public static String decodeURL(final String url) {
    try {
      return URLDecoder.decode(url, "utf-8");
    } catch(Exception ignore) {
      ignore.printStackTrace();
    }
    return url;
  }
}
