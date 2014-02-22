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

package test.org.jppf.utils;

import static org.junit.Assert.*;

import java.io.*;

import org.jppf.utils.*;
import org.junit.Test;

/**
 * Unit test for the <code>TypedProperties</code> class.
 * @author Laurent Cohen
 */
public class TestTypedProperties {
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
      TypedProperties props = TypedProperties.loadAndResolve(r);
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
    sb.append("#!include url ").append(file.toURI().toURL()).append('\n');
    sb.append("prop.2 = prop.2.value\n");
    try (Reader r = new StringReader(sb.toString())) {
      TypedProperties props = TypedProperties.loadAndResolve(r);
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
      TypedProperties props = TypedProperties.loadAndResolve(r);
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
  public void testMultipleIncludes() throws Exception
  {
    StringBuilder sb = new StringBuilder();
    sb.append("prop.1 = prop.1.value\n");
    sb.append("#!include class ").append(TestConfigurationSourceReader.class.getName()).append('\n');
    sb.append("prop.2 = prop.2.value\n");
    File file = new File("classes/tests/test/org/jppf/utils/URLInclude.properties");
    sb.append("#!include url ").append(file.toURI().toURL()).append('\n');
    sb.append("#!include file test/org/jppf/utils/FileInclude.properties\n");
    try (Reader r = new StringReader(sb.toString())) {
      TypedProperties props = TypedProperties.loadAndResolve(r);
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
    sb.append("#!include url ").append(file.toURI().toURL()).append('\n');
    try (Reader r = new StringReader(sb.toString())) {
      TypedProperties props = TypedProperties.loadAndResolve(r);
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
      TypedProperties props = TypedProperties.loadAndResolve(r);
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
      TypedProperties props = TypedProperties.loadAndResolve(r);
      System.out.println(ReflectionUtils.getCurrentMethodName() + ": resolved properties: " + props);
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
  public void testUnresolvedSubstitutions() throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append("prop.1 = 1/${prop.2}\n");
    try (Reader r = new StringReader(sb.toString())) {
      TypedProperties props = TypedProperties.loadAndResolve(r);
      System.out.println(ReflectionUtils.getCurrentMethodName() + ": resolved properties: " + props);
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
      TypedProperties props = TypedProperties.loadAndResolve(r);
      System.out.println(ReflectionUtils.getCurrentMethodName() + ": resolved properties: " + props);
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
      TypedProperties props = TypedProperties.loadAndResolve(r);
      System.out.println(ReflectionUtils.getCurrentMethodName() + ": resolved properties: " + props);
      checkProperty(props, "prop.1", "1/" + path);
      checkProperty(props, "prop.2", "2-1/" + path);
    }
  }

  /**
   * Test that substitutions of undefined environment variables references are handled properly.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testUnresolvedEnvironmentVariableSubstitution() throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append("prop.1 = 1/${env.This_is_my_undefined_environment_variable}\n");
    sb.append("prop.2 = 2-${prop.1}\n");
    try (Reader r = new StringReader(sb.toString())) {
      TypedProperties props = TypedProperties.loadAndResolve(r);
      System.out.println(ReflectionUtils.getCurrentMethodName() + ": resolved properties: " + props);
      checkProperty(props, "prop.1", "1/");
      checkProperty(props, "prop.2", "2-1/");
    }
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
