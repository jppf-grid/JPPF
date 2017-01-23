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

package org.jppf.serialization;

import java.io.*;

import net.jpountz.lz4.*;

/**
 * A composite serialization scheme which applies a LZ4 compression/decompression to a concrete {@link JPPFSerialization} implementation.
 * <p><b>See also: <a href="http://cyan4973.github.io/lz4/">LZ4 home</a></b>.
 * @author Laurent Cohen
 */
public class LZ4Serialization extends JPPFCompositeSerialization {
  @Override
  public void serialize(final Object o, final OutputStream os) throws Exception {
    LZ4BlockOutputStream lz4os = new LZ4BlockOutputStream(os, 32*1024, LZ4Factory.fastestInstance().fastCompressor());
    try {
      getDelegate().serialize(o, lz4os);
    } finally {
      lz4os.finish();
    }
  }

  @Override
  public Object deserialize(final InputStream is) throws Exception {
    LZ4BlockInputStream lz4is = new LZ4BlockInputStream(is, LZ4Factory.fastestInstance().fastDecompressor());
    return getDelegate().deserialize(lz4is);
  }

  @Override
  public String getName() {
    return "LZ4";
  }
}
