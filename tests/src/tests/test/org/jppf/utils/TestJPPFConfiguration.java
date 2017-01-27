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
import org.junit.*;

import test.org.jppf.test.setup.*;

/**
 * Unit test for the <code>JPPFConfiguration</code> class.
 * @author Laurent Cohen
 */
public class TestJPPFConfiguration extends BaseTest {
  /**
   * Value of the {@code JPPFConfiguration.CONFIG_PROPERTY} system property.
   */
  private static String configProp;
  /**
   * Value of the {@code JPPFConfiguration.CONFIG_PLUGIN_PROPERTY} system property.
   */
  private static String configPluginProp;

  /**
   * Saves the values of {@code JPPFConfiguration.CONFIG_PROPERTY} and {@code JPPFConfiguration.CONFIG_PLUGIN_PROPERTY} system properties.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    configProp = System.getProperty(JPPFConfiguration.CONFIG_PROPERTY);
    configPluginProp = System.getProperty(JPPFConfiguration.CONFIG_PLUGIN_PROPERTY);
  }

  /**
   * Restores the values of {@code JPPFConfiguration.CONFIG_PROPERTY} and {@code JPPFConfiguration.CONFIG_PLUGIN_PROPERTY} system properties.
   * @throws Exception if a process could not be stopped.
   */
  @AfterClass
  public static void cleanup() throws Exception {
    System.setProperty(JPPFConfiguration.CONFIG_PROPERTY, configProp == null ? "" : configProp);
    System.setProperty(JPPFConfiguration.CONFIG_PLUGIN_PROPERTY, configPluginProp == null ? "" : configPluginProp);
    BaseSetup.resetClientConfig();
  }

  /**
   * Test reading the configuration from a {@link JPPFConfiguration.ConfigurationSource} plugin.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 5000)
  public void testAlternateConfigurationSource() throws Exception {
    JPPFConfiguration.ConfigurationSource source = new TestConfigurationSource();
    System.setProperty(JPPFConfiguration.CONFIG_PROPERTY, "");
    System.setProperty(JPPFConfiguration.CONFIG_PLUGIN_PROPERTY, source.getClass().getName());
    JPPFConfiguration.reset();
    TypedProperties config = JPPFConfiguration.getProperties();
    assertNotNull(config);
    String s = config.getString("jppf.config.source.origin", null);
    assertNotNull(s);
    assertEquals(s, "stream");
  }

  /**
   * Test reading the configuration from a {@link JPPFConfiguration.ConfigurationSourceReader} plugin.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 5000)
  public void testAlternateConfigurationSourceReader() throws Exception {
    JPPFConfiguration.ConfigurationSourceReader source = new TestConfigurationSourceReader();
    System.setProperty(JPPFConfiguration.CONFIG_PROPERTY, "");
    System.setProperty(JPPFConfiguration.CONFIG_PLUGIN_PROPERTY, source.getClass().getName());
    JPPFConfiguration.reset();
    TypedProperties config = JPPFConfiguration.getProperties();
    assertNotNull(config);
    String s = config.getString("jppf.config.source.reader.origin", null);
    assertNotNull(s);
    assertEquals(s, "reader");
  }

  /**
   * Test implementation of alternate configuration source.
   */
  public static class TestConfigurationSource implements JPPFConfiguration.ConfigurationSource {
    @Override
    public InputStream getPropertyStream() throws IOException {
      String props = "jppf.config.source.origin = stream";
      JPPFBuffer buffer = new JPPFBuffer(props);
      return new ByteArrayInputStream(buffer.getBuffer());
    }
  }

  /**
   * Test implementation of alternate configuration source.
   */
  public static class TestConfigurationSourceReader implements JPPFConfiguration.ConfigurationSourceReader {
    @Override
    public Reader getPropertyReader() throws IOException {
      String props = "jppf.config.source.reader.origin = reader";
      return new StringReader(props);
    }
  }
}
