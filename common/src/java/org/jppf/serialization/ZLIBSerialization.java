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

package org.jppf.serialization;

import java.io.*;
import java.util.zip.*;

/**
 * A composite serialization scheme which applies a ZLIB compression/decompression to a concrete {@link JPPFSerialization} implementation.
 * @author Laurent Cohen
 */
public class ZLIBSerialization extends JPPFCompositeSerialization {
  @Override
  public void serialize(final Object o, final OutputStream os) throws Exception {
    Deflater deflater = new Deflater();
    DeflaterOutputStream zlibos = new DeflaterOutputStream(os, deflater);
    try {
      getDelegate().serialize(o, zlibos);
    } finally {
      zlibos.flush();
      zlibos.finish();
      deflater.end(); // required to clear the native/JNI buffers
    }
  }

  @Override
  public Object deserialize(final InputStream is) throws Exception {
    Inflater inflater = new Inflater();
    InflaterInputStream zlibis = new InflaterInputStream(is, inflater);
    try {
      return getDelegate().deserialize(zlibis);
    } finally {
      inflater.end(); // required to clear the native/JNI buffers
    }
  }

  @Override
  public String getName() {
    return "ZLIB";
  }
}
