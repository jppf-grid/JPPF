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

package test.org.jppf.management.forwarding;

import static org.junit.Assert.*;

import java.util.*;

import org.jppf.management.forwarding.AbstractMBeanForwarder;
import org.jppf.utils.*;

import test.org.jppf.test.setup.*;

/**
 * Abstract super class for the autimatically generated units tests of the node mbeans forwarding proxies (also generated).
 * @author Laurent Cohen
 * @since 6.2
 */
public class AbstractTestForwarderProxy extends Setup1D2N1C {
  /**
   * Mapping of primitive types to corresponding wrapper types.
   */
  final static Map<Class<?>, Class<?>> WRAPPER_TYPES = new HashMap<Class<?>, Class<?>>() {{
    put(boolean.class, Boolean.class);
    put(char.class, Character.class);
    put(byte.class, Byte.class);
    put(short.class, Short.class);
    put(int.class, Integer.class);
    put(long.class, Long.class);
    put(float.class, Float.class);
    put(double.class, Double.class);
    put(void.class, Void.class);
  }};

  /**
   * Get a forwarding proxy for the specified node MBean interface.
   * @param inf the node MBean interface
   * @return an iinstance of a subclass of {@link AbstractMBeanForwarder}.
   * @throws Exception if any error occurs.
   */
  protected static AbstractMBeanForwarder getForwardingProxy(final Class<?> inf) throws Exception {
    return BaseSetup.getJMXConnection().getMBeanForwarder(inf);
  }

  /**
   * Check the results of a proxy method invcation.
   * @param results the invocation results.
   * @param expectedType the expected type of the result for each node.
   * @throws Exception if any error occurs.
   */
  protected void checkResults(final ResultsMap<String, ?> results, final Class<?> expectedType) throws Exception {
    assertNotNull(results);
    assertFalse(results.isEmpty());
    results.forEach((uuid, result) -> {
      assertNotNull(result);
      if (result.isException()) {
        print(false, false, "got exception for node %s: %s", uuid, ExceptionUtils.getStackTrace(result.exception()));
        fail("node " + uuid + " got " + ExceptionUtils.getMessage(result.exception()));
      }
      final Object res = result.result();
      if (res != null) assertTrue(wrapperType(expectedType).isAssignableFrom(res.getClass()));
    });
  }

  /**
   * Convert the specified type to its corresponding wrapper type, if any.
   * @param type the type to convert.
   * @return the corresponding wrapper type, or the initial type if there is no wrapper type.
   */
  private static Class<?> wrapperType(final Class<?> type) {
    final Class<?> c = WRAPPER_TYPES.get(type);
    return (c == null) ? type : c;
  }
}
