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

import java.security.cert.*;
import java.util.*;

import javax.net.ssl.X509TrustManager;

/**
 * Instances of this class allow an SSLSocketFactory or an SSLEngine to perform checks against multiple trust managers from multiple trusstores.
 * @author Laurent Cohen
 * @since 6.0
 */
public class CompositeX509TrustManager implements X509TrustManager {
  /**
   * The list of key managers to handle.
   */
  private final List<X509TrustManager> trustManagers;

  /**
   * Creates a new {@link CompositeX509TrustManager}.
   * @param trustManagers the X509 trust managers, ordered with the most-preferred managers first.
   */
  public CompositeX509TrustManager(final List<X509TrustManager> trustManagers) {
    this.trustManagers = Collections.unmodifiableList(new ArrayList<>(trustManagers));
  }

  @Override
  public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
    for (X509TrustManager mgr: trustManagers) {
      try {
        mgr.checkClientTrusted(chain, authType);
        return;
      } catch (@SuppressWarnings("unused") final Exception e) {
      }
    }
    throw new CertificateException(String.format("client not trusted for chain = %s, authType = %s, accepted issuers = %s", Arrays.asList(chain), authType, Arrays.asList(getAcceptedIssuers())));
  }

  @Override
  public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
    for (X509TrustManager mgr: trustManagers) {
      try {
        mgr.checkServerTrusted(chain, authType);
        return;
      } catch (@SuppressWarnings("unused") final Exception e) {
      }
    }
    throw new CertificateException(String.format("server not trusted for chain = %s, authType = %s, accepted issuers = %s", Arrays.asList(chain), authType, Arrays.asList(getAcceptedIssuers())));
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    final List<X509Certificate> issuers = new ArrayList<>();
    for (final X509TrustManager mgr: trustManagers) {
      for (final X509Certificate cert: mgr.getAcceptedIssuers()) issuers.add(cert);
    }
    return issuers.toArray(new X509Certificate[issuers.size()]);
  }
}
