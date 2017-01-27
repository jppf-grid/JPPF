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
package org.jppf.admin.web.admin;

import java.io.*;
import java.util.concurrent.Callable;

import org.jppf.admin.web.JPPFWebConsoleApplication;
import org.jppf.utils.*;

/**
 * The source for the SSL configuration properties.
 * @author Laurent Cohen
 */
public class SSLConfigSource implements Callable<InputStream> {
  @Override
  public InputStream call() throws Exception {
    InputStream is = null;
    TypedProperties sslConfig = JPPFWebConsoleApplication.get().getConfig(ConfigType.SSL).getProperties();
    if (sslConfig.isEmpty()) {
      is = FileUtils.getFileInputStream("ssl.properties");
    } else {
      is = new ByteArrayInputStream(sslConfig.asString().getBytes("UTF-8"));
    }
    return is;
  }
}
