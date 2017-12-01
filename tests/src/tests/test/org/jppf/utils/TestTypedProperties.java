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

package test.org.jppf.utils;

import static org.junit.Assert.*;

import java.io.*;

import org.jppf.utils.*;
import org.jppf.utils.configuration.*;
import org.junit.Test;

import test.org.jppf.test.setup.BaseTest;

/**
 * Unit test for the <code>TypedProperties</code> class.
 * @author Laurent Cohen
 */
public class TestTypedProperties extends BaseTest {
  /**
   * Test including a file within a properties file.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testIncludeFile() throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append("prop.1 = prop.1.value\n");
    sb.append("#!include file test/org/jppf/utils/FileInclude.properties\n");
    sb.append("prop.2 = prop.2.value\n");
    try (Reader r = new StringReader(sb.toString())) {
      TypedProperties props = new TypedProperties().loadAndResolve(r);
      checkProperty(props, "prop.1", "prop.1.value");
      checkProperty(props, "prop.2", "prop.2.value");
      checkProperty(props, "file.include.prop.1", "file.include.prop.1.value");
      checkProperty(props, "file.include.prop.2", "file.include.prop.2.value");
    }
  }

  /**
   * Test including a URL within a properties file.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testIncludeURL() throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append("prop.1 = prop.1.value\n");
    File file = new File("classes/tests/test/org/jppf/utils/URLInclude.properties");
    sb.append("#!include url ").append("file:").append(StringUtils.getDecodedURLPath(file.toURI().toURL())).append('\n');
    sb.append("prop.2 = prop.2.value\n");
    try (Reader r = new StringReader(sb.toString())) {
      TypedProperties props = new TypedProperties().loadAndResolve(r);
      checkProperty(props, "prop.1", "prop.1.value");
      checkProperty(props, "prop.2", "prop.2.value");
      checkProperty(props, "url.include.prop.1", "url.include.prop.1.value");
      checkProperty(props, "url.include.prop.2", "url.include.prop.2.value");
    }
  }

  /**
   * Test including a configuration source within a properties file.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testIncludeConfigSource() throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append("prop.1 = prop.1.value\n");
    sb.append("#!include class ").append(TestConfigurationSourceReader.class.getName()).append('\n');
    sb.append("prop.2 = prop.2.value\n");
    try (Reader r = new StringReader(sb.toString())) {
      TypedProperties props = new TypedProperties().loadAndResolve(r);
      checkProperty(props, "prop.1", "prop.1.value");
      checkProperty(props, "prop.2", "prop.2.value");
      checkProperty(props, "reader.include.prop.1", "reader.include.prop.1.value");
      checkProperty(props, "reader.include.prop.2", "reader.include.prop.2.value");
    }
  }

  /**
   * Test multiple includes of various types.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testMultipleIncludes() throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append("prop.1 = prop.1.value\n");
    sb.append("#!include class ").append(TestConfigurationSourceReader.class.getName()).append('\n');
    sb.append("prop.2 = prop.2.value\n");
    File file = new File("classes/tests/test/org/jppf/utils/URLInclude.properties");
    sb.append("#!include url ").append("file:").append(StringUtils.getDecodedURLPath(file.toURI().toURL())).append('\n');
    sb.append("#!include file test/org/jppf/utils/FileInclude.properties\n");
    try (Reader r = new StringReader(sb.toString())) {
      TypedProperties props = new TypedProperties().loadAndResolve(r);
      checkProperty(props, "prop.1", "prop.1.value");
      checkProperty(props, "prop.2", "prop.2.value");
      checkProperty(props, "reader.include.prop.1", "reader.include.prop.1.value");
      checkProperty(props, "reader.include.prop.2", "reader.include.prop.2.value");
      checkProperty(props, "url.include.prop.1", "url.include.prop.1.value");
      checkProperty(props, "url.include.prop.2", "url.include.prop.2.value");
      checkProperty(props, "file.include.prop.1", "file.include.prop.1.value");
      checkProperty(props, "file.include.prop.2", "file.include.prop.2.value");
    }
  }

  /**
   * Test multiple levels of nested includes of various types.
   * <p>NestedURLInclude.properties includes NestedFileInclude.properties which includes TestConfigurationSourceReader.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testNestedIncludes() throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append("prop.1 = prop.1.value\n");
    sb.append("prop.2 = prop.2.value\n");
    File file = new File("classes/tests/test/org/jppf/utils/NestedURLInclude.properties");
    sb.append("#!include url ").append("file:").append(StringUtils.getDecodedURLPath(file.toURI().toURL())).append('\n');
    try (Reader r = new StringReader(sb.toString())) {
      TypedProperties props = new TypedProperties().loadAndResolve(r);
      checkProperty(props, "prop.1", "prop.1.value");
      checkProperty(props, "prop.2", "prop.2.value");
      checkProperty(props, "reader.include.prop.1", "reader.include.prop.1.value");
      checkProperty(props, "reader.include.prop.2", "reader.include.prop.2.value");
      checkProperty(props, "url.include.prop.1", "url.include.prop.1.value");
      checkProperty(props, "url.include.prop.2", "url.include.prop.2.value");
      checkProperty(props, "file.include.prop.1", "file.include.prop.1.value");
      checkProperty(props, "file.include.prop.2", "file.include.prop.2.value");
    }
  }

  /**
   * Test that cycles in nested includes are handled.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15000L)
  public void testIncludeCycle() throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append("prop.1 = prop.1.value\n");
    sb.append("#!include file test/org/jppf/utils/CyclicFileInclude1.properties\n");
    sb.append("prop.2 = prop.2.value\n");
    try (Reader r = new StringReader(sb.toString())) {
      TypedProperties props = new TypedProperties().loadAndResolve(r);
      String s = props.getProperty("jppf.configuration.error");
      assertNotNull(s);
      assertTrue(s.indexOf(StackOverflowError.class.getName()) >= 0);
    }
  }

  /**
   * Test that property substitutions are handled properly.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testSubstitutions() throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append("prop.1 = 1/${prop.2}/${prop.3}/${prop.4}\n");
    sb.append("prop.2 = 2-${prop.3}-${prop.4}\n");
    sb.append("prop.3 = 3.${prop.4}\n");
    sb.append("prop.4 = 4\n");
    try (Reader r = new StringReader(sb.toString())) {
      TypedProperties props = new TypedProperties().loadAndResolve(r);
      printOut("%s: resolved properties: %s", ReflectionUtils.getCurrentMethodName(), props);
      checkProperty(props, "prop.4", "4");
      checkProperty(props, "prop.3", "3.4");
      checkProperty(props, "prop.2", "2-3.4-4");
      checkProperty(props, "prop.1", "1/2-3.4-4/3.4/4");
    }
  }

  /**
   * Test that property substitutions are handled properly.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testSubstitutions2() throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append("prop.1 = one\n");
    sb.append("prop.2 = two\n");
    sb.append("prop.3 = three\n");
    sb.append("prop.4 = ${prop.1}-${prop.2}-${prop.3}-four\n");
    try (Reader r = new StringReader(sb.toString())) {
      TypedProperties props = new TypedProperties().loadAndResolve(r);
      printOut("%s: resolved properties: %s", ReflectionUtils.getCurrentMethodName(), props);
      checkProperty(props, "prop.4", "one-two-three-four");
      checkProperty(props, "prop.3", "three");
      checkProperty(props, "prop.2", "two");
      checkProperty(props, "prop.1", "one");
    }
  }

  /**
   * Test that property substitutions are handled properly when the property name to substitute is empty (after trimming).
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testEmptyPropertyNameSubstitutions() throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append("prop.1 = 1/${prop.2}/${prop.3}\n");
    sb.append("prop.2 = 2-${}-${prop.3}\n");
    sb.append("prop.3 = 3\n");
    sb.append("prop.4 = ${  }+${prop.3}\n");
    try (Reader r = new StringReader(sb.toString())) {
      TypedProperties props = new TypedProperties().loadAndResolve(r);
      printOut("%s: resolved properties: %s", ReflectionUtils.getCurrentMethodName(), props);
      checkProperty(props, "prop.4", "${  }+3");
      checkProperty(props, "prop.3", "3");
      checkProperty(props, "prop.2", "2-${}-3");
      checkProperty(props, "prop.1", "1/2-${}-3/3");
    }
  }

  /**
   * Test that property substitutions are handled properly.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testUnresolvedSubstitutions() throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append("prop.1 = 1/${prop.2}\n");
    try (Reader r = new StringReader(sb.toString())) {
      TypedProperties props = new TypedProperties().loadAndResolve(r);
      printOut("%s: resolved properties: %s", ReflectionUtils.getCurrentMethodName(), props);
      checkProperty(props, "prop.1", "1/${prop.2}");
    }
  }

  /**
   * Test that property substitutions whith an unresolvable cycle are handled properly.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testSubstitutionsWithCycle() throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append("prop.1 = 1/${prop.2}\n");
    sb.append("prop.2 = 2-${prop.1}\n");
    try (Reader r = new StringReader(sb.toString())) {
      TypedProperties props = new TypedProperties().loadAndResolve(r);
      printOut("%s: resolved properties: %s", ReflectionUtils.getCurrentMethodName(), props);
      checkProperty(props, "prop.1", "1/${prop.2}");
      checkProperty(props, "prop.2", "2-${prop.1}");
    }
  }

  /**
   * Test that substitutions of environment variables references are handled properly.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testEnvironmentVariableSubstitution() throws Exception {
    String path = System.getenv("PATH");
    StringBuilder sb = new StringBuilder();
    sb.append("prop.1 = 1/${env.PATH}\n");
    sb.append("prop.2 = 2-${prop.1}\n");
    try (Reader r = new StringReader(sb.toString())) {
      TypedProperties props = new TypedProperties().loadAndResolve(r);
      printOut("%s: resolved properties: %s", ReflectionUtils.getCurrentMethodName(), props);
      checkProperty(props, "prop.1", "1/" + path);
      checkProperty(props, "prop.2", "2-1/" + path);
    }
  }

  /**
   * Test that substitutions of environment variables references are handled properly when the environment variable name is empty (after trimming).
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testEmptyEnvironmentVariableNameSubstitution() throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append("prop.1 = 1/${env.  }\n");
    sb.append("prop.2 = 2-${prop.1}-${env.}\n");
    try (Reader r = new StringReader(sb.toString())) {
      TypedProperties props = new TypedProperties().loadAndResolve(r);
      printOut("%s: resolved properties: %s", ReflectionUtils.getCurrentMethodName(), props);
      checkProperty(props, "prop.1", "1/${env.  }");
      checkProperty(props, "prop.2", "2-1/${env.  }-${env.}");
    }
  }

  /**
   * Test that substitutions of undefined environment variables references are handled properly.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testUnresolvedEnvironmentVariableSubstitution() throws Exception {
    StringBuilder sb = new StringBuilder();
    String undef = "${env.This_is_my_undefined_environment_variable}";
    sb.append("prop.1 = 1/" + undef + "\n");
    sb.append("prop.2 = 2-${prop.1}\n");
    try (Reader r = new StringReader(sb.toString())) {
      TypedProperties props = new TypedProperties().loadAndResolve(r);
      printOut("%s: resolved properties: %s", ReflectionUtils.getCurrentMethodName(), props);
      checkProperty(props, "prop.1", "1/" + undef);
      checkProperty(props, "prop.2", "2-1/" + undef);
    }
  }

  /**
   * Test that substitutions of system properties references are handled properly.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testSystemPropertySubstitution() throws Exception {
    String value = "sys.value";
    System.setProperty("test", value);
    StringBuilder sb = new StringBuilder();
    sb.append("prop.1 = 1/${sys.test}\n");
    sb.append("prop.2 = 2-${prop.1}\n");
    try (Reader r = new StringReader(sb.toString())) {
      TypedProperties props = new TypedProperties().loadAndResolve(r);
      printOut("%s: resolved properties: %s", ReflectionUtils.getCurrentMethodName(), props);
      checkProperty(props, "prop.1", "1/" + value);
      checkProperty(props, "prop.2", "2-1/" + value);
    }
  }

  /**
   * Test that substitutions of system properties references are handled properly when the environment variable name is empty (after trimming).
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testEmptySystemPropertyNameSubstitution() throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append("prop.1 = 1/${sys.  }\n");
    sb.append("prop.2 = 2-${prop.1}-${sys.}\n");
    try (Reader r = new StringReader(sb.toString())) {
      TypedProperties props = new TypedProperties().loadAndResolve(r);
      printOut("%s: resolved properties: %s", ReflectionUtils.getCurrentMethodName(), props);
      checkProperty(props, "prop.1", "1/${sys.  }");
      checkProperty(props, "prop.2", "2-1/${sys.  }-${sys.}");
    }
  }

  /**
   * Test that substitutions of undefined system properties references are handled properly.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testUnresolvedSystemPropertySubstitution() throws Exception {
    StringBuilder sb = new StringBuilder();
    String undef = "${sys.This_is_my_undefined_system_property}";
    sb.append("prop.1 = 1/" + undef + "\n");
    sb.append("prop.2 = 2-${prop.1}\n");
    try (Reader r = new StringReader(sb.toString())) {
      TypedProperties props = new TypedProperties().loadAndResolve(r);
      printOut("%s: resolved properties: %s", ReflectionUtils.getCurrentMethodName(), props);
      checkProperty(props, "prop.1", "1/" + undef);
      checkProperty(props, "prop.2", "2-1/" + undef);
    }
  }

  /**
   * Test scripted property values are evaluated properly.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testScriptedValues() throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append("jppf.script.default.language = $script::file{ test/org/jppf/utils/test.js }$\n");
    sb.append("prop.0 = hello miscreant world\n");
    sb.append("prop.1 = hello $script:javascript{ 2 + 3 }$ world\n");
    sb.append("prop.2 = hello $script:glouglou:{ 2 + 3 }$ world\n");
    sb.append("prop.3 = hello $script{ return 2 + 3 }$ world\n");
    sb.append("prop.4 = hello $script:groovy{ return 2 + 3 }$ dear $script:javascript{'' + (2 + 5)}$ world\n");
    sb.append("prop.5 = hello $script{ return '${prop.0} ' + (2 + 3) }$ world\n");
    sb.append("prop.6 = hello $script{ return thisProperties.getString('prop.0') + (2 + 3) }$ universe\n");

    sb.append("prop.11 = hello $s:javascript{ 2 + 3 }$ world\n");
    sb.append("prop.12 = hello $S:glouglou:{ 2 + 3 }$ world\n");
    sb.append("prop.13 = hello $s{ return 2 + 3 }$ world\n");
    sb.append("prop.14 = hello $S:groovy{ return 2 + 3 }$ dear $script:javascript{'' + (2 + 5)}$ world\n");
    sb.append("prop.15 = hello $s{ return '${prop.0} ' + (2 + 3) }$ world\n");
    sb.append("prop.16 = hello $s{ return thisProperties.getString('prop.0') + (2 + 3) }$ universe\n");

    sb.append("prop.21 = $s:js:f{ test/org/jppf/utils/test.js }$\n");
    File file = new File("classes/tests/test/org/jppf/utils/test.js");
    sb.append("prop.22 = $S:js:U{ ").append(file.toURI().toURL()).append(" }$\n");

    try (Reader r = new StringReader(sb.toString())) {
      TypedProperties props = new TypedProperties().loadAndResolve(r);
      printOut("%s: resolved properties: %s", ReflectionUtils.getCurrentMethodName(), props);
      checkProperty(props, "jppf.script.default.language", "groovy");
      checkProperty(props, "prop.0", "hello miscreant world");

      checkProperty(props, "prop.1", "hello 5.0 world");
      checkProperty(props, "prop.2", "hello $script:glouglou:{ 2 + 3 }$ world");
      checkProperty(props, "prop.3", "hello 5 world");
      checkProperty(props, "prop.4", "hello 5 dear 7 world");
      checkProperty(props, "prop.5", "hello hello miscreant world 5 world");
      checkProperty(props, "prop.6", "hello hello miscreant world5 universe");

      checkProperty(props, "prop.11", "hello 5.0 world");
      checkProperty(props, "prop.12", "hello $S:glouglou:{ 2 + 3 }$ world");
      checkProperty(props, "prop.13", "hello 5 world");
      checkProperty(props, "prop.14", "hello 5 dear 7 world");
      checkProperty(props, "prop.15", "hello hello miscreant world 5 world");
      checkProperty(props, "prop.16", "hello hello miscreant world5 universe");

      checkProperty(props, "prop.21", "groovy");
      checkProperty(props, "prop.22", "groovy");
    }
  }

  /**
   * Test properties with parmaterized names.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testParmetrizedProperties() throws Exception {
    JPPFProperty<String> hostProp = new StringProperty("<driver_name>.jppf.server.host", "localhost");
    String[] params = hostProp.getParameters();
    assertNotNull(params);
    assertEquals(1, params.length);
    assertEquals("driver_name", params[0]);
    JPPFProperty<Integer> portProp = new IntProperty("<driver_name>.jppf.server.port", 11111);
    params = portProp.getParameters();
    assertNotNull(params);
    assertEquals(1, params.length);
    assertEquals("driver_name", params[0]);
    TypedProperties props = new TypedProperties().set(JPPFProperties.DRIVERS, new String[] { "driver1", "driver2" });
    int i = 1;
    for (String driver: props.get(JPPFProperties.DRIVERS)) {
      props.set(hostProp, "host" + i, driver);
      props.set(portProp, 11110 + i, driver);
      i++;
    }
    print(true, true, "testing parmetrized properties: %s", props);
    i = 1;
    for (String driver: props.get(JPPFProperties.DRIVERS)) {
      assertEquals("host" + i, props.get(hostProp, driver));
      assertEquals(11110 + i, (int) props.get(portProp, driver));
      i++;
    }
    assertEquals("host1", props.getString("driver1.jppf.server.host"));
    assertEquals(11111, props.getInt("driver1.jppf.server.port"));
    assertEquals("host2", props.getString("driver2.jppf.server.host"));
    assertEquals(11112, props.getInt("driver2.jppf.server.port"));

    assertEquals("host1", props.remove(hostProp, "driver1"));
    assertEquals(Integer.valueOf(11111), props.remove(portProp, "driver1"));
    assertEquals("host2", props.remove(hostProp, "driver2"));
    assertEquals(Integer.valueOf(11112), props.remove(portProp, "driver2"));
  }

  /**
   * Test that the property with the specified key exists in the {@code TypedProperties} container and has the specified value.
   * @param props the {@link TypedProperties} containing the property to check.
   * @param key the name of the property to check.
   * @param value the value of the property to check.
   * @throws Exception if any error occurs.
   */
  private void checkProperty(final TypedProperties props, final String key, final String value) throws Exception {
    assertTrue("properties do not contain key=" + key, props.containsKey(key));
    assertEquals(value, props.getProperty(key));
  }

  /**
   * Test implementation of alternate configuration source.
   */
  public static class TestConfigurationSourceReader implements JPPFConfiguration.ConfigurationSourceReader {
    @Override
    public Reader getPropertyReader() throws IOException {
      StringBuilder sb = new StringBuilder();
      sb.append("reader.include.prop.1 = reader.include.prop.1.value\n");
      sb.append("reader.include.prop.2 = reader.include.prop.2.value\n");
      return new StringReader(sb.toString());
    }
  }
}
