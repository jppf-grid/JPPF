/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package test.org.jppf.server.protocol;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

import org.jppf.node.protocol.*;
import org.jppf.utils.FileUtils;
import org.junit.Test;

/**
 * Unit tests for the {@link Location} API.
 * @author Laurent Cohen
 */
public class TestLocation {
  /**
   * Test the copy from one location to another.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testCopyTo() throws Exception {
    int size = 11;
    byte[] buf = new byte[size];
    for (byte i=0; i<(byte) size; i++) buf[i] = i;
    checkCopy(new MemoryLocation(buf), size);
    File file = new File("tmpSrc.loc");
    try {
      FileUtils.writeBytesToFile(buf, file);
      checkCopy(new FileLocation(file), size);
      URL url = file.getAbsoluteFile().toURI().toURL();
      checkCopy(new URLLocation(url), size);
    } finally {
      if (file.exists()) file.delete();
    }
  }

  /**
   * Check the copy of the source location to all possible kinds of locations.
   * @param source the source location to copy.
   * @param size the size of the source.
   * @throws Exception if any error occurs.
   */
  private void checkCopy(final Location<?> source, final int size) throws Exception {
    assertEquals(size, source.size());
    checkCopiedLocation(source, new MemoryLocation((int) source.size()));
    File file = new File("tmpCopy.loc");
    try {
      checkCopiedLocation(source, new FileLocation(file));
    } finally {
      if (file.exists()) file.delete();
    }
    URL url = file.getAbsoluteFile().toURI().toURL();
    try {
      checkCopiedLocation(source, new URLLocation(url));
    } finally {
      if (file.exists()) file.delete();
    }
  }

  /**
   * Test the copy from one location to another.
   * @param source the location that is copied.
   * @param dest the location which is the destination of the copy.
   * @throws Exception if any error occurs.
   */
  private void checkCopiedLocation(final Location<?> source, final Location<?> dest) throws Exception {
    source.copyTo(dest);
    assertEquals(source.size(), dest.size());
    byte[] buf1 = source.toByteArray();
    assertNotNull(buf1);
    byte[] buf2 = dest.toByteArray();
    assertNotNull(buf2);
    assertTrue(Arrays.equals(buf1, buf2));
  }
}
