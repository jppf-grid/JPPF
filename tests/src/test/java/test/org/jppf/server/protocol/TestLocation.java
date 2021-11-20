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

package test.org.jppf.server.protocol;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

import org.jppf.location.*;
import org.jppf.utils.FileUtils;
import org.junit.Test;

import test.org.jppf.test.setup.BaseTest;

/**
 * Unit tests for the {@link Location} API.
 * @author Laurent Cohen
 */
public class TestLocation extends BaseTest {
  /**
   * Test the copy from one location to another.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testCopyTo() throws Exception {
    final int size = 11;
    final byte[] buf = new byte[size];
    for (byte i=0; i<(byte) size; i++) buf[i] = i;
    checkCopy(new MemoryLocation(buf), size);
    final File file = new File("tmp Src.loc");
    try {
      FileUtils.writeBytesToFile(buf, file);
      checkCopy(new FileLocation(file), size);
      final URL url = file.getAbsoluteFile().toURI().toURL();
      checkCopy(new URLLocation(url), size);
    } finally {
      if (file.exists()) file.delete();
    }
  }

  /**
   * Test a maven central location.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15_000)
  public void testMavenCentralLocation() throws Exception {
    final File dest = new File("test1.jar");
    new MavenCentralLocation("org.jppf:jppf-node:6.1.1").copyTo(new FileLocation(dest));
    assertTrue(dest.exists());
    dest.delete();
  }

  /**
   * Check the copy of the source location to all possible kinds of locations.
   * @param source the source location to copy.
   * @param size the size of the source.
   * @throws Exception if any error occurs.
   */
  private static void checkCopy(final Location<?> source, final int size) throws Exception {
    assertEquals(size, source.size());
    checkCopiedLocation(source, new MemoryLocation((int) source.size()));
    final File file = new File("tmpCopy.loc");
    try {
      checkCopiedLocation(source, new FileLocation(file));
    } finally {
      if (file.exists()) file.delete();
    }
    final URL url = file.getAbsoluteFile().toURI().toURL();
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
  private static void checkCopiedLocation(final Location<?> source, final Location<?> dest) throws Exception {
    source.copyTo(dest);
    assertEquals(source.size(), dest.size());
    final byte[] buf1 = source.toByteArray();
    assertNotNull(buf1);
    final byte[] buf2 = dest.toByteArray();
    assertNotNull(buf2);
    assertTrue(Arrays.equals(buf1, buf2));
  }
}
