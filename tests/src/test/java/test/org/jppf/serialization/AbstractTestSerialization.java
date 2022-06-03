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

package test.org.jppf.serialization;

import static org.junit.Assert.*;

import java.util.*;

import org.jppf.serialization.*;
import org.jppf.serialization.kryo.KryoSerialization;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.configuration.JPPFProperties;
import org.junit.*;

import test.org.jppf.test.setup.*;

/**
 * Unit tests for the JPPF serialization scheme.
 * @author Laurent Cohen
 */
public abstract class AbstractTestSerialization extends AbstractNonStandardSetup {
  /**
   * Test timeout.
   */
  private static final long TEST_TIMEOUT = 15_000L;
  /**
   * Whether the serialization scheme allows non-serializable classes.
   */
  static boolean allowsNonSerializable = true;
  /**
   * 
   */
  final static Map<String, String> serializationMap = new HashMap<String, String>() {{
    put("TestJava", DefaultJavaSerialization.class.getName());
    put("TestJavaWithLZ4", "LZ4 " + DefaultJavaSerialization.class.getName());
    put("TestJavaWithZLIB", "ZLIB " + DefaultJavaSerialization.class.getName());
    put("TestJPPF", DefaultJPPFSerialization.class.getName());
    put("TestJPPFWithLZ4", "LZ4 " + DefaultJPPFSerialization.class.getName());
    put("TestJPPFWithZLIB", "ZLIB " + DefaultJPPFSerialization.class.getName());
    put("TestKryo", KryoSerialization.class.getName());
    put("TestKryoWithLZ4", "LZ4 " + KryoSerialization.class.getName());
    put("TestKryoWithZLIB", "ZLIB " + KryoSerialization.class.getName());
    put("TestXStream", XstreamSerialization.class.getName());
    put("TestXStreamWithLZ4", "LZ4 " + XstreamSerialization.class.getName());
    put("TestXStreamWithZLIB", "ZLIB " + XstreamSerialization.class.getName());
  }};

  /**
   * @throws Exception if any error occurs could not be started.
   */
  static void resetSerialization() throws Exception {
    //if (BaseSetup.isTestWithEmbeddedGrid()) {
      final StackTraceElement elt = new Throwable().getStackTrace()[1];
      final String fqn = elt.getClassName();
      final int idx = fqn.lastIndexOf('.');
      assertTrue(idx > 0);
      final String simpleName = fqn.substring(idx + 1);
      final String value = serializationMap.get(simpleName);
      if (value == null) throw new IllegalStateException(simpleName + " is not a recognized serialization test class");
      print("serialization identifier: " + value);
      JPPFConfiguration.set(JPPFProperties.OBJECT_SERIALIZATION_CLASS, value);
      JPPFSerialization.Factory.reset();
    //}
  }

  /**
   * Test a simple job.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testSimpleJob() throws Exception {
    super.testSimpleJob(null);
  }

  @Override
  @Test(timeout = TEST_TIMEOUT)
  public void testMultipleJobs() throws Exception {
    super.testMultipleJobs();
  }

  @Override
  @Test(timeout = TEST_TIMEOUT)
  public void testCancelJob() throws Exception {
    super.testCancelJob();
  }

  @Override
  @Test(timeout = TEST_TIMEOUT)
  public void testNotSerializableWorkingInNode() throws Exception {
    if (allowsNonSerializable) super.testNotSerializableWorkingInNode();
  }

  @Override
  @Test(timeout = TEST_TIMEOUT)
  public void testForwardingMBean() throws Exception {
    super.testForwardingMBean();
  }
}
