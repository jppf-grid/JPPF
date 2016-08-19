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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.Callable;

/**
 * The source for the SSL configuration properties.
 * @author Laurent Cohen
 */
public class SSLConfigSource implements Callable<InputStream> {
  @Override
  public InputStream call() throws Exception {
    byte[] bytes = null;
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      AndroidHelper.getSSLConfig().store(baos, null);
      bytes = baos.toByteArray();
    }
    return new ByteArrayInputStream(bytes);
  }
}
